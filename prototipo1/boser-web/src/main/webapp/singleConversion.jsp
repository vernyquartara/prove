<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %> 
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>TEST</title>
<link rel="stylesheet" href="css/boser.css"/>
<meta name="author" content="Verny Quartara"/>
<script type="text/javascript" language="Javascript">
    function showCredits() {
        alert('Verny Quartara 2015');
    }
</script>
</head>

<body>

<div id="barra">
	<div id="pulsanti">
		<p class="istruzioni">inserisci il link all'articolo da convertire in pdf</p>
		<form action="/urlToPdf" method="post" enctype="application/x-www-form-urlencoded">
			<input type="text" name="url" size="100" />
			<input type="hidden" name="crawlerId" value="1" />
			<input type="submit" value="Start" />
		</form>
	</div>
	<div id="logo" onclick="showCredits();">boser</div>
</div>
<div id="nav">
	<a href="<c:url value="/conversionHome"/>">Converti più articoli a partire da un foglio xls</a>
</div>
<c:if test="${errorMsg!=null}">
	<div id="error"><p><c:out value="${errorMsg}"/></p></div>
</c:if>

</body>
</html>
