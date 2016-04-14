package de.idadachverband.archive;

import java.nio.file.Path;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;

import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

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

    protected final VersionInfo origin;
    
    protected final InstitutionArchive institutionArchive;
        
    private Path uploadFile, workingFormatFile, solrFormatFile;
    
    
    public JsonObjectBuilder writeJson(SimpleDateFormat dateFormat)
    {
        return origin.writeJson(dateFormat);
    }
    
    public void readJson(JsonObject object, SimpleDateFormat dateFormat) throws ArchiveException, ParseException
    {
        origin.readJson(object, dateFormat);
    } 

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
