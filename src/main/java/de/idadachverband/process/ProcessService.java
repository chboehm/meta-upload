package de.idadachverband.process;

import de.idadachverband.archive.AbstractVersion;
import de.idadachverband.archive.ArchiveException;
import de.idadachverband.archive.ArchiveService;
import de.idadachverband.archive.BaseVersion;
import de.idadachverband.archive.IdaInputArchiver;
import de.idadachverband.archive.InstitutionIndexState;
import de.idadachverband.archive.InstitutionArchive;
import de.idadachverband.archive.VersionInfo;
import de.idadachverband.archive.VersionKey;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.job.JobCallable;
import de.idadachverband.job.JobExecutionService;
import de.idadachverband.solr.SolrUpdateService;
import de.idadachverband.solr.SolrCore;
import de.idadachverband.transform.IdaTransformer;
import de.idadachverband.transform.TransformationBean;
import de.idadachverband.transform.xslt.WorkingFormatToSolrDocumentTransformer;
import de.idadachverband.utils.Directories;
import de.idadachverband.vufind.VufindInstanceManager;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.transform.TransformerException;

import org.apache.solr.client.solrj.SolrServerException;

import com.google.common.collect.Collections2;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by boehm on 09.10.14.
 */
@Named
@Slf4j
public class ProcessService
{
    final private JobExecutionService jobExecutionService;
    
    final private IdaInputArchiver idaInputArchiver;

    final private WorkingFormatToSolrDocumentTransformer workingFormatTransformer;

    final private ProcessFileConfiguration processFileConfiguration;
    
    final private ArchiveService archiveService;

    final private SolrUpdateService solrUpdateService;
    
    final private VufindInstanceManager vufindInstanceManager;
    

    @Inject
    public ProcessService(IdaTransformer transformationStrategy,
                               WorkingFormatToSolrDocumentTransformer workingFormatTransformer,
                               IdaInputArchiver idaInputArchiver,
                               ProcessFileConfiguration processFileConfiguration,
                               ArchiveService archiveService,
                               SolrUpdateService solrUpdateService,
                               JobExecutionService jobExecutionService,
                               VufindInstanceManager vufindInstanceManager)
    {
        this.idaInputArchiver = idaInputArchiver;
        this.workingFormatTransformer = workingFormatTransformer;
        this.processFileConfiguration = processFileConfiguration;
        this.archiveService = archiveService;
        this.solrUpdateService = solrUpdateService;
        this.jobExecutionService = jobExecutionService;
        this.vufindInstanceManager = vufindInstanceManager;
    }

    public ProcessJobBean processAsync(final Path input, final IdaInstitutionBean institution, final SolrCore solr, boolean incrementalUpdate, VersionInfo origin) throws IOException
    {
        log.info("Start processing of: {} for institution: {} on Solr core: {}", origin.getUploadedFileName(), institution, solr);
        final ProcessJobBean processJobBean = new ProcessJobBean(
                new TransformationBean(solr, institution, input, origin, incrementalUpdate));
        processJobBean.setJobName(String.format("Process file %s for %s, %s", 
                origin.getUploadedFileName(), solr.getName(), institution.getInstitutionName()));
        
        jobExecutionService.executeAsynchronous(processJobBean, new JobCallable<ProcessJobBean>()
        {
            @Override
            public void call(ProcessJobBean jobBean) throws Exception
            {
                process(jobBean.getTransformation());
            }
        });
        return processJobBean;
    }
    
    protected void process(TransformationBean transformation) throws TransformerException, IOException, SolrServerException, ArchiveException
    {
        try
        {
            transform(transformation);
            
            InstitutionArchive institutionArchive = archiveService.getArchive(transformation.getInstitution());
            InstitutionIndexState indexState = institutionArchive.getIndexState(transformation.getSolrCore());
            synchronized (indexState)
            {
                solrUpdateService.updateSolr(transformation, true);

                AbstractVersion archivedVersion = archiveService.archive(transformation);                
                archiveService.updateInstitutionIndexState(indexState, archivedVersion);
            }
            vufindInstanceManager.updateInstances(transformation.getSolrCore(), transformation.getInstitution());
        } 
        finally
        {
            Files.deleteIfExists(transformation.getTransformationInput());
            deleteProcessingFolder(transformation.getKey());
        }
    }
    
    protected void transform(Iterable<TransformationBean> transformations) throws IOException, TransformerException
    {
        for (TransformationBean transformation : transformations)
        {
            transform(transformation);
        }
    }
    
    protected void transform(TransformationBean transformationBean) throws IOException, TransformerException
    {
        final String key = transformationBean.getKey();
        Path path = idaInputArchiver.uncompressFile(
                transformationBean.getTransformationInput(), 
                processFileConfiguration.getStepFolder(ProcessStep.upload, key));

        path = transformToWorkingFormat(path, transformationBean);
        
        path = transformToSolrFormat(path, transformationBean);
        
        transformationBean.setSolrInput(path);
    }
    
