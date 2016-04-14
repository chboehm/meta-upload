package de.idadachverband.archive;

import static de.idadachverband.utils.Directories.*;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.process.ProcessFileConfiguration;
import de.idadachverband.process.ProcessStep;
import de.idadachverband.solr.SolrCore;
import de.idadachverband.transform.TransformationBean;
import de.idadachverband.user.UserService;
import de.idadachverband.utils.Directories;
import de.idadachverband.utils.JsonHelper;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by boehm on 25.02.15.
 */
@Slf4j
@Named
public class ArchiveService
{
    
    public static final String VERSION_FILE = "version.json";
    public static final String INSTITUTION_FILE = "institution.json";
    
    private final ArchiveConfiguration archiveConfiguration;
    
    private final ProcessFileConfiguration processFileConfiguration;
    
    private final SimpleDateFormat dateFormat;
    
    private final IdaInputArchiver idaInputArchiver;
    
    private final Map<IdaInstitutionBean, InstitutionArchive> institutionArchives;
    
    @Inject
    public ArchiveService(ArchiveConfiguration archiveConfiguration,
                          ProcessFileConfiguration processFileConfiguration,
                          SimpleDateFormat dateFormat,
                          IdaInputArchiver idaInputArchiver,
                          UserService userService)
    {
        this.archiveConfiguration = archiveConfiguration;
        this.processFileConfiguration = processFileConfiguration;
        this.dateFormat = dateFormat;
        this.idaInputArchiver = idaInputArchiver;
        this.institutionArchives = new ConcurrentHashMap<>();
    }
    
    /**
     * finds the path of an archived file
     * @param step
     * @param coreName
     * @param institutionId
     * @param baseId
     * @param updateId id of incremental update, or LATEST_UPDATE / NO_UPDATE
     * @return
     * @throws ArchiveException
     */
    public Path findFile(ProcessStep step, String institutionId, VersionKey version) 
            throws ArchiveException
    {
        Path folder = archiveConfiguration.getStepFolder(step, institutionId, version);
        try 
        {
            Path file = Directories.findFirstFile(folder);
            return file;
        } catch (IOException e)
        {
            log.error("Could not find an archived file in: {}", folder, e);
            throw new ArchiveException(String.format("Could not find an archived file of step: %s for institution: %s version: %s", 
                    step, institutionId, version), e);
        }
    }
    
    /* -------- archive transformations --------*/
    
    
    public AbstractVersion archive(Iterable<TransformationBean> transformations) throws IOException, ArchiveException
    {
        AbstractVersion lastArchivedVersion = null;
        for (TransformationBean transformation : transformations)
        {
            lastArchivedVersion = archive(transformation);
        }
        return lastArchivedVersion;
    }
    
    /**
     * archives the step files of a given transformation
     * @param transformation
     * @throws IOException
     * @throws ArchiveException
     */
    public AbstractVersion archive(TransformationBean transformation) throws IOException, ArchiveException
    {
        final String transformationKey = transformation.getKey();
        final String institutionId = transformation.getInstitutionId();
        
        InstitutionArchive institutionArchive = getArchive(transformation.getInstitution());
        AbstractVersion version = institutionArchive.createNextVersion(transformation.getOrigin(), 
                transformation.isIncrementalUpdate());                
        
        VersionKey versionKey = version.getVersionKey();
        log.info("Archive transformation: {} for institution: {} using version: {}", transformationKey, institutionId, versionKey);
        transformation.setArchivedVersion(versionKey);

        version.setUploadFile(
                archiveStep(ProcessStep.upload, transformationKey, institutionId, versionKey));
        version.setWorkingFormatFile(
                archiveStep(ProcessStep.workingFormat, transformationKey, institutionId,versionKey));
        version.setSolrFormatFile(
                archiveStep(ProcessStep.solrFormat, transformationKey, institutionId, versionKey));
        
        storeVersionProperties(version);

        clearOldVersions(institutionArchive);   
        
        return version;
    }

    protected Path archiveStep(ProcessStep step, String key, String institutionId, VersionKey version) throws IOException
    {
        final Path sourcePath = Directories.findFirstFile(processFileConfiguration.getStepFolder(step, key));
        final Path targetFolder = archiveConfiguration.getStepFolder(step, institutionId, version);      
        
        return idaInputArchiver.archiveFile(sourcePath, targetFolder, step.getName() + "-");
    }

    protected void clearOldVersions(InstitutionArchive institutionArchive) 
    {
        synchronized (institutionArchive)
        {
            while (institutionArchive.getBaseVersions().size() > archiveConfiguration.getMaxArchivedVersions())
            {
                VersionKey oldVersion = new VersionKey(institutionArchive.getOldestBaseNumber(), 0);
                log.info("Clear old version: {} of institution: {}", oldVersion, institutionArchive);
                institutionArchive.removeBaseVersion(oldVersion.getBaseNumber());
                final Path versionFolder = archiveConfiguration.getVersionFolder(institutionArchive.getInstitutionId(), oldVersion);
                delete(versionFolder);
            }
        }
    }
    
    /* -------- delete archived files --------*/
    
    public void deleteVersion(IdaInstitutionBean institution, VersionKey versionKey) 
    {
        getArchive(institution).removeVersion(versionKey);
        final Path versionFolder = archiveConfiguration.getVersionFolder(institution.getInstitutionId(), versionKey);
        delete(versionFolder);
    }
        
    /* -------- archive traversal --------*/
    
    /**
     * @return list of archived institutions
     */
    public List<InstitutionArchive> getArchives(Iterable<IdaInstitutionBean> institutions)
    {
        ArrayList<InstitutionArchive> institutionArchives = new ArrayList<>();
        for (IdaInstitutionBean institution : institutions)
        {
            try
            {
                institutionArchives.add(getArchive(institution));
            } catch (Exception e)
            {
                log.warn("Could not load archived institution {}", institution, e);
            }
        }
        return institutionArchives;
    }
    
