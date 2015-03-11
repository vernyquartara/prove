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
ricerche effettuate
<c:forEach items="${searches}" var="search">
	<ul>
		<li>
			<a href="
				<c:url value="/searchDownload">
					<c:param name="searchId" value="${search.id}"/>
				</c:url>
					">
			<c:out value="${search.zipLabel}"/>
			</a>
			<%-- <a href="
				<c:url value="/searchDownload">
					<c:param name="srUrl" value="${result.url}"/>
					<c:param name="srKey" value="${result.key.id}"/>
					<c:param name="srSearch" value="${result.search.id}"/>
				</c:url>
					">
			<c:out value="${result.zipFilePath}"/>
			</a> --%>
		</li>
	</ul>
</c:forEach>
</div>

<hr />
<div>
<a href="<c:url value="/search?searchConfigId=1"/>">effettua nuova ricerca</a>&nbsp;(usa l'indice più recente)
</div>

<hr />
<a href="<c:url value="/"/>">pagina iniziale</a>
</body>
</html>
