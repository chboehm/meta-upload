package de.idadachverband.archive;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class VersionInfo implements Comparable<VersionInfo>
{
    public enum Action
    {
        UPLOAD, REPROCESS, REINDEX, UNKNOWN
    }
    
    private Date date;
    
    private Action action;
    
    private String userName;
    
    private String uploadedFileName; 
     
    private VersionKey originalVersion;
    
    public String getDescription()
    {
        switch (action) {
        case UPLOAD:
            return "user '" + userName + "' uploaded file '" + uploadedFileName + "'";
        case REINDEX:
            return "user '" + userName + "' re-indexed archived upload version " + originalVersion;
        case REPROCESS:
            return "user '" + userName + "' re-processed archived upload version " + originalVersion;
        default:
            return "";
        }
    } 
    
    public JsonObjectBuilder writeJson(SimpleDateFormat dateFormat)
    {
        JsonObjectBuilder object = Json.createObjectBuilder();
        object.add("date", dateFormat.format(date));
        object.add("user", userName);
        object.add("action", action.toString());
        if (originalVersion != null && originalVersion != VersionKey.NO_VERSION) 
        {
            object.add("originalVersion", originalVersion.toString());
        }
        if (uploadedFileName != null && !uploadedFileName.isEmpty())
        {
            object.add("uploadedFileName", uploadedFileName);
        }
        return object;
    }
    
    public void readJson(JsonObject object, SimpleDateFormat dateFormat) throws ArchiveException, ParseException
    {
        this.date = dateFormat.parse(object.getString("date"));
        this.userName = object.getString("user");
        this.action= Action.valueOf(object.getString("action"));
        this.originalVersion = VersionKey.parse(object.getString("originalVersion", "")); 
        this.uploadedFileName = object.getString("uploadedFileName", ""); 
    }

    @Override
    public int compareTo(VersionInfo other)
    {
        return this.date.compareTo(other.date);
    }
    
    public static VersionInfo ofUpload(String userName, String uploadedFileName)
    {
        return new VersionInfo(new Date(), Action.UPLOAD, userName, uploadedFileName, VersionKey.NO_VERSION);
    }
    
    public static VersionInfo ofReprocess(String userName, VersionKey originalVersion)
    {
        return new VersionInfo(new Date(), Action.REPROCESS, userName, "", originalVersion);
    }
    
    public static VersionInfo ofReindex(String userName, VersionKey originalVersion)
    {
        return new VersionInfo(new Date(), Action.REINDEX, userName, "", originalVersion);
    }
}
