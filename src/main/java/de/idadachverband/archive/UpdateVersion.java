package de.idadachverband.archive;

import lombok.Getter;

@Getter
public class UpdateVersion extends AbstractVersion
{
    private final BaseVersion baseVersionArchive;
    
    public UpdateVersion(VersionKey versionKey, VersionInfo versionInfo, BaseVersion baseVersion)
    {
        super(versionKey, versionInfo, baseVersion.institutionArchive);
        this.baseVersionArchive = baseVersion;
    }

}
