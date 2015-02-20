package de.idadachverband.transform.xslt;

import de.idadachverband.institution.IdaInstitutionBean;
import lombok.Cleanup;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Named;
import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

/**
 * Transforms XML files to Solr input format.
 * Created by boehm on 17.07.14.
 */
@Slf4j
@Named
public class XsltTransformer extends AbstractXsltTransformer
{
    /**
     * Implementation class for TransformerFactory is defined in:
     * {@code src/main/resources/META-INF/services/javax.xml.transform.TransformerFactory}
     * Currently @see net.sf.saxon.TransformerFactoryImpl is used because it is the only one that works for me.
     */
    public XsltTransformer()
    {
        //System.setProperty("javax.xml.transform.TransformerFactory", "net.sf.saxon.TransformerFactoryImpl");
    }

    @Override
    public void transform(Path input, Path output, IdaInstitutionBean institutionBean) throws TransformerException, IOException
    {
        transform(input, output, institutionBean.getTransformationRecipeFile(), institutionBean.getInstitutionName());
    }

    /**
     * Transforms input to to Ida standard format, only stored in temporary file, and then to Solr input xml.
     *
     * @param input           the input XML to be transformed
     * @param outputFile
     *@param institutionXsl  XSL for institution corresponding to input XML
     * @param institutionName   @return Solr input file. Can be added to Solr via update request
     * @throws TransformerException
     * @throws IOException
     */
    private void transform(final Path input, Path outputFile, Path institutionXsl, String institutionName) throws TransformerException, IOException
    {
        log.debug("Transform: {} to: {} using XSL: {}", input, outputFile, institutionXsl);
        @Cleanup
        InputStream in = Files.newInputStream(input, StandardOpenOption.READ);
        @Cleanup
        OutputStream out = Files.newOutputStream(outputFile, StandardOpenOption.CREATE, StandardOpenOption.WRITE);

        transformInstitution(in, out, institutionXsl);
        log.info("Transformed to Working format");

    }
}