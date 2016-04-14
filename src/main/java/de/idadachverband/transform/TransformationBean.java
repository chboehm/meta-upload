package de.idadachverband.transform;

import lombok.Getter;
import lombok.Setter;

import java.nio.file.Path;

import de.idadachverband.archive.BaseVersion;
import de.idadachverband.archive.UpdateVersion;
import de.idadachverband.archive.VersionInfo;
import de.idadachverband.archive.VersionKey;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.solr.SolrUpdateBean;
import de.idadachverband.solr.SolrCore;

/**
 * Bean to hold transformation details.
 * Created by boehm on 09.10.14.
 */
@Getter
@Setter
public class TransformationBean extends SolrUpdateBean
{
    private final Path transformationInput;
    
    private VersionKey archivedVersion;
    
    private String transformationWorkingFormatMessages = "";
    
    private String transformationSolrFormatMessages = "";
      
    public TransformationBean(
                SolrCore solrService, 
                IdaInstitutionBean institution, 
                Path transformationInput, 
                VersionInfo origin,
                boolean incrementalUpdate)
    {
        super(solrService, institution, null, origin, incrementalUpdate);
        this.transformationInput = transformationInput;
    }

    @Override
    public void buildResultMessage(StringBuilder sb)
    {
        if (!transformationWorkingFormatMessages.isEmpty())
        {
            sb.append("Transformation to working format: ");
            sb.append(transformationWorkingFormatMessages);
            sb.append('\n');
        }
        if (!transformationSolrFormatMessages.isEmpty())
        {
            sb.append("Transformation to Solr format: ");
            sb.append(transformationSolrFormatMessages);
            sb.append('\n');
        }
        super.buildResultMessage(sb);
    }
       
    public static TransformationBean fromBaseVersion(BaseVersion baseVersion, SolrCore solr, String userName)
    {
        return new TransformationBean(solr, baseVersion.getInstitution(), 
                baseVersion.getUploadFile(), 
                VersionInfo.ofReprocess(userName, baseVersion.getVersionKey()),
                false);
    }

    public static TransformationBean fromUpdateVersion(UpdateVersion updateVersion, SolrCore solr, String userName)
    {
        return new TransformationBean(solr, updateVersion.getInstitution(), 
                updateVersion.getUploadFile(), 
                VersionInfo.ofReprocess(userName, updateVersion.getVersionKey()),
                true);
    }
}
