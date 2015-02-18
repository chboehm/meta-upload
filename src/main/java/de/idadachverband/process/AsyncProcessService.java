package de.idadachverband.process;

import de.idadachverband.archive.IdaInputArchiver;
import de.idadachverband.institution.IdaInstitutionBean;
import de.idadachverband.result.NotificationException;
import de.idadachverband.result.ResultNotifier;
import de.idadachverband.solr.SolrService;
import de.idadachverband.transform.IdaTransformer;
import de.idadachverband.transform.TransformationBean;
import de.idadachverband.transform.xslt.WorkingFormatToSolrDocumentTransformer;
import lombok.extern.slf4j.Slf4j;
import org.apache.solr.client.solrj.SolrServerException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.AsyncResult;

import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Date;
import java.util.concurrent.Future;

import static org.apache.solr.client.solrj.impl.HttpSolrServer.RemoteSolrException;

/**
 * Created by boehm on 08.10.14.
 */
@Named
@Slf4j
public class AsyncProcessService
{
    final private IdaTransformer transformationStrategy;

    final private ResultNotifier resultMailSender;

    final private IdaInputArchiver idaInputArchiver;

    final private WorkingFormatToSolrDocumentTransformer workingFormatTransformer;

    @Inject
    public AsyncProcessService(IdaTransformer transformationStrategy, WorkingFormatToSolrDocumentTransformer workingFormatTransformer, ResultNotifier resultMailSender, IdaInputArchiver idaInputArchiver)
    {
        this.transformationStrategy = transformationStrategy;
        this.resultMailSender = resultMailSender;
        this.idaInputArchiver = idaInputArchiver;
        this.workingFormatTransformer = workingFormatTransformer;
    }

    @Async
    public Future<Void> processAsynchronous(Path input, IdaInstitutionBean institution, SolrService solr, TransformationBean transformationBean) throws NotificationException
    {
        log.debug("Start asynchronous processing of: {}", transformationBean);
        AsyncResult<Void> asyncResult = new AsyncResult<>(null);
        try
        {
            Path path = idaInputArchiver.archiveFile(input, institution.getInstitutionName());

            Path workingFormatFile = transformToWorkingFormat(path, institution);
            path = idaInputArchiver.archiveFile(workingFormatFile, institution.getInstitutionName());

            final Path transformedFile = transformToSolrFormat(institution, path);
            path = idaInputArchiver.archiveFile(transformedFile, institution.getInstitutionName());

            transformationBean.setTransformedFile(path);
            upateSolr(solr, transformationBean, path, institution);

        } catch (TransformerException | IOException | SolrServerException | RemoteSolrException | NullPointerException e)
        {
            log.warn("Transformation failed: ", e);
            transformationBean.setException(e);
        } finally
        {
            transformationBean.setEndTime(new Date());
            final String transformationMessagesFromUpload = transformationStrategy.getTransformationMessages();
            final String transformationMessagesToSolrFormat = workingFormatTransformer.getTransformationMessages();
            transformationBean.setTransformationMessages(" - Transformation to working format: " + transformationMessagesFromUpload + "\n - Transformation to solr format: " + transformationMessagesToSolrFormat);
            resultMailSender.notify(transformationBean);
        }

        log.debug("* End of asynchronous processing of: {}", transformationBean);
        return asyncResult;
    }


    public Path transformToWorkingFormat(Path inputFile, IdaInstitutionBean institution) throws TransformerException, IOException
    {
        log.info("Start transformation of: {} for: {} to working format", inputFile, institution);
        final long start = System.currentTimeMillis();
        final Path unzippedFile = idaInputArchiver.readArchivedFile(inputFile);
        final Path transformedFile = transformationStrategy.transform(unzippedFile, institution);
        final long end = System.currentTimeMillis();
        log.info("Transformation of: {} for: {} to working format took: {} seconds", inputFile, institution, (end - start) / 1000);
        return transformedFile;
    }

    public Path transformToSolrFormat(IdaInstitutionBean institution, Path inputFile) throws TransformerException, IOException
    {
        log.info("Start transformation of: {} for: {} to Solr format", inputFile, institution);
        final long start = System.currentTimeMillis();
        final Path unzippedFile = idaInputArchiver.readArchivedFile(inputFile);
        final Path transformedFile = workingFormatTransformer.transform(unzippedFile, institution);
        final long end = System.currentTimeMillis();
        log.info("Transformation of: {} for: {} to Solr format took: {} seconds", inputFile, institution, (end - start) / 1000);
        return transformedFile;
    }

    public void upateSolr(SolrService solr, TransformationBean transformationBean, Path inputFile, IdaInstitutionBean institution) throws IOException, SolrServerException
    {
        log.info("Start Solr update of core: {} for: {} with file: {}", solr, institution, inputFile);
        final long start = System.currentTimeMillis();

        final Path unzippedFile = idaInputArchiver.readArchivedFile(inputFile);

        if (!institution.isIncrementalUpdate())
        {
            solr.deleteInstitution(institution.getInstitutionName());
        }

        String solrResult = solr.update(unzippedFile);
        transformationBean.setSolrResponse(solrResult);

        final long end = System.currentTimeMillis();
        log.info("Solr update of core: {} for: {} with file: {} took: {} seconds.", solr, institution, inputFile, (end - start) / 1000);

        log.debug("Solr result {}", solrResult);
    }

}
