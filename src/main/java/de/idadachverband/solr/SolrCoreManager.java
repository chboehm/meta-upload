package de.idadachverband.solr;

import org.springframework.core.convert.converter.Converter;

import com.google.common.collect.Maps;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Provides {@link SolrCore} from name of solr core.
 * <p/>
 * Created by boehm on 23.09.14.
 */
@Named
public class SolrCoreManager implements Converter<String, SolrCore>
{
    private Map<String, SolrCore> coreMap;
    
    @Inject
    public SolrCoreManager(Set<SolrCore> solrCoreSet)
    {
        this.coreMap = new HashMap<>(Maps.uniqueIndex(solrCoreSet, solrCore -> solrCore.getName()));
    }
    
    @Override
    public SolrCore convert(String name)
    {
        if (!coreMap.containsKey(name))
            throw new IllegalArgumentException("Did not find solr core with name " + name);
        return coreMap.get(name);
    }
    
    public Collection<SolrCore> getAllSolrCores()
    {
        return coreMap.values();
    }
}
