Hallo ${user}!

<#if job.failure>
Die Daten konnten nicht umgewandelt werden. <#if !admin>Bitte kontaktieren Sie die Servicestelle.</#if>
<#else>
Die Daten wurden erfolgreich eingespielt. <#if job.transformation??>Bitte überprüfen Sie die Daten auf ${urlHelper.getVufindInstanceUrl(job.transformation.solrCore.name)}.</#if>
</#if>
<#if admin>

<#if job.exception??>
Exception:
${job.exception}

</#if>
Job Message:
${message}
</#if>

Daten-Upload META-Katalog