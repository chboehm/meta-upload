package de.idadachverband.archive;

import java.nio.file.Path;
import java.util.Collection;

import de.idadachverband.institution.IdaInstitutionBean;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Delegate;

@Data
@EqualsAndHashCode(of = "versionKey")
public class AbstractVersion
{
    @Delegate
    protected final VersionKey versionKey;

    @Delegate
    protected final VersionInfo origin;
    
    protected final InstitutionArchive institutionArchive;
        
    private Path uploadFile, workingFormatFile, solrFormatFile;
    

    public String getInstitutionId()
    {
        return institutionArchive.getInstitutionId();
    }
    
    public IdaInstitutionBean getInstitution()
    {
        return institutionArchive.getInstitution();
    }
    
    public Collection<InstitutionIndexState> getIndexStates()
    {
        return institutionArchive.getIndexStates(versionKey);
    }
}
