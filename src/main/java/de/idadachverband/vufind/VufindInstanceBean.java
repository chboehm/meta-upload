package de.idadachverband.vufind;

import de.idadachverband.institution.IdaInstitutionBean;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "instanceId")
public class VufindInstanceBean
{
    private final String instanceId;
    
    private final String url;

    private final String publicUrl;
    
    private final String coreName;
    
    private final HierarchyCacheDeleteMethod hierarchyCacheDeleteMethod;

    public VufindInstanceBean(String instanceId, String url)
    {
        this(instanceId, url, url, instanceId, "/HierarchyHelper?institution=$institutionId", "$institutionId");
    }
    
    public VufindInstanceBean(String instanceId, String url, String publicUrl, String coreName,
            String hierachyCacheDeletePath, String institutionIdPlaceholder)
    {
        this.instanceId = instanceId;
        this.url = url;
        this.publicUrl = publicUrl;
        this.coreName = coreName;
        this.hierarchyCacheDeleteMethod = new HierarchyCacheDeleteMethod(
                url + hierachyCacheDeletePath, institutionIdPlaceholder);
    }
    
    public synchronized void updateInstance(IdaInstitutionBean institution)
    {
        hierarchyCacheDeleteMethod.deleteHierarchyCache(institution);
    }
}