    private Path transformToWorkingFormat(Path inputFile, TransformationBean transformation) throws TransformerException, IOException
    {
        final IdaInstitutionBean institution = transformation.getInstitution();
        
        log.info("Start transformation of: {} for: {} to working format", inputFile, institution);
        final long start = System.currentTimeMillis();
        transformation.setTransformationWorkingFormatMessages("Processing...");
        
        Path workingFormatFile = processFileConfiguration.getStepFolder(ProcessStep.workingFormat, transformation.getKey()).resolve(inputFile.getFileName());
        Files.createDirectories(workingFormatFile.getParent());
       
        IdaTransformer transformationStrategy = institution.getTransformationStrategy();
        try
        {
            transformationStrategy.transform(inputFile, workingFormatFile, institution);
        }
        catch (Exception e)
        {
            log.warn("Transformation of: {} for: {} to working format failed", inputFile, institution, e);
            transformation.setTransformationWorkingFormatMessages("Failure!" + transformationStrategy.getTransformationMessages());
            throw e;
        }
        
        final long end = System.currentTimeMillis();
        final long duration = (end - start) / 1000;
        log.info("Transformation of: {} for: {} to working format took: {} seconds", inputFile, institution, duration);
        transformation.setTransformationWorkingFormatMessages(
                String.format("Finished in %d seconds. %s", duration, transformationStrategy.getTransformationMessages()));
        
        return workingFormatFile;
    }

    private Path transformToSolrFormat(Path inputFile, TransformationBean transformation) throws TransformerException, IOException
    {
        final IdaInstitutionBean institution = transformation.getInstitution();
        
        log.info("Start transformation of: {} for: {} to Solr format", inputFile, institution);
        final long start = System.currentTimeMillis();
        transformation.setTransformationSolrFormatMessages("Processing...");
        
        Path solrFormatFile = processFileConfiguration.getStepFolder(ProcessStep.solrFormat, transformation.getKey()).resolve(inputFile.getFileName());
        Files.createDirectories(solrFormatFile.getParent());

        try 
        {
            workingFormatTransformer.transform(inputFile, solrFormatFile, institution);
        }
        catch (Exception e)
        {
            log.warn("Transformation of: {} for: {} to Solr format failed", inputFile, institution, e);
            transformation.setTransformationSolrFormatMessages("Failure!" + workingFormatTransformer.getTransformationMessages());
            throw e;
        }
        
        final long end = System.currentTimeMillis();
        final long duration = (end - start) / 1000;
        log.info("Transformation of: {} for: {} to Solr format took: {} seconds", inputFile, institution, duration);
        transformation.setTransformationSolrFormatMessages(
                String.format("Finished in %d seconds. %s", duration, workingFormatTransformer.getTransformationMessages()));
        
        return solrFormatFile;
    }
    
    private void deleteProcessingFolder(String key)
    {
        final Path path = processFileConfiguration.getBasePath().resolve(key);
        log.debug("Delete processing folder: {} for transformation: {}", path, key);
        Directories.delete(path);
    }

    /**
     * Asynchronously re-processes a given upload version (up to a given incremental update) of an institution in a Solr core
     * @param solrCore
     * @param institution
     * @param versionId
     * @param upToUpdateId
     * @return the bean of the enqueued re-process job
     * @throws ArchiveException
     */
    public ReprocessJobBean reprocessVersionAsync(final SolrCore solrCore, final IdaInstitutionBean institution, 
            final VersionKey version) throws ArchiveException
    {
        log.info("Start re-processing of version: {} for institution: {} on Solr core: {}", 
                version, institution, solrCore.getName());
        
        ReprocessJobBean reprocessJobBean = new ReprocessJobBean(solrCore, institution, version);
       
        jobExecutionService.executeAsynchronous(reprocessJobBean, new JobCallable<ReprocessJobBean>()
        {
            @Override
            public void call(ReprocessJobBean jobBean) throws Exception
            {
                List<TransformationBean> transformations =
                        prepareTransformations(solrCore, institution, version, jobBean.getUser().getUsername());
                jobBean.getTransformations().addAll(transformations);
                reprocess(solrCore, institution, version, transformations);
            }
        });
        
        return reprocessJobBean;
    }
   
    protected List<TransformationBean> prepareTransformations(SolrCore solrCore, IdaInstitutionBean institution, 
            VersionKey targetVersion, String userName) throws ArchiveException 
    {
        BaseVersion baseVersion = archiveService.getArchive(institution).getBaseVersion(targetVersion);

        List<TransformationBean> transformations = new ArrayList<>();
        transformations.add(TransformationBean.fromBaseVersion(baseVersion, solrCore, userName));
        transformations.addAll(Collections2.transform(
                baseVersion.getUpdatesIn(1, targetVersion.getUpdateNumber()), 
                updateVersion -> TransformationBean.fromUpdateVersion(updateVersion, solrCore, userName))
        );
           
        return transformations;
    }
    
    protected void reprocess(SolrCore solrCore, IdaInstitutionBean institution, VersionKey targetVersion, 
            List<TransformationBean> transformations) throws IOException, TransformerException, SolrServerException, ArchiveException
    {
        try
        {
            transform(transformations);
            
            InstitutionIndexState indexState = archiveService.getInstitutionIndexState(institution, solrCore);
            synchronized (indexState)
            {
                solrUpdateService.updateSolr(transformations, true);

                AbstractVersion lastArchivedVersion = archiveService.archive(transformations);
                archiveService.updateInstitutionIndexState(indexState, lastArchivedVersion);
            }
            vufindInstanceManager.updateInstances(solrCore, institution);

        } finally
        {
            for (TransformationBean transformation : transformations)
            {
                deleteProcessingFolder(transformation.getKey());
            }
        }
    }
}
