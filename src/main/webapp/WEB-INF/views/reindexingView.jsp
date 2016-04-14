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
        <spring:url value="/files/" var="fileUrl"/>
        <spring:url value="/result/getResult" var="stateUrl"/>
        <spring:url value="/resources/images/waiting.gif" var="waiting"/>

        <div id="waiting">
            <div style="display: inline-block">
                <img src="${waiting}">
            </div>
            <div style="display: inline-block; padding-left: 20px; vertical-align: top">
                <h1>Die Version ${version} von ${institution} wird nach ${core} indiziert...</h1>
                <p>Die Verarbeitung kann eine Weile dauern. Sie können dieses Fenster jetzt schließen.<br />
                Sie werden über das Ergebnis per Mail informiert.</p>
                Job ID: ${jobId}
            </div>
        </div>

        <div id="success" style="display: none;">
            <h1>Indizierung der Version ${version} von ${institution} nach ${core} erfolgreich!</h1>
            <p>Die Daten wurden erfolgreich eingespielt.</p>
        </div>

        <div id="failure" style="display: none;">
            <h1>Indizierung der Version ${version} von ${institution} nach ${core} fehlgeschlagen!</h1>
            <h2>Exception: </h2>
            <div id="exception"></div>
        </div>

		<div id="jobMessage" style="display: none;">
	            <h2>Job Message: </h2>
	            <div id="message"></div>
        </div>

        <script type="application/javascript">
            successCallback = function (v) {
                console.log(v);
                if (v.state === "<%= JobProgressState.SUCCESS %>") {
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
