package de.idadachverband.archive;

import java.text.ParseException;
import java.text.SimpleDateFormat;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;

@Getter
@RequiredArgsConstructor
public class InstitutionIndexState
{
    private final InstitutionArchive institutionArchive;
    
    private final String coreName;
    
    @Delegate
    private VersionKey versionKey = VersionKey.NO_VERSION;
    
    private VersionInfo origin;
    
    public void update(VersionKey indexedVersionKey, VersionInfo origin)
    {
        this.versionKey = indexedVersionKey;
        this.origin = origin;
    }

    public JsonObjectBuilder writeJson(SimpleDateFormat dateFormat)
    {
        JsonObjectBuilder object = Json.createObjectBuilder();
        object.add("version", versionKey.toString());
        if (origin != null) 
        {
            object.add("origin", origin.writeJson(dateFormat));
        }
        return object;
    }
    
    public void readJson(JsonObject object, SimpleDateFormat dateFormat) throws ArchiveException, ParseException
    {
        this.versionKey = VersionKey.parse(object.getString("version", "")); 
        if (object.containsKey("origin"))
        {
            this.origin = new VersionInfo();
            this.origin.readJson(object.getJsonObject("origin"), dateFormat); 
        }
    }
    
}