    public synchronized InstitutionArchive getArchive(IdaInstitutionBean institution)
    {
        if (!institutionArchives.containsKey(institution))
        {
            institutionArchives.put(institution, loadInstitutionArchive(institution));
        }
        return institutionArchives.get(institution);
    }
    
    public VersionKey getIndexedVersionKey(IdaInstitutionBean institution, SolrCore solr)
    {
        return getArchive(institution).getIndexState(solr).getVersionKey();
    }
    
    public InstitutionIndexState getInstitutionIndexState(IdaInstitutionBean institution, SolrCore solrCore)
    {
        return getArchive(institution).getIndexState(solrCore);
    }
    
    public synchronized void updateInstitutionIndexState(InstitutionIndexState indexState, VersionKey indexedVersionKey, VersionInfo origin)
    {
        indexState.update(indexedVersionKey, origin);
        storeInstitutionProperties(indexState.getInstitutionArchive());
    }
        
    public void updateInstitutionIndexState(InstitutionIndexState indexState, AbstractVersion indexedVersion)
    {
        updateInstitutionIndexState(indexState, indexedVersion.getVersionKey(), indexedVersion.getOrigin());
    }
    
    protected InstitutionArchive loadInstitutionArchive(IdaInstitutionBean institution) 
    {
        InstitutionArchive institutionArchive = new InstitutionArchive(institution);
        loadInstitutionProperties(institutionArchive);
       
        for (String baseVersionId : getArchivedBaseVersionIds(institution.getInstitutionId()))
        {
            log.debug("found version: {}", baseVersionId);
            try 
            {
                loadBaseVersion(institutionArchive, baseVersionId);
            } catch (Exception e)
            {
                log.warn("Could not load archived version {} of institution {}", baseVersionId, institution, e);
            }
        }
       
        return institutionArchive;
    }

    private void loadInstitutionProperties(InstitutionArchive institutionArchive)
    {
        try
        {
            Path path = archiveConfiguration.getInstitutionFolder(institutionArchive.getInstitutionId()).resolve(INSTITUTION_FILE);
            institutionArchive.readJson(JsonHelper.loadJsonFile(path), dateFormat);
        } 
        catch (Exception e)
        {
            log.warn("Could not load institution properties for archived institution {}", institutionArchive.getInstitution());
        }
    }
    
    private void storeInstitutionProperties(InstitutionArchive institutionArchive)
    {
        Path path = archiveConfiguration.getInstitutionFolder(institutionArchive.getInstitutionId()).resolve(INSTITUTION_FILE);
        try
        {
            JsonHelper.storeJsonFile(institutionArchive.writeJson(dateFormat), path);
        } catch (IOException e)
        {
            log.error("Could not store properties for archived institution {}", institutionArchive, e);
        }
    }
    
    protected BaseVersion loadBaseVersion(InstitutionArchive institutionArchive, String baseId) throws ArchiveException 
    {  
        final VersionKey baseVersionKey = new VersionKey(baseId);
        final BaseVersion baseVersionArchive = new BaseVersion(baseVersionKey, new VersionInfo(), institutionArchive);
        loadVersionProperties(baseVersionArchive);
        institutionArchive.addBaseVersion(baseVersionArchive);
        
        // load updates 
        for (String updateId : getArchivedUpdateVersionIds(institutionArchive.getInstitutionId(), baseId))
        {
            log.debug("found update: {}", updateId);
            final VersionKey updateVersionKey = new VersionKey(baseId, updateId);
            final UpdateVersion updateVersionArchive = new UpdateVersion(updateVersionKey, new VersionInfo(), baseVersionArchive);
            loadVersionProperties(updateVersionArchive);
            baseVersionArchive.addUpdate(updateVersionArchive);
        }
        
        return baseVersionArchive;
    }
    
    private void loadVersionProperties(AbstractVersion version) throws ArchiveException
    {
        final String institutionId = version.getInstitutionId();
        final VersionKey versionKey = version.getVersionKey();
        try
        {
            Path path = archiveConfiguration.getVersionFolder(institutionId, versionKey).resolve(VERSION_FILE);
            version.readJson(JsonHelper.loadJsonFile(path), dateFormat);
        } 
        catch (Exception e)
        {
            log.warn("Could not load version properties for archived version {} of institution {}",
                    versionKey, institutionId);
        }
        
        version.setUploadFile(
                findFile(ProcessStep.upload, institutionId, versionKey));
        version.setWorkingFormatFile(
                findFile(ProcessStep.workingFormat, institutionId, versionKey));
        version.setSolrFormatFile(
                findFile(ProcessStep.solrFormat, institutionId, versionKey));
    }
    
    private void storeVersionProperties(AbstractVersion version) throws IOException
    {
        Path path = archiveConfiguration.getVersionFolder(version.getInstitutionId(), version.getVersionKey())
                .resolve(VERSION_FILE);
        JsonHelper.storeJsonFile(version.writeJson(dateFormat), path);
    }
    
    protected List<String> getArchivedBaseVersionIds(String institutionId) 
    {
        return listDirectoryNames(archiveConfiguration.getInstitutionFolder(institutionId), true);
    }
    
    protected List<String> getArchivedUpdateVersionIds(String institutionId, String baseId) 
    {
        final List<String> updateIds;
        Path updatesBasePath = archiveConfiguration.getUpdatesBasePath(institutionId, baseId);
        if (Files.exists(updatesBasePath))
        {
            updateIds = listDirectoryNames(updatesBasePath, true);
        } else
        {
            updateIds = Collections.emptyList(); 
        }        
        return updateIds;
    }
}    

