package de.idadachverband.job;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Future;

/**
 * Created by boehm on 02.10.14.
 */
@Slf4j
@RequiredArgsConstructor
public enum JobProgressState
{
    SUCCESS("Erfolgreich"), NOTFOUND("Unbekannt"), PROCESSING("In Verarbeitung"), 
    CANCELLED("Abgebrochen"), FAILURE("Fehlgeschlagen"), NOTSTARTED("In Wartestellung");

    @Getter
    private final String description;
    

    /**
     * Get current state by key
     *
     * @param jobBean
     * @return State of job
     */
    public static JobProgressState getState(JobBean jobBean)
    {
        if (jobBean == null)
        {
            return NOTFOUND;
        }

        Future<?> future = jobBean.getFuture();
        if (future == null)
        {
            log.warn("Probably not started '{}' in '{}'.", jobBean);
            return NOTSTARTED;
        } 
        else if (future.isCancelled())
        {
            return CANCELLED;
        }
        else if (jobBean.getException() != null)
        {
            log.debug("Job {} failed.", jobBean);
            return FAILURE;
        } 
        else if (future.isDone())
        {
            return SUCCESS;
        } 
        return PROCESSING;
    }
}
