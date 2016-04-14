<%--
  Created by IntelliJ IDEA.
  User: boehm
  Date: 23.01.15
  Time: 15:31
  To change this template use File | Settings | File Templates.
--%>
<!DOCTYPE html>
<html>
<%@include file="head.jspf" %>
<body>
	<c:set var="currentTab" value="jobs"/>
    <%@include file="menu.jspf" %>
    <c:set var="newLine" value="&#10;"/>
    <div class="main" id="page-jobs">
        <h1>Laufende Jobs</h1>
        <ul class="jobs">
            <c:forEach var="job" items="${runningJobs}">
                <li>(<fmt:formatDate value="${job.startTime}" type="both" dateStyle="short" timeStyle="short"/>) ${job.jobName}
                	<span class="infoBubble" title="Started by '${job.user}', Job-ID: ${job.jobId}${newLine}${job.resultMessage}">i</span>
                	<a href="cancel/${job.jobId}" class="btn">Cancel</a>
                </li>
            </c:forEach>
        </ul>
        <h1>Abgeschlossene Jobs</h1>
        <ul class="jobs">
            <c:forEach var="job" items="${stoppedJobs}">
                <li>(<fmt:formatDate value="${job.startTime}" type="both" dateStyle="short" timeStyle="short"/> - <fmt:formatDate value="${job.endTime}" type="time" timeStyle="short"/>) ${job.jobName}: ${job.progressState} 
                	<span class="infoBubble" title="Started by '${job.user}', Job-ID: ${job.jobId}${newLine}${job.resultMessage}${job.exception != null ? newLine.concat('Exception: ').concat(job.exception) : ''}">i</span>
                	<div class="options">
                		<a href="delete/${job.jobId}" class="delete" title="Delete"></a>
                	</div>
                </li>
            </c:forEach>
        </ul>
        <br />
        <a href="clear" class="btn">Löschen</a>
    </div>
    <%@include file="footer.jspf" %>
</body>
</html>
