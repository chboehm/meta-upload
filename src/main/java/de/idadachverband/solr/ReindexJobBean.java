package de.idadachverband.solr;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import de.idadachverband.archive.VersionKey;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.job.JobBean;

@Getter
public class ReindexJobBean extends JobBean
{
    private final List<SolrUpdateBean> solrUpdates = new ArrayList<>();
   
    private final SolrCore solrService;
    private final IdaInstitutionBean institution;
    private final VersionKey version;

    
    public ReindexJobBean(SolrCore solrService,
            IdaInstitutionBean institution, VersionKey version)
    {
        this.solrService = solrService;
        this.institution = institution;
        this.version = version;
        setJobName(String.format("Indizierung der Version %s von %s nach %s", 
                version, institution.getInstitutionName(), solrService.getName()));
    }
    
    @Override
    public void buildResultMessage(StringBuilder sb)
    {
        for (SolrUpdateBean solrUpdate : solrUpdates)
        {
            solrUpdate.buildResultMessage(sb);
        }
    }
}
