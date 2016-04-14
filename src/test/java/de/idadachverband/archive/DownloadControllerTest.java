package de.idadachverband.archive;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.FileSystemResource;
import org.springframework.security.access.AccessDeniedException;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.process.ProcessStep;
import de.idadachverband.user.IdaUser;
import de.idadachverband.user.UserService;

import javax.servlet.http.HttpServletResponse;

import java.nio.file.Path;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DownloadControllerTest
{
    @InjectMocks
    private DownloadController cut;
    
    @Mock
    private ArchiveService archiveService;
    
    @Mock
    private UserService userService;
    
    @Mock
    private IdaUser user;
    
    @Mock 
    private IdaInstitutionBean institutionBean;

    @Mock
    private HttpServletResponse response;

    @BeforeMethod
    public void setUp() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(userService.getUser()).thenReturn(user);
        when(user.getInstitutionsSet()).thenReturn(Collections.singleton(institutionBean));
        when(institutionBean.getInstitutionId()).thenReturn("institution");
    }

    @Test(expectedExceptions = IllegalArgumentException.class)
    public void downloadWithInvalidStep() throws Exception
    {
        cut.downloadVersion("wrongFormat", institutionBean, "1.0", response);
    }
    
    @Test
    public void downloadSuccess() throws Exception
    {
        VersionKey version = new VersionKey(1,0);
        when(archiveService.findFile(ProcessStep.upload, "institution", version)).thenReturn(mock(Path.class));

        FileSystemResource actual = cut.downloadVersion("upload", institutionBean, "1.0", response);
        assertThat(actual, notNullValue());
    }

    @SuppressWarnings("unchecked")
    @Test(expectedExceptions = ArchiveException.class)
    public void downloadFailure() throws Exception
    {
        VersionKey version = new VersionKey(1,0);
        when(archiveService.findFile(ProcessStep.upload, "institution", version)).thenThrow(ArchiveException.class);
        cut.downloadVersion("upload", institutionBean, "1.0", response);
    }
    
    @Test(expectedExceptions = AccessDeniedException.class)
    public void downloadWrongInstitution() throws ArchiveException
    {
        when(user.getInstitutionsSet()).thenReturn(Collections.<IdaInstitutionBean>emptySet());
        cut.downloadVersion("upload", institutionBean, "1.0", response);
    }

}