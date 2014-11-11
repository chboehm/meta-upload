package de.idadachverband.process;

import de.idadachverband.archive.ZipService;
import de.idadachverband.solr.SolrService;
import de.idadachverband.transform.TransformationBean;
import de.idadachverband.transform.TransformationProgressService;
import de.idadachverband.upload.IdaInstitutionBean;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.inject.Named;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Future;

/**
 * Created by boehm on 09.10.14.
 */
@Named
@Slf4j
public class ProcessService
{
    @Inject
    private AsyncProcessService asyncProcessService;

    @Inject
    private TransformationProgressService transformationProgressService;

    @Inject
    private ZipService zipService;

    public TransformationBean process(File input, IdaInstitutionBean institution, SolrService solr) throws IOException
    {
        return processFile(zipService.unzip(input), institution, solr);
    }

    private TransformationBean processFile(InputStream input, IdaInstitutionBean institution, SolrService solr)
    {
        TransformationBean transformationBean = new TransformationBean();
        transformationProgressService.add(transformationBean);
        log.debug("====================== Call async method");
        Future<?> voidFuture = asyncProcessService.processAsynchronous(input, institution, solr, transformationBean);
        transformationBean.setFuture(voidFuture);
        log.debug("====================== Async method returned");
        return transformationBean;
    }
}
