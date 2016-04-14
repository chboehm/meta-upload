package de.idadachverband.solr;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.solr.client.solrj.SolrServerException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.idadachverband.archive.ArchiveService;
import de.idadachverband.archive.BaseVersion;
import de.idadachverband.archive.IdaInputArchiver;
import de.idadachverband.archive.InstitutionArchive;
import de.idadachverband.archive.UpdateVersion;
import de.idadachverband.archive.VersionInfo;
import de.idadachverband.archive.VersionKey;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.job.JobExecutionService;
import de.idadachverband.vufind.VufindInstanceManager;

public class SolrUpdateServiceTest
{
    final Path versionPath = Paths.get("update.xml");
    final Path updatePath1 = Paths.get("iupdate1.xml");
    final Path updatePath2 = Paths.get("iupdate2.xml");
	
    @Mock
    private IdaInputArchiver idaInputArchiver;

    @Mock
    private ArchiveService archiveService;
    
    @Mock
    private JobExecutionService jobExecutionService;
     
    @Mock
    private VufindInstanceManager vufindInstanceManager;
    
    private final String coreName = "corename";
    
    @Mock 
    private SolrCore solrCore;
    
    private final String institutionId = "institution1";
    
    @Mock
    private IdaInstitutionBean institution;
    
    private InstitutionArchive institutionArchive; 
    
    private BaseVersion baseVersion;
    
    private UpdateVersion updateVersion1, updateVersion2;

    private SolrUpdateService cut;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        
        when(institution.getInstitutionId()).thenReturn(institutionId);
        when(solrCore.getName()).thenReturn(coreName);
        when(idaInputArchiver.uncompressToTemporaryFile(Mockito.any(Path.class))).thenReturn(Paths.get("uncompressed.tmp"));
        
        // archived versions
        institutionArchive = new InstitutionArchive(institution);
        when(archiveService.getArchive(institution)).thenReturn(institutionArchive);
        
        baseVersion = new BaseVersion(new VersionKey(1, 0), new VersionInfo(), institutionArchive);
        baseVersion.setSolrFormatFile(versionPath);
        institutionArchive.addBaseVersion(baseVersion);
        
        updateVersion1 = new UpdateVersion(new VersionKey(1, 1), new VersionInfo(), baseVersion);
        updateVersion1.setSolrFormatFile(updatePath1);
        baseVersion.addUpdate(updateVersion1);

        updateVersion2 = new UpdateVersion(new VersionKey(1, 2), new VersionInfo(), baseVersion);
        updateVersion2.setSolrFormatFile(updatePath2);
        baseVersion.addUpdate(updateVersion2);
        
        cut = new SolrUpdateService(archiveService, idaInputArchiver, jobExecutionService, vufindInstanceManager);
    }

    @Test
    public void rollbackInstitution() throws Exception
    {
        VersionKey version = new VersionKey(1,2); 
        when(archiveService.getIndexedVersionKey(institution, solrCore)).thenReturn(version);
        
        cut.rollbackInstitution(institution, solrCore);

        verify(solrCore, times(1)).deleteInstitution(institutionId);
        verify(solrCore, times(3)).update(Mockito.any(Path.class));
    }

    @Test
    public void updateSolrRollback() throws Exception 
    {
        VersionKey version = new VersionKey(1,0); 
        when(archiveService.getIndexedVersionKey(institution, solrCore)).thenReturn(version);
    	
    	final Path invalidUpdatePath = Paths.get("invalidupdate.xml");
    	when(solrCore.update(invalidUpdatePath)).thenThrow(new SolrServerException("updates fails"));
    	
    	SolrUpdateBean solrUpdate = new SolrUpdateBean(solrCore, institution, invalidUpdatePath, VersionInfo.ofUpload("", ""), false);
    	try 
    	{
    	    cut.updateSolr(solrUpdate, true);
    	} catch (SolrServerException e)
    	{
    	    // expected
    	}
    	
    	verify(solrCore).update(invalidUpdatePath);
    	verify(solrCore).update(versionPath); // rollback to archived version
    	verify(solrCore, times(2)).deleteInstitution(institutionId);
    }
}