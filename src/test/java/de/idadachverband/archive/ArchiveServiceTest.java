package de.idadachverband.archive;

import static org.mockito.Mockito.when;

import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.common.collect.Lists;

import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.institution.IdaInstitutionManager;
import de.idadachverband.process.ProcessFileConfiguration;
import de.idadachverband.process.ProcessStep;
import de.idadachverband.user.UserService;
import de.idadachverband.utils.Directories;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ArchiveServiceTest
{
    private ArchiveService cut;
    private Path archivePath, processFilePath;
    
    @Mock
    private IdaInstitutionManager idaInstitutionConverter;
    
    @Mock
    private IdaInstitutionBean institutionBean1, institutionBean2;
    
    @Mock
    private IdaInputArchiver idaInputArchiver;
    
    @Mock
    private UserService userService;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        archivePath = Paths.get(this.getClass().getClassLoader().getResource("archive").toURI());
        processFilePath = Files.createTempDirectory("archiveTest");
        when(userService.getUsername()).thenReturn("testUser");
        cut = new ArchiveService(
                new ArchiveConfiguration(archivePath, 1), 
                new ProcessFileConfiguration(processFilePath),
                new SimpleDateFormat("yyyyMMdd_HHmmss"), 
                idaInputArchiver, userService);
        
        when(institutionBean1.getInstitutionId()).thenReturn("institution1");
        when(institutionBean2.getInstitutionId()).thenReturn("institution2");
    }
    
    @AfterClass
    public void cleanup() throws Exception
    {
        Directories.delete(processFilePath);
    }
          
    @Test
    public void getArchivedBaseVersionIds() throws Exception
    {
        List<String> actual = cut.getArchivedBaseVersionIds("institution2");
        assertThat(actual, is(equalTo(Arrays.asList("v0002", "v0003"))));
    }
    
    @Test
    public void getUpdateIds() throws Exception
    {
        List<String> actual = cut.getArchivedUpdateVersionIds("institution1", "v0001");
        assertThat(actual, is(equalTo(Arrays.asList("u0001", "u0002"))));
    }
    
//    @Test
//    public void getUpdateIdsUpTo() throws Exception
//    {
//        List<String> actual = cut.getUpdateIdsUpTo("corename", "institution1", "v0001_20150428_083025", "u0001_20150428_093025");
//        assertThat(actual, is(equalTo(Arrays.asList("u0001_20150428_093025"))));
//    }

    @Test
    public void getArchive() throws Exception
    {
        final List<InstitutionArchive> institutions = cut.getArchives(Lists.newArrayList(institutionBean1, institutionBean2));
        assertThat(institutions.size(), is(equalTo(2)));
        final InstitutionArchive institution1 = institutions.get(0);
        assertThat(institution1.getInstitutionId(), is(equalTo("institution1")));
        
        assertThat(institution1.getBaseVersions().size(), is(1));

        final BaseVersion version = institution1.getBaseVersion(1);
        assertThat(version.getUploadFile().getFileName().toString(), is(equalTo("update.zip")));
        
        assertThat(version.getUpdates().size(), is(2));
        final UpdateVersion update2 = version.getUpdate(2);
        assertThat(update2.getUpdateNumber(), is(equalTo(2)));
        assertThat(update2.getUploadFile().getFileName().toString(), is(equalTo("iupdate2.zip")));
        
        final BaseVersion version2 = institutions.get(1).getBaseVersion(3);
        assertThat(version2.getBaseNumber(), is(equalTo(3)));
        assertThat(version2.getUploadFile().getFileName().toString(), is(equalTo("update3.zip")));
    }
    
    @Test
    public void findFile() throws Exception
    {
        Path actual = cut.findFile(ProcessStep.upload, "institution1", new VersionKey(1,0));
        assertThat(actual, is(equalTo(archivePath.resolve("institution1/v0001/upload/update.zip"))));
        
        actual = cut.findFile(ProcessStep.solrFormat, "institution1", new VersionKey(1,1));
        assertThat(actual, is(equalTo(archivePath.resolve("institution1/v0001/incremental/u0001/solr/iupdate1.zip"))));
    }
    
    @Test(expectedExceptions = ArchiveException.class)
    public void findFileFailsForMissingVersion() throws Exception
    {
        cut.findFile(ProcessStep.upload, "institution1", new VersionKey(4,0));
    }
    
    @Test
    public void createNextBaseVersion() throws Exception
    {
        BaseVersion actual = cut.getArchive(institutionBean2).createNextBaseVersion(new VersionInfo());
        assertThat(actual.getVersionKey(), is(equalTo(new VersionKey(4,0))));
    }
    
    @Test
    public void createNextUpdateVersion() throws Exception
    {
        AbstractVersion actual = cut.getArchive(institutionBean1).createNextVersion(new VersionInfo(), true);
        assertThat(actual.getVersionKey(), is(equalTo(new VersionKey(1,3))));
    }

}