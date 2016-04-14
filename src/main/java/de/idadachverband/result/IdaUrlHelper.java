package de.idadachverband.result;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Value;

import de.idadachverband.transform.TransformationBean;
import de.idadachverband.vufind.VufindInstanceManager;

@Named
public class IdaUrlHelper
{
    @Value("${uploadTool.url}")
    private String uploadToolUrl;
    
    @Inject
    private VufindInstanceManager vufindInstanceManager;
    
    public String getArchiveUrl(String institutionId)
    {
        return uploadToolUrl + "/archive/" + institutionId; 
    }
    
    public String getArchiveUrl(TransformationBean transformation)
    {
        return getArchiveUrl(transformation.getInstitution().getInstitutionId());
    }
    
    public String getVufindInstanceUrl(String coreName)
    {
        return vufindInstanceManager.getInstancePublicUrl(coreName);
    }
}
