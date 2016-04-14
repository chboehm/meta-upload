package de.idadachverband.archive;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;

import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.experimental.Delegate;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.solr.SolrCore;

@RequiredArgsConstructor
public class InstitutionArchive
{
    @Getter
    @Delegate
    private final IdaInstitutionBean institution;
    
    private SortedMap<Integer, BaseVersion> baseVersions = Collections.synchronizedSortedMap(new TreeMap<>());
    
    private Map<String, InstitutionIndexState> indexedVersions = new HashMap<>();
    
        
    public Collection<BaseVersion> getBaseVersions()
    {
        return baseVersions.values();
    }
    
    public BaseVersion getBaseVersion(int baseNumber)
    {
        return baseVersions.get(baseNumber);
    }

    public BaseVersion getBaseVersion(VersionKey versionKey)
    {
        return getBaseVersion(versionKey.getBaseNumber());
    }
        
    public synchronized int getOldestBaseNumber()
    {
        return (baseVersions.isEmpty()) ? 0 : baseVersions.firstKey();
    }
    
    public synchronized int getLatestBaseNumber()
    {
        return (baseVersions.isEmpty()) ? 0 : baseVersions.lastKey();
    }
    
    public synchronized AbstractVersion getLatestVersion()
    {
        BaseVersion baseVersion = getLatestBaseVersion();
        if (baseVersion == null) 
            return null;
        
        return baseVersion.hasUpdates() ? baseVersion.getLatestUpdate() : baseVersion;
    }
    
    public synchronized BaseVersion getLatestBaseVersion()
    {
        return (baseVersions.isEmpty()) ? null : baseVersions.get(baseVersions.lastKey());
    }
    
    public synchronized AbstractVersion createNextVersion(VersionInfo nextVersionInfo, boolean incrementalUpdate)
    {
        return (incrementalUpdate && !baseVersions.isEmpty()) ?
                getLatestBaseVersion().createNextUpdateVersion(nextVersionInfo) :
                createNextBaseVersion(nextVersionInfo);
    }
    
    public synchronized BaseVersion createNextBaseVersion(VersionInfo nextVersionInfo)
    {
        VersionKey nextVersionKey = new VersionKey(getLatestBaseNumber() + 1, 0);
        BaseVersion nextBaseVersion = new BaseVersion(nextVersionKey, nextVersionInfo, this);
        addBaseVersion(nextBaseVersion);
        return nextBaseVersion;
    }
        
    public synchronized InstitutionIndexState getIndexState(SolrCore solrCore)
    {
        final String coreName = solrCore.getName();
        if (!indexedVersions.containsKey(coreName))
        {
            indexedVersions.put(coreName, new InstitutionIndexState(this, coreName));
        }
        return indexedVersions.get(coreName);
    }
    
    public synchronized Collection<InstitutionIndexState> getIndexStates(VersionKey versionKey)
    {
        return Collections2.filter(indexedVersions.values(), indexState -> indexState.getVersionKey().equals(versionKey));
    }
    
    public synchronized List<AbstractVersion> getAllVersions()
    {
        ArrayList<AbstractVersion> allVersions = new ArrayList<>();
        for (BaseVersion baseVersion : getBaseVersions())
        {
            allVersions.add(baseVersion);
            allVersions.addAll(baseVersion.getUpdates());
        }
        return allVersions;
    }
    
    public List<AbstractVersion> getAllVersionsDescending()
    {
        return Lists.reverse(getAllVersions());
    }
    
    public JsonObjectBuilder writeJson(SimpleDateFormat dateFormat)
    {
        JsonObjectBuilder indexedObject = Json.createObjectBuilder();
        for (Entry<String, InstitutionIndexState> entry: indexedVersions.entrySet())
        {
            indexedObject.add(entry.getKey(), entry.getValue().writeJson(dateFormat));
        }
        JsonObjectBuilder object = Json.createObjectBuilder();
        object.add("indexed", indexedObject);
        return object;
    }
    
    public void readJson(JsonObject object, SimpleDateFormat dateFormat) throws ArchiveException, ParseException
    {
        JsonObject indexedObject = object.getJsonObject("indexed");
        indexedVersions.clear();
        for (Entry<String, JsonValue> entry: indexedObject.entrySet())
        {
            InstitutionIndexState indexedVersionState = new InstitutionIndexState(this, entry.getKey());
            indexedVersionState.readJson((JsonObject)entry.getValue(), dateFormat);
            indexedVersions.put(entry.getKey(), indexedVersionState);
        }
    }
    
    @Override
    public String toString()
    {
        return institution.getInstitutionName();
    }
    
    public void addBaseVersion(BaseVersion baseVersion) 
    {
        baseVersions.put(baseVersion.getBaseNumber(), baseVersion);
    }
    
    public void removeVersion(VersionKey versionKey)
    {
        if (versionKey.isBaseVersion())
        {
            removeBaseVersion(versionKey.getBaseNumber());
        }
        else if (baseVersions.containsKey(versionKey.getBaseNumber()))
        {
            getBaseVersion(versionKey).removeUpdate(versionKey.getUpdateNumber());
        }
    }
    
    public void removeBaseVersion(int baseNumber)
    {
        baseVersions.remove(baseNumber);
    }
}
