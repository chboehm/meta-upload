package de.idadachverband.archive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.solr.SolrCore;
import de.idadachverband.utils.ToStringIgnoringCaseComparator;

import javax.inject.Inject;


/**
 * Created by boehm on 30.10.14.
 */
@Controller
@RequestMapping("/archive")
public class ArchiveController
{
    private final ArchiveService archiveService;
    
    private final Set<IdaInstitutionBean> institutionsSet;

    private final Set<SolrCore> solrCoreSet;

    @Inject
    public ArchiveController(ArchiveService archiveService, Set<IdaInstitutionBean> institutionsSet, Set<SolrCore> solrCoreSet)
    {
        this.archiveService = archiveService;
        this.institutionsSet = institutionsSet;
        this.solrCoreSet = solrCoreSet;
    }

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView listInstitutions(ModelAndView mav) throws ArchiveException
    {
        List<IdaInstitutionBean> sortedInstitutions = new ArrayList<IdaInstitutionBean>(institutionsSet);
        Collections.sort(sortedInstitutions, ToStringIgnoringCaseComparator.INSTANCE);
        mav.setViewName("institutionArchiveList");
        mav.addObject("institutions", archiveService.getArchives(sortedInstitutions));
        return mav;
    }

    @RequestMapping(value = "{institution}", method = RequestMethod.GET)
    public ModelAndView listVersions(
            @PathVariable("institution") IdaInstitutionBean institution,
            ModelAndView mav) throws ArchiveException
    {
        mav.setViewName("institutionArchive");
        mav.addObject("institution", archiveService.getArchive(institution));
        mav.addObject("solrCores", solrCoreSet);
        return mav;
    }
    
    @RequestMapping(value = "delete/{institution}/{version:.+}", method = RequestMethod.GET)
    public String deleteVersion(
            @PathVariable("institution") IdaInstitutionBean institution,
            @PathVariable("version") String version,
            ModelMap map) throws ArchiveException 
    {
        archiveService.deleteVersion(institution, VersionKey.parse(version));
        return "redirect:/archive/" + institution.getInstitutionId();
    }
}
