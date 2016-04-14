package de.idadachverband.vufind;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;

import lombok.Getter;

import org.springframework.core.convert.converter.Converter;

import com.google.common.base.Optional;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.solr.SolrCore;

@Named
public class VufindInstanceManager implements Converter<String, VufindInstanceBean>
{
    @Getter
    private Map<String, VufindInstanceBean> instanceMap;
 
    @Inject
    public VufindInstanceManager(Set<VufindInstanceBean> vufindInstanceSet)
    {
        this.instanceMap = new HashMap<>(Maps.uniqueIndex(vufindInstanceSet, vufindInstance -> vufindInstance.getInstanceId()));
    }
    
    @Override
    public VufindInstanceBean convert(String instanceId)
    {
        VufindInstanceBean bean = instanceMap.get(instanceId);
        if (bean == null)
        {
            throw new IllegalArgumentException("Did not find Vufind instance with instanceId " + instanceId);
        }
        return bean;
    }
    
    public Iterable<VufindInstanceBean> getInstancesByCoreName(String coreName)
    {
        return Iterables.filter(instanceMap.values(), vufindInstance -> coreName.equals(vufindInstance.getCoreName()));
    }
    
    public String getInstancePublicUrl(String coreName)
    {
        Optional<VufindInstanceBean> optionalInstance =
                Iterables.tryFind(instanceMap.values(), vufindInstance -> coreName.equals(vufindInstance.getCoreName()));
        return optionalInstance.isPresent() ? optionalInstance.get().getPublicUrl() : ""; 
    }
    
    public void updateInstances(SolrCore solrCore, IdaInstitutionBean institution)
    {
        for (VufindInstanceBean vufindInstance : getInstancesByCoreName(solrCore.getName()))
        {
            vufindInstance.updateInstance(institution);
        }
    }
}
