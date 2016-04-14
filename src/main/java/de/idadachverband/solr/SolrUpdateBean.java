package de.idadachverband.solr;

import java.nio.file.Path;
import java.util.UUID;

import de.idadachverband.archive.VersionInfo;
import de.idadachverband.institution.IdaInstitutionBean;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(of = "key")
public class SolrUpdateBean
{
    private final String key;
    private final SolrCore solrCore;
    private final IdaInstitutionBean institution;
    private final VersionInfo origin;
    private final boolean incrementalUpdate;
    
    private Path solrInput;
    private String solrMessage = "";
    
    public SolrUpdateBean(SolrCore solrService, IdaInstitutionBean institution, Path solrInput, VersionInfo origin, boolean incrementalUpdate)
    {
        this.key = UUID.randomUUID().toString();
        this.solrCore = solrService;
        this.institution = institution;
        this.solrInput = solrInput;
        this.origin = origin;
        this.incrementalUpdate = incrementalUpdate;
    }
    
    public String getCoreName()
    {
        return solrCore.getName();
    }
    
    public String getInstitutionId() 
    {
        return institution.getInstitutionId();
    }
    
    public String getInstitutionName()
    {
        return institution.getInstitutionName();
    }
    
    public String getResultMessage() 
    {
        StringBuilder sb = new StringBuilder();
        buildResultMessage(sb);
        return sb.toString();
    }

    public void buildResultMessage(StringBuilder sb)
    {
        if (!solrMessage.isEmpty())
        {
            sb.append("Solr update: ");
            sb.append(solrMessage);
            sb.append('\n');
        }
    }
}
