package de.idadachverband.process;

import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Created by boehm on 20.02.15.
 */
@Named
public class ProcessFileConfiguration
{
    @Getter
    final private Path basePath;

    @Inject
    public ProcessFileConfiguration(Path processBasePath) throws IOException
    {
        this.basePath = processBasePath;
        Files.createDirectories(this.basePath);
    }
    

    public Path getPath(String fileName, ProcessStep step, String key)
    {
        final Path folder = getStepFolder(step, key);
        final Path path = folder.resolve(fileName);

        return path;
    }

    public Path getStepFolder(ProcessStep step, String key)
    {
        return basePath.resolve(key).resolve(step.getName());
    }
}
