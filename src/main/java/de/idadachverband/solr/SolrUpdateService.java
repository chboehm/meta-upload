package de.idadachverband.solr;

import de.idadachverband.archive.ArchiveException;
import de.idadachverband.archive.ArchiveService;
import de.idadachverband.archive.BaseVersion;
import de.idadachverband.archive.IdaInputArchiver;
import de.idadachverband.archive.InstitutionIndexState;
import de.idadachverband.archive.InstitutionArchive;
import de.idadachverband.archive.UpdateVersion;
import de.idadachverband.archive.VersionInfo;
import de.idadachverband.archive.VersionKey;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.job.JobCallable;
import de.idadachverband.job.JobExecutionService;
import de.idadachverband.vufind.VufindInstanceManager;
import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrServerException;

import com.google.common.collect.Collections2;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by boehm on 25.02.15.
 */
@Named
@Slf4j
public class SolrUpdateService
{
    private final ArchiveService archiveService;
    private final IdaInputArchiver idaInputArchiver;
    private final JobExecutionService jobExecutionService;
    private final VufindInstanceManager vufindInstanceManager;
    
    @Inject
    public SolrUpdateService(
            ArchiveService archiveService, 
            IdaInputArchiver idaInputArchiver, 
            JobExecutionService jobExecutionService,
            VufindInstanceManager vufindInstanceManager)
    {
        this.archiveService = archiveService;
        this.idaInputArchiver = idaInputArchiver;
        this.jobExecutionService = jobExecutionService;
        this.vufindInstanceManager = vufindInstanceManager;
    }
    
    /**
     * Asynchronously re-indexes the latest upload of an institutions in a Solr core
     * @param solr
     * @param institution
     * @return the bean of the enqueued re-process job
     * @throws FileNotFoundException
     */
  /*  public ReindexJobBean reindexInstitutionAsync(SolrService solr, IdaInstitutionBean institution) throws ArchiveException
    {
        return reindexVersionAsync(solr, institution, 
                archiveService.getLatestVersionKey(institution.getInstitutionId()));
    }
  */  
    /**
     * Asynchronously re-indexes the latest upload of an institutions in a Solr core
     * @param solr
     * @param institution
     * @return the bean of the enqueued re-process job
     * @throws FileNotFoundException
     */
    public ReindexJobBean reindexVersionAsync(final SolrCore solr, final IdaInstitutionBean institution, 
            final VersionKey version) throws ArchiveException
    {
        log.info("Start re-indexing of version: {} for institution: {} on Solr core: {}", 
                version, institution, solr);
        
        final ReindexJobBean reindexJobBean = new ReindexJobBean(solr, institution, version);
        
        jobExecutionService.executeAsynchronous(reindexJobBean, new JobCallable<ReindexJobBean>()
        {
            @Override
            public void call(ReindexJobBean jobBean) throws Exception
            {
                List<SolrUpdateBean> solrUpdates =
                        reindex(solr, institution, version, jobBean.getUser().getUsername(), false);
                jobBean.getSolrUpdates().addAll(solrUpdates);
            }
        });
        
        return reindexJobBean;
    }
    
    /**
     * Asynchronously re-indexes latest uploads of all institution in a Solr core 
     * @param solrCore
     * @return batch job waiting for all re-index jobs (one job per institution)
     * @throws FileNotFoundException
     */
/*    public BatchJobBean reindexCoreAsync(SolrService solr) throws ArchiveException
    {
        final String coreName = solr.getName();
        final BatchJobBean batchJob = new BatchJobBean();
        batchJob.setJobName(String.format("Re-index archived uploads for all institutions on %s", coreName));
        
        for (InstitutionArchive archivedInstitution : archiveService.getInstitutionArchives(false))
        {
            batchJob.addChildJob(reindexInstitutionAsync(solr, archivedInstitution.getInstitutionBean()));
        }
        jobExecutionService.executeBatchAsynchronous(batchJob);
        return batchJob;
    }*/

    protected List<SolrUpdateBean> reindex(SolrCore solrCore, IdaInstitutionBean institution, VersionKey targetVersion, 
                                           String username, boolean isRollback) throws IOException, SolrServerException, ArchiveException 
    {
        // prepare Solr updates
        final List<SolrUpdateBean> solrUpdates = new ArrayList<>();
        
        InstitutionArchive institutionArchive = archiveService.getArchive(institution);
        InstitutionIndexState indexState = institutionArchive.getIndexState(solrCore);
        synchronized (indexState)
        {
            VersionKey indexedVersion = indexState.getVersionKey();
            BaseVersion baseVersion = institutionArchive.getBaseVersion(targetVersion);
            if (!isRollback && 
                !indexedVersion.isMissing() && 
                targetVersion.isIncrementalUpdateOf(indexedVersion))
            {
                solrUpdates.addAll(Collections2.transform(
                        baseVersion.getUpdatesIn(indexedVersion.getUpdateNumber() + 1, targetVersion.getUpdateNumber()),
                        updateVersion -> createSolrUpdateFromUpdateVersion(updateVersion, solrCore, username)));
            }
            else
            {
                // full re-indexing
                solrUpdates.add(createSolrUpdateFromBaseVersion(baseVersion, solrCore, username));
                solrUpdates.addAll(Collections2.transform(
                        baseVersion.getUpdatesIn(1, targetVersion.getUpdateNumber()),
                        updateVersion -> createSolrUpdateFromUpdateVersion(updateVersion, solrCore, username)));
            }
       
            // execute updates
            if (!solrUpdates.isEmpty())
            {
                updateSolr(solrUpdates, !isRollback);
                
                VersionInfo lastUpdateInfo = solrUpdates.get(solrUpdates.size() - 1).getOrigin();
                archiveService.updateInstitutionIndexState(indexState, lastUpdateInfo.getOriginalVersion(), lastUpdateInfo);
            }
        }
        if (!isRollback)
        {
            vufindInstanceManager.updateInstances(solrCore, institution);
        }
        
        return solrUpdates;
    }
    
