package de.idadachverband.user;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.institution.IdaInstitutionManager;
import de.idadachverband.solr.SolrCore;
import de.idadachverband.solr.SolrCoreManager;

import javax.inject.Inject;
import javax.inject.Named;

/**
 * Utility class to get UserDetails
 * Created by boehm on 13.11.14.
 */
@Named
@Slf4j
public class UserService
{
    public static final String ADMIN_ROLE = "admin"; 
     
    private IdaInstitutionManager institutionManager;
    
    private SolrCoreManager solrCoreManager;
    
    private SolrCore defaultSolrCore;

    @Value("${result.mail.from}")
    private String mailFrom;
    
    @Inject
    public UserService(IdaInstitutionManager institutionManager, 
            SolrCoreManager solrCoreManager, 
            SolrCore defaultSolrCore)
    {
        this.institutionManager = institutionManager;
        this.solrCoreManager = solrCoreManager;
        this.defaultSolrCore = defaultSolrCore;
    }

    public IdaUser getUser()
    {
        UserDetails userDetails = getUserDetails();
        IdaUser user = new IdaUser(userDetails.getUsername());
        
        for (GrantedAuthority authority : userDetails.getAuthorities())
        {
            String userDetail = authority.getAuthority();
            if (userDetail.equals(ADMIN_ROLE))
            {
                user.setAdmin(true);
                user.getInstitutionsSet().addAll(institutionManager.getAllInstitutions());
                user.getSolrCores().addAll(solrCoreManager.getAllSolrCores());
            }
            else if (userDetail.contains("@"))
            {
                user.setEmail(userDetail);
                log.debug("Found email {} for user {}", userDetail, user);
            }
            else if (userDetail.startsWith("#"))
            {
                String coreName = userDetail.substring(1);
                SolrCore solrService = solrCoreManager.convert(coreName);
                if (solrService != null)
                {
                    log.debug("Found solr service {} for user {}", solrService, user);
                    user.getSolrCores().add(solrService);
                } else
                {
                    log.warn("Invalid solr core: {} in user roles.", coreName);
                }
            }
            else
            {
                IdaInstitutionBean institution = institutionManager.convert(userDetail);
                if (institution != null)
                {
                    log.debug("Found institution {} for user {}", institution, user);
                    user.getInstitutionsSet().add(institution);
                } else
                {
                    log.warn("Invalid institution ID: {} in user roles.", userDetail);
                }
            }
        }
        
        if (user.getEmail() == null)
        {
            user.setEmail(mailFrom);
            log.warn("Did not find email for user {}", user);
        }
        if (user.getSolrCores().isEmpty())
        {
            user.getSolrCores().add(defaultSolrCore);
            log.debug("Set default solr service {} for user {}", defaultSolrCore, user);
        }
        
        return user;
    }
    
    /**
     * @return username of authenticated user
     * @throws Exception
     */
    public String getUsername()
    {
        UserDetails userDetails;
        try
        {
            userDetails = getUserDetails();
        } catch (AuthenticationNotFoundException e)
        {
            return "";
        }
        final String username = userDetails.getUsername();
        log.debug("User name is {}", username);
        return username;
    }
      
    protected UserDetails getUserDetails() throws AuthenticationNotFoundException
    {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null)
        {
            log.warn("Can not find user!");
            throw new AuthenticationNotFoundException();
        }
        return (UserDetails) authentication.getPrincipal();
    }
}
