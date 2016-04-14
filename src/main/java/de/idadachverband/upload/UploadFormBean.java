package de.idadachverband.upload;

import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.solr.SolrCore;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;


/**
 * Created by boehm on 04.09.14.
 */
@Data
public class UploadFormBean
{
    private IdaInstitutionBean institution;
    private SolrCore solr;
    private MultipartFile file;
    private boolean update;
}
