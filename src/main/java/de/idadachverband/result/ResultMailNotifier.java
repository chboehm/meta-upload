package de.idadachverband.result;

import de.idadachverband.job.JobBean;
import de.idadachverband.job.JobProgressState;
import de.idadachverband.user.IdaUser;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import javax.inject.Inject;
import javax.inject.Named;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Sends notification of result to logged in user.
 * Created by boehm on 02.10.14.
 */
@Named
@Slf4j
public class ResultMailNotifier implements ResultNotifier
{
    private final MailSender mailSender;

    private final Configuration freemarkerMailConfiguration;
    
    private final IdaUrlHelper idaUrlHelper;

    @Value("${result.mail.from}")
    private String mailFrom;
    @Value("${result.mail.admin}")
    private String mailAdmin;
    @Value("${result.mail.subject}")
    private String subject;

    @Inject
    public ResultMailNotifier(MailSender mailSender, Configuration freemarkerMailConfiguration, IdaUrlHelper idaUrlHelper)
    {
        this.mailSender = mailSender;
        this.freemarkerMailConfiguration = freemarkerMailConfiguration;
        this.idaUrlHelper = idaUrlHelper;
    }

    public void notify(JobBean jobBean) throws NotificationException
    {      
        IdaUser user = jobBean.getUser();
        if (!user.isAdmin())
        {
            sendMail(jobBean, "Admin", mailAdmin, true);
        }
        sendMail(jobBean, user.getUsername(), user.getEmail(), user.isAdmin());
    }

    private void sendMail(JobBean jobBean, String userName, String eMail, boolean isAdmin)
            throws NotificationException
    {
        String result = !jobBean.isFailure() ? 
                JobProgressState.SUCCESS.getDescription() : 
                jobBean.getProgressState().getDescription(); 
        
        SimpleMailMessage email = new SimpleMailMessage();
        email.setTo(eMail);
        email.setSubject(subject + " - " + result + ": " + jobBean.getJobName());
        email.setFrom(mailFrom);

        Map<String, Object> model = new HashMap<>();
        model.put("user", userName);
        model.put("admin", isAdmin);
        model.put("job", jobBean);
        model.put("result", result);
        model.put("message", jobBean.getResultMessage());
        model.put("urlHelper", idaUrlHelper);

        try
        {
            final String text = FreeMarkerTemplateUtils.processTemplateIntoString(freemarkerMailConfiguration.getTemplate("result-mail.ftl"), model);
            email.setText(text);
            log.debug("Send mail {}", email);
            mailSender.send(email);
        } catch (IOException | TemplateException | MailException e)
        {
            log.warn("Failed to send mail {} for job {}", email, jobBean, e);
            throw new NotificationException(e);
        }
    }
}
