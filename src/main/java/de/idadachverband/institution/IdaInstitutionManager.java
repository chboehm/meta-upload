package de.idadachverband.institution;

import org.springframework.core.convert.converter.Converter;

import com.google.common.collect.Maps;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import lombok.Getter;

/**
 * Provides {@link IdaInstitutionBean} from institutionId of institution
 * Created by boehm on 23.09.14.
 */
@Named
public class IdaInstitutionManager implements Converter<String, IdaInstitutionBean>
{
    @Getter
    private Map<String, IdaInstitutionBean> institutionsMap;
 
    @Inject
    public IdaInstitutionManager(Set<IdaInstitutionBean> institutionsSet)
    {
        this.institutionsMap = new HashMap<>(Maps.uniqueIndex(institutionsSet, institution -> institution.getInstitutionId()));
    }
    
    @Override
    public IdaInstitutionBean convert(String id)
    {
        IdaInstitutionBean bean = institutionsMap.get(id);
        if (bean == null)
        {
            throw new IllegalArgumentException("Did not find institution with institutionId " + id);
        }
        return bean;
    }
}
