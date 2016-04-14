<!DOCTYPE html>
<html>
<%@include file="head.jspf" %>
<body onload='document.f.j_username.focus();'>
<header>
    <div id="slogan">
        Daten-Upload<br />Metadaten-Katalog
    </div>
    <div id="logo"></div>
    <div style="height: 105px"></div>
</header>
<div class="main">
    <c:if test="${'fail' eq param.auth}">
		<div class="error">Accountname oder Passwort ungültig!</div><br />
	</c:if>
	<c:if test="${'success' eq param.logout}">
		<div class="msg">Sie haben sich erfolgreich abgemeldet.</div><br />
	</c:if>
	<h1>Anmelden</h1>
    <form name='f' action='j_spring_security_check' method='POST'>
        <input type='text' name='j_username' value='' placeholder="Accountname" style="width: 300px"  /><br />
        <br />
        <input type='password' name='j_password' placeholder="Passwort" style="width: 300px" /><br />
        <br />
        <br />
        <input name="submit" type="submit" value="Login" class="btn"/>
    </form>
</div>
</body>
</html>