    private SolrUpdateBean createSolrUpdateFromUpdateVersion(UpdateVersion updateVersionArchive, SolrCore solrService, String userName)
    {
        return new SolrUpdateBean(solrService, updateVersionArchive.getInstitution(), 
                updateVersionArchive.getSolrFormatFile(), 
                VersionInfo.ofReindex(userName, updateVersionArchive.getVersionKey()), 
                true);
    }

    private SolrUpdateBean createSolrUpdateFromBaseVersion(BaseVersion baseVersionArchive, SolrCore solrService, String userName)
    {
        return new SolrUpdateBean(solrService, baseVersionArchive.getInstitution(), 
                baseVersionArchive.getSolrFormatFile(),
                VersionInfo.ofReindex(userName, baseVersionArchive.getVersionKey()),
                false);
    }
    
    public void updateSolr(Iterable<? extends SolrUpdateBean> solrUpdates, boolean rollbackOnError) throws IOException, SolrServerException, ArchiveException
    {
        for (SolrUpdateBean solrUpdate : solrUpdates)
        {
            updateSolr(solrUpdate, rollbackOnError);
        }
    }

    public void updateSolr(SolrUpdateBean solrUpdate, boolean rollbackOnError) throws IOException, SolrServerException, ArchiveException
    {
        final SolrCore solrCore = solrUpdate.getSolrCore();
        final IdaInstitutionBean institution = solrUpdate.getInstitution();
       
        Path inputFile = solrUpdate.getSolrInput();
        
        log.info("Start Solr update of core: {} for: {} with file: {}", solrCore, institution, inputFile);
        final long start = System.currentTimeMillis();
        solrUpdate.setSolrMessage("Updating...");
        
        if (!solrUpdate.isIncrementalUpdate())
        {
            log.info("Delete institution: {} on Solr core: {}", institution, solrCore.getName());
            solrCore.deleteInstitution(institution.getInstitutionId());
        }

        boolean inputIsArchived = idaInputArchiver.inputIsZip(inputFile);
        if (inputIsArchived)
        {
            inputFile = idaInputArchiver.uncompressToTemporaryFile(inputFile);
        }
        
        try
        {
            String solrResult = solrCore.update(inputFile);
            log.debug("Solr result {}", solrResult);
        } 
        catch (Exception e)
        {
            if (rollbackOnError) {
                log.warn("Update of solr {} failed for institution {}. Start rollback.", solrCore, institution, e);
                final String rollbackResult = rollbackInstitution(institution, solrCore);
                log.info("Result of rollback is: {}", rollbackResult);
                solrUpdate.setSolrMessage(
                        String.format("Failure!\n%s\nSolr rollback: %s", e.getMessage(), rollbackResult));
            }
            else 
            {
                solrUpdate.setSolrMessage(
                        String.format("Failure!\n%s", e.getMessage()));
            }
            throw new SolrServerException("Invalid update", e);
        } 
        finally
        {
            if (inputIsArchived) {
                Files.deleteIfExists(inputFile);
            }
        }
        
        final long end = System.currentTimeMillis();
        final long duration = (end - start) / 1000;
        log.info("Solr update of core: {} for: {} with file: {} took: {} seconds.", solrCore, institution, inputFile, duration);
        solrUpdate.setSolrMessage(String.format("Finished in %d seconds.", duration));
    }
    
    /**
     * Re-indexes the last indexed version 
     * @param institution
     * @param solrCore
     * @return
     * @throws IOException
     * @throws SolrServerException
     */
    protected String rollbackInstitution(IdaInstitutionBean institution, SolrCore solrCore) throws ArchiveException, SolrServerException, IOException
    {
        VersionKey indexedVersion = archiveService.getIndexedVersionKey(institution, solrCore);
        if (indexedVersion.isMissing())
        {
            log.warn("Unknown indexed version for institution: {} on Solr core: {}", institution, solrCore);
            return "No archived version!";
        }
        
        List<SolrUpdateBean> solrUpdates = reindex(solrCore, institution, indexedVersion, "ROLLBACK", true);
        
        StringBuilder sb = new StringBuilder();
        for (SolrUpdateBean solrUpdate : solrUpdates)
        {
            solrUpdate.buildResultMessage(sb);
        }        
        return sb.toString();
    }
}
