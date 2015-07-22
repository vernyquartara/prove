<?xml version="1.0" encoding="UTF-8" ?>
<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %> 
<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>PDFConverter Manager</title>
<link rel="stylesheet" href="style/console.css"/>
</head>
<body>
<div id="barra">
	<div id="logo">boser</div>
</div>
<div id="legenda">
	<p>Il convertitore <strong>&egrave; ${status}</strong></p>
	<c:if test="${not running}">
		<p>Premi il pulsante qui sotto per avviarlo (l'avvio dura circa un minuto)</p>
		<p>Il convertitore sar&agrave; messo in standby automaticamente dopo 55 minuti di inattivit&agrave;</p>
		<form action="<c:url value="/pdfcmgr"/>" method="post">
			<input id="startBtn" type="submit" value="AVVIO" onclick="javascript:getElementById('startBtn').disabled=true;"/>
		</form>
	</c:if>
	<c:if test="${running}">
		<p><a href="http://boser.quartara.it/conversionHome">Accedi</a></p>
	</c:if>
</div>
</body>
</html>