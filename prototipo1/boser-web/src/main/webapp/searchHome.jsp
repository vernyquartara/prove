<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>TEST</title>
<meta name="author" content="Verny Quartara"/>
<script type="text/javascript" language="Javascript">
    function showCredits() {
        alert('Verny Quartara 2015');
    }
</script>
</head>

<body>


<div>
ricerche effettuate (ricerca.txt)
<c:forEach items="${searchResults}" var="result">
	<ul>
		<li>
			<a href="<c:url value="/searchDownload?searchResultId=${result.id}" />"><c:out value="${result.zipFilePath}"/></a>
		</li>
	</ul>
</c:forEach>
</div>

<hr />
<%--
<div>
<html:link action="search">effettua nuova ricerca </html:link>(usa l'indice più recente)
</div>

<hr />
<html:link href="/">pagina iniziale</html:link>
--%>
</body>
</html>
