<%--
  Created by IntelliJ IDEA.
  User: boehm
  Date: 17.07.14
  Time: 15:09
  To change this template use File | Settings | File Templates.
--%>
<!DOCTYPE html>
<html>
<%@ include file="head.jspf" %>
<%@ page import="de.idadachverband.job.JobProgressState" %>
<body>
    <%@include file="menu.jspf" %>
    <div class="main">
        <spring:url value="/files/solrFormat/" var="fileUrl"/>
        <spring:url value="/result/getResult" var="stateUrl"/>
        <spring:url value="/resources/images/waiting.gif" var="waiting"/>

        <div id="waiting">
            <div style="display: inline-block">
                <img src="${waiting}">
            </div>
            <div style="display: inline-block; padding-left: 20px; vertical-align: top">
            	<h1>Die Daten werden verarbeitet...</h1>
                <p>Die Verarbeitung kann eine Weile dauern. Sie können dieses Fenster jetzt schließen.<br />
                Sie werden über das Ergebnis per Mail informiert.</p>
                <sec:authorize access="hasAuthority('admin')">
	                Job ID: ${jobId}
                </sec:authorize>
            </div>
        </div>

        <div id="success" style="display: none;">
            <h1>Verarbeitung der Daten erfolgreich!</h1>
            <p>Die Daten wurden erfolgreich eingespielt. Bitte überprüfe Sie die Daten auf <a id="instanceLink" target="_blank"></a></p>
           	<%-- <a id="filelink" href="${fileUrl}" target="_blank">Transformierte XML-Datei</a> --%>
        </div>

        <div id="failure" style="display: none;">
            <h1>Fehler bei der Verarbeitung!</h1>
            <p>Die Daten konnten nicht umgewandelt werden. Bitte kontaktieren Sie die Servicestelle.</p>
            <sec:authorize access="hasAuthority('admin')">
            	<h2>Exception: </h2>
            	<div id="exception"></div>
            </sec:authorize>
        </div>
        
        <sec:authorize access="hasAuthority('admin')">
	        <div id="jobMessage" style="display: none;">
	            <h2>Job Message: </h2>
	            <div id="message"></div>
	        </div>
        </sec:authorize>

        <script type="application/javascript">
            successCallback = function (v) {
                console.log(v);
                if (v.state === "<%= JobProgressState.SUCCESS %>") {
                    var instanceLink = jQuery("#instanceLink");
                    instanceLink.attr("href", v.instanceUrl);
                    instanceLink.text(v.instanceUrl);

                    /* var fileLink = jQuery("#filelink");
                    var url = fileLink.attr("href");
                    fileLink.attr("href", url + v.path); */
                    
                    jQuery("#success").toggle();
                    done();
                }
                else if (v.state === "<%= JobProgressState.FAILURE %>") {
                    var failure = jQuery("#failure");
                    jQuery("#exception").text(v.exception);
                    failure.toggle();
                    done();
                }
                if (v.message) {
                	var jobMessage = jQuery("#jobMessage");
	                jQuery("#message").html(v.message);
    	            jobMessage.toggle(true);
                }
            };

            done = function () {
                clearInterval(pollInterval);
                jQuery("#waiting").toggle();
            };

            poll = function () {
                jQuery.getJSON(
                    "${stateUrl}",
                    {"jobId": "${jobId}"},
                    successCallback
                );
            };

            pollInterval = setInterval(poll, 15000);
        </script>
    </div>
    <%@include file="footer.jspf" %>
</body>
</html>
