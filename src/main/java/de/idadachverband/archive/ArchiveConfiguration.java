package de.idadachverband.archive;

import de.idadachverband.process.ProcessFileConfiguration;
import de.idadachverband.process.ProcessStep;
import lombok.Getter;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Created by boehm on 19.02.15.
 */
@Named
class ArchiveConfiguration {

    final private String incrementalFolder = "incremental";
    
    @Getter
    private final Path basePath;
    private final ProcessFileConfiguration processFileConfiguration;
    
    @Getter
    final private int maxArchivedVersions;
    
    @Inject
    public ArchiveConfiguration(Path archivePath, Integer maxArchivedVersions) throws IOException
    {
        this.basePath = archivePath;
        this.processFileConfiguration = new ProcessFileConfiguration(archivePath);
        Files.createDirectories(this.basePath);
        this.maxArchivedVersions = maxArchivedVersions;
    }
    
    public Path getStepFolder(ProcessStep step, String institutionId, VersionKey version)
    {
        return (version.isBaseVersion())
            ? getBaseStepFolder(step, institutionId, version.getBaseId())
            : getIncrementalStepFolder(step, institutionId, version.getBaseId(), version.getUpdateId());
    }
    
    public Path getBaseStepFolder(ProcessStep step, String institutionId, String versionId)
    {
        return checkPath(processFileConfiguration.getStepFolder(step, Paths.get(institutionId, versionId).toString()));
    }
    
    public Path getIncrementalStepFolder(ProcessStep step, String institutionId, String versionId, String updateId)
    {
        return checkPath(processFileConfiguration.getStepFolder(step, 
                Paths.get(institutionId, versionId, incrementalFolder, updateId).toString()));
    }
    
    public Path getInstitutionFolder(String institutionId) 
    {
    	return checkPath(basePath.resolve(institutionId));
    }
    
    public Path getVersionFolder(String institutionId, VersionKey version)
    {
        return (version.isBaseVersion()) ? 
                getBaseVersionFolder(institutionId, version.getBaseId()) :
                getUpdateVersionFolder(institutionId, version.getBaseId(), version.getUpdateId());
    }

    public Path getBaseVersionFolder(String institutionId, String versionId)
    {
        return checkPath(basePath.resolve(institutionId).resolve(versionId));
    }
    
    public Path getUpdatesBasePath(String institutionId, String versionId) 
    {
        return checkPath(basePath.resolve(institutionId).resolve(versionId).resolve(incrementalFolder));
    }
    
    public Path getUpdateVersionFolder(String institutionId, String versionId, String updateId) 
    {
        return checkPath(basePath.resolve(institutionId).resolve(versionId).resolve(incrementalFolder).resolve(updateId));
    }
    
    protected Path checkPath(Path path) 
    {
        final Path normalizedPath = path.normalize();
        if (!normalizedPath.startsWith(basePath)) 
        {
            throw new IllegalArgumentException("Path: " + path + " points out of archive directory!");
        }
        return path;
    }
}