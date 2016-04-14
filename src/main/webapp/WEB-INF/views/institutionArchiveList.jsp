<%--
  Created by IntelliJ IDEA.
  User: boehm
  Date: 30.10.14
  Time: 16:21
  To change this template use File | Settings | File Templates.
--%>
<!DOCTYPE html>
<html>
<%@include file="head.jspf" %>
<body>	
	<c:set var="currentTab" value="archive"/>
	<%@include file="menu.jspf" %>
    <div class="main" id="page-archiveList">
        <h1>Archiv</h1>
        <ul class="institutionList">
            <spring:url value="/solr/reindex" var="reindexLink"/>
            <spring:url value="/process/reprocess" var="reprocessLink"/>
            <spring:url value="/files/upload" var="uploadLink"/>
            <spring:url value="/files/workingFormat" var="workingFormatLink"/>
            <spring:url value="/files/solrFormat" var="solrLink"/>
            <spring:url value="/archive/delete" var="deleteLink"/>
            <spring:url value="/archive" var="archiveLink"/>
            <c:forEach var="institution" items="${institutions}">
                <li>
                	<a href="${archiveLink}/${institution.institutionId}">${institution.institutionName}</a>
                	<c:set var="latestVersion" value="${institution.latestVersion}"/>
                	<c:if test="${latestVersion != null}">
                		<div class=latest>
                			<span>Letzte Version: ${latestVersion.versionKey} (<fmt:formatDate value="${latestVersion.origin.date}" type="both" dateStyle="short" timeStyle="short"/>)</span>
	                		<span class="infoBubble" title="${latestVersion.origin.description}">i</span>
                		</div>
                	</c:if>
                </li>
            </c:forEach>
        </ul>
    </div>
    <%@include file="footer.jspf" %>
</body>
</html>
