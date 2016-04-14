package de.idadachverband.upload;

import de.idadachverband.archive.AbstractVersion;
import de.idadachverband.archive.ArchiveService;
import de.idadachverband.archive.VersionInfo;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.process.ProcessJobBean;
import de.idadachverband.process.ProcessService;
import de.idadachverband.solr.SolrCore;
import de.idadachverband.user.AuthenticationNotFoundException;
import de.idadachverband.user.IdaUser;
import de.idadachverband.user.UserService;
import de.idadachverband.utils.ToStringIgnoringCaseComparator;
import lombok.extern.slf4j.Slf4j;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.inject.Inject;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Created by boehm on 23.09.14.
 */
@Controller
@RequestMapping(value = "/upload")
@Slf4j
public class AsyncFileUploadController
{
    @Inject
    private UserService userService;
    
    @Inject
    private ProcessService processService;
    
    @Inject 
    private ArchiveService archiveService;

    @Inject
    private SimpleDateFormat dateFormat;
    
    @Inject
    private SolrCore defaultSolrCore;

    @RequestMapping(method = RequestMethod.GET)
    public ModelAndView prepareUploadForm() throws AuthenticationNotFoundException
    {
        ModelAndView mav = new ModelAndView("uploadform");
        IdaUser user = userService.getUser();
        
        List<IdaInstitutionBean> institutions = new ArrayList<IdaInstitutionBean>(user.getInstitutionsSet());
        Collections.sort(institutions, ToStringIgnoringCaseComparator.INSTANCE);
        List<SolrCore> solrCores = new ArrayList<SolrCore>(user.getSolrCores());
        Collections.sort(solrCores, ToStringIgnoringCaseComparator.INSTANCE);
   
        boolean allowIncremental = false;
        boolean incrementalDefault = false;
        for (IdaInstitutionBean institution : institutions)
        {
            allowIncremental = (institution.isIncrementalUpdateAllowed()) ? true : allowIncremental;
            incrementalDefault = (institution.isIncrementalUpdate()) ? true : incrementalDefault;
        }
        UploadFormBean uploadFormBean = new UploadFormBean();
        uploadFormBean.setUpdate(incrementalDefault);
        
        List<AbstractVersion> latestUploadVersions = archiveService.getLatestUploadVersions(institutions, 4);
        
        mav.addObject("institutions", institutions);
        mav.addObject("solrCores", solrCores);
        mav.addObject("defaultSolrCore", defaultSolrCore);
        mav.addObject("allowIncremental", allowIncremental);
        mav.addObject("incrementalDefault", incrementalDefault); 
        mav.addObject("latestUploads", latestUploadVersions);
        mav.addObject("transformation", uploadFormBean);

        return mav;
    }


    /**
     * TODO Should check authorization for user
     *
     * @param uploadFormBean
     * @param authentication
     * @param map
     * @return
     */
    @RequestMapping(method = RequestMethod.POST)
    public Callable<String> handleFormUpload(@ModelAttribute final UploadFormBean uploadFormBean, final RedirectAttributes map)
    {
        log.info("Attempt to process uploaded file: {}", uploadFormBean);
        return new Callable<String>()
        {
            @Override
            public String call()
            {
                MultipartFile file = uploadFormBean.getFile();
                IdaUser user = userService.getUser(); 
                log.info("User: {} uploaded file: {}", user, file);
                Path tmpPath;
                try
                {
                    IdaInstitutionBean institution = uploadFormBean.getInstitution();
                    SolrCore solr = uploadFormBean.getSolr();
                    if (!user.getSolrCores().contains(solr) || !user.getInstitutionsSet().contains(institution))
                    {
                        throw new AccessDeniedException(solr.getName() + "/" + institution.getInstitutionId());
                    }
                    if (uploadFormBean.isUpdate() && !institution.isIncrementalUpdateAllowed())
                    {
                        throw new IllegalArgumentException("Institution " + institution + " does not support incremental updates!");
                    }

                    if (file.isEmpty())
                    {
                        throw new IllegalArgumentException("File is empty!");
                    }
                    tmpPath = moveToTempFile(file, institution.getInstitutionId());

                    ProcessJobBean jobBean = processService.processAsync(tmpPath, institution, solr, uploadFormBean.isUpdate(), 
                            VersionInfo.ofUpload(user.getUsername(), file.getOriginalFilename()));
                    map.addAttribute("jobId", jobBean.getJobId());

                    return "redirect:result/success";
                } catch (Exception e)
                {
                    log.warn("Upload of file {} failed", file, e);
                    map.addFlashAttribute("cause", e.getCause());
                    map.addFlashAttribute("message", e.getMessage());
                    map.addFlashAttribute("stacktrace", e.getStackTrace());
                    return "redirect:result/failure";
                }
            }
        };
    }

    private Path moveToTempFile(MultipartFile file, String institutionName) throws IOException
    {
        final String prefix = institutionName + "-" + dateFormat.format(new Date()) + "-upload-";
        final String suffix = file.getContentType().toLowerCase().equals("application/zip") ? ".tmp.zip" : ".tmp";
        Path tmpPath = Files.createTempFile(prefix, suffix);
        file.transferTo(tmpPath.toFile());
        log.debug("Moved uploaded file: {} to: {}", file, tmpPath);
        return tmpPath;
    }
}
