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
        <h1>Archiv: ${institution.institutionName}</h1>
        <c:set var="versions" value="${institution.allVersionsDescending}"/>
        <c:if test="${empty versions}">
        	<p>Keine Versionen archiviert.</p>
        </c:if>
        <ul class="versionList">
            <spring:url value="/solr/reindex" var="reindexLink"/>
            <spring:url value="/process/reprocess" var="reprocessLink"/>
            <spring:url value="/files/upload" var="uploadLink"/>
            <spring:url value="/files/workingFormat" var="workingFormatLink"/>
            <spring:url value="/files/solrFormat" var="solrLink"/>
            <spring:url value="/archive/delete" var="deleteLink"/>
            <spring:url value="/archive" var="archiveLink"/>
            <c:forEach var="version" items="${versions}">
                <li>
               		<span class="version" data-is-base-version="${version.baseVersion}">Version ${version.versionKey}</span>
                	<span>(<fmt:formatDate value="${version.date}" type="both" dateStyle="short" timeStyle="short"/>)</span>
                	<span class="infoBubble" title="${version.description}">i</span>
               	
                	<div class="options">
                		<div class="download dropdown">
	                		<button class="dropdown-btn btn btn-bright">Download</button>
	                		<ul>
	                			<li><a href="${uploadLink}/${institution.institutionId}/${version.versionKey}" class="option">Original Datei</a></li>
	                            <li><a href="${workingFormatLink}/${institution.institutionId}/${version.versionKey}" class="option">Arbeitsformat</a></li>
	                            <li><a href="${solrLink}/${institution.institutionId}/${version.versionKey}" class="option">Solr-Format</a></li>
	                		</ul>
	                	</div>
	                	<div class="re-index dropdown">
	                		<button class="dropdown-btn btn">Indizieren</button>
	                		<ul>
	                			<c:forEach var="solrCore" items="${solrCores}">
	                				<li><a href="${reindexLink}/${solrCore.name}/${institution.institutionId}/${version.versionKey}" class="option">nach ${solrCore.name}</a></li>
	                			</c:forEach>
	                		</ul>
	                	</div>
	                	<div class="re-process dropdown">
	                		<button class="dropdown-btn btn">Neu verarbeiten</button>
	                		<ul>
	                			<c:forEach var="solrCore" items="${solrCores}">
	                				<li><a href="${reprocessLink}/${solrCore.name}/${institution.institutionId}/${version.versionKey}" class="option">nach ${solrCore.name}</a></li>
	                			</c:forEach>
	                		</ul>
	                	</div>
                		<a href="${deleteLink}/${institution.institutionId}/${version.versionKey}" class="delete" title="delete"></a>
                	</div>
                	
                	<c:set var="indexStates" value="${version.indexStates}"/>
                	<c:if test="${not empty indexStates}">
                		<div class="indexStates">
                			<span>auf</span>
                			<c:forEach var="indexState" items="${indexStates}" varStatus="loop">
	                			<span>
	                				${indexState.coreName}
	                				<span class="infoBubble" title="${indexState.origin.description}">i</span>${!loop.last ? ',' : ''}
	                			</span>
	                		</c:forEach>
                		</div>
                	</c:if>
                </li>
            </c:forEach>
        </ul>
        <script>
	     	// Dropdown Menu
	        var dropdowns = Array.prototype.slice.call(document.querySelectorAll('.dropdown'),0);
	        dropdowns.forEach(function(dropdown) {
	        	var button = dropdown.querySelector('button');

	        	button.onclick = function(event) {
	        		dropdown.classList.toggle("open");
	        	};
	        });
	        // Close the dropdown menu if the user clicks outside of it
	        window.onclick = function(event) {
	          if (!event.target.matches('.dropdown-btn')) {
	            var dropdowns = Array.prototype.slice.call(document.querySelectorAll(".dropdown.open"),0);
	            dropdowns.forEach(function(dropdown) {
            		dropdown.classList.remove('open');
	            });
	          }
	        }
        </script>
    </div>
    <%@include file="footer.jspf" %>
</body>
</html>
