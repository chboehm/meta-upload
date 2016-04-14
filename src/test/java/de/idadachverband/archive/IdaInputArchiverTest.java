package de.idadachverband.archive;

import de.idadachverband.utils.Directories;
import de.idadachverband.utils.ZipService;

import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class IdaInputArchiverTest
{
    @InjectMocks
    private IdaInputArchiver cut;

    private Path input;
    private Path target;

    @Mock
    private Path archivePath;

    @Mock
    private ZipService zipService;

    @BeforeMethod
    public void setUp() throws Exception
    {
        initMocks(this);
        input = Files.createTempFile("input", ".xml");
        target = Files.createTempDirectory("idaInputArchiverTest");
    }
    
    @AfterMethod
    public void cleanup() throws IOException
    {
        Directories.delete(target);
        Files.delete(input);
    }    

    @Test
    public void archiveFile() throws Exception
    {
        Path path = cut.archiveFile(input, target, "");
        Path expectedPath = target.resolve(input.getFileName().toString() + ".zip");
        assertThat(path, equalTo(expectedPath));
        verify(zipService, times(1)).zip(any(Path.class), any(Path.class));
    }
}