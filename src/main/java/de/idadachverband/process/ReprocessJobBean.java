package de.idadachverband.process;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;
import de.idadachverband.archive.VersionKey;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.job.JobBean;
import de.idadachverband.solr.SolrCore;
import de.idadachverband.transform.TransformationBean;

@Getter
public class ReprocessJobBean extends JobBean
{
    private final List<TransformationBean> transformations = new ArrayList<>();
    
    private final SolrCore solrService;
    private final IdaInstitutionBean institution;
    private final VersionKey version;
    

    public ReprocessJobBean(SolrCore solrService,
            IdaInstitutionBean institution, VersionKey version)
    {
        setJobName(String.format("Neu-Verarbeitung der Version %s von %s nach %s", 
                version, institution.getInstitutionName(), solrService.getName()));
        this.solrService = solrService;
        this.institution = institution;
        this.version = version;
    }
    
    @Override
    public void buildResultMessage(StringBuilder sb)
    {
        for (TransformationBean transformationBean : transformations)
        {
            transformationBean.buildResultMessage(sb);
        }
    }
}
