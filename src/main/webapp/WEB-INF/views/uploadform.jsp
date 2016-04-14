<!DOCTYPE html>
<html>
<%@include file="head.jspf" %>
<body>
    <c:set var="currentTab" value="upload"/>
    <%@include file="menu.jspf" %>
    <div class="main" id="page-upload">
        <h1>
            Bitte laden Sie eine Datei hoch.
            <sec:authorize access="hasAuthority('admin')">
                Sie sind Admin!
            </sec:authorize>
        </h1>
        <form:form method="post" action="upload" enctype="multipart/form-data" modelAttribute="transformation">
            <form:label path="file">Datei zum Hochladen</form:label>
            <form:input path="file" type="file" name="file"/><br/>
            <br />
            <div style="display: none;"><sec:authorize access="hasAuthority('admin')"></div><div></sec:authorize>
                <form:label path="solr">Solr Core</form:label>
                <form:select path="solr">
                	<c:forEach items="${solrCores}" var="solrCore">
    					<c:choose>
    						<c:when test="${solrCore eq defaultSolrCore}">
	    						<form:option value="${solrCore}" selected="selected"/>
	    					</c:when>
	    					<c:otherwise>
	    						<form:option value="${solrCore}"/>
	    					</c:otherwise>
	    				</c:choose>
					</c:forEach>
                </form:select><br />
                <br/>
            </div>
            <div style="display: none;"><c:if test="${institutions.size() gt 1}"></div><div></c:if>
                <form:label path="institution">Einrichtung</form:label>
                <form:select path="institution">
                	<c:forEach items="${institutions}" var="institution">
    					<form:option value="${institution.institutionId}" label="${institution.institutionName}"/>
					</c:forEach>
                </form:select><br />
                <br/>
            </div>
            <c:if test="${allowIncremental}">
            	<form:radiobutton path="update" value="true" label="Schrittweises Update"/><label class="buttonDescription">Neue Daten seit letztem Update hinzufügen.</label><br/>
            	<form:radiobutton path="update" value="false" label="Volles Update"/><label class="buttonDescription">Existierende Daten löschen und durch neue Daten ersetzen.</label><br/>
                <br/>
            </c:if>
            <div>
	            <input type="submit" value="Senden" class="btn" />
	            <label class="buttonDescription">Klicken Sie auf Senden um die Datei hochzuladen!</label>
            </div>
        </form:form>
        <c:if test="${not empty latestUploads}">
	        <div class="latest-uploads">
	        	<h2>Letzte Uploads <span class="infoBubble" title="Neue Uploads werden erst nach ihrer Verarbeitung sichtbar.">i</span></h2>
	        	<ul>
        		<c:forEach var="upload" items="${latestUploads}">
        			<li>
		        		<span><fmt:formatDate value="${upload.date}" type="both" dateStyle="short" timeStyle="short"/>: <em>${upload.uploadedFileName}</em></span>
	        		    <span>von ${upload.userName}</span>
	        		    <c:if test="${institutions.size() gt 1}">
	        		    	<span>für ${upload.institution.institutionName}</span>
	        		    </c:if>
        			</li>
        		</c:forEach>
	        	</ul>
	        </div>
        </c:if>
    </div>
    <%@include file="footer.jspf" %>
</body>
</html>
