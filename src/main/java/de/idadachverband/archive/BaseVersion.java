package de.idadachverband.archive;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.common.collect.Collections2;

public class BaseVersion extends AbstractVersion
{
    private final SortedMap<Integer, UpdateVersion> updates = Collections.synchronizedSortedMap(new TreeMap<>());

    public BaseVersion(VersionKey versionKey, VersionInfo versionInfo, InstitutionArchive institutionArchive)
    {
        super(versionKey, versionInfo, institutionArchive);
    }

    public boolean hasUpdates()
    {
        return !updates.isEmpty();
    }
    
    public Collection<UpdateVersion> getUpdates()
    {
        return updates.values();
    }
    
    public UpdateVersion getUpdate(int updateNumber)
    {
        return updates.get(updateNumber);
    }

    public UpdateVersion getUpdate(VersionKey versionKey)
    {
        return getUpdate(versionKey.getUpdateNumber());
    }
    
    public Collection<UpdateVersion> getUpdatesIn(int fromUpdateNumber, int toUpdateNumber)
    {
        return Collections2.filter(updates.values(), 
                update -> fromUpdateNumber <= update.getUpdateNumber() && update.getUpdateNumber() <= toUpdateNumber);
    }
    
    public synchronized int getLatestUpdateNumber()
    {
        return updates.isEmpty() ? 0 : updates.lastKey();
    }
    
    public synchronized UpdateVersion getLatestUpdate()
    {
        return updates.isEmpty() ? null : updates.get(updates.lastKey());
    }
    
    public synchronized UpdateVersion createNextUpdateVersion(VersionInfo nextVersionInfo)
    {
        VersionKey nextVersionKey = new VersionKey(versionKey.getBaseNumber(), getLatestUpdateNumber() + 1);
        UpdateVersion nextUpdateVersion = new UpdateVersion(nextVersionKey, nextVersionInfo, this);
        addUpdate(nextUpdateVersion);
        return nextUpdateVersion;
    }
    
    public void addUpdate(UpdateVersion updateVersionBean)
    {
        updates.put(updateVersionBean.getUpdateNumber(), updateVersionBean);
    }

    public void removeUpdate(int updateNumber)
    {
        updates.remove(updateNumber);
    }
    
}
