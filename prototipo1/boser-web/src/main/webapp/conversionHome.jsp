<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %> 
<html xmlns="http://www.w3.org/1999/xhtml">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
<title>TEST</title>
<meta name="author" content="Verny Quartara"/>
<script type="text/javascript" language="Javascript">
    function showCredits() {
        alert('Verny Quartara 2015');
    }
    function AutoRefresh( t ) {
    	setTimeout("location.reload(true);", t);
    }
</script>
</head>

<body onload="JavaScript:AutoRefresh(7000);">

<div>carica un foglio Excel, Boser creerà un PDF per ogni articolo</div>
<div>(la conversione può durare fino a due minuti per ogni articolo)</div>
<form action="/xlsToPdf" method="post" enctype="multipart/form-data">
	<input type="file" name="file" size="50" />
	<input type="hidden" name="crawlerId" value="1" />
	<input type="submit" value="Upload File" />
</form>
<hr/>
<div>
conversioni effettuate
<table border="1">
	<thead>
	<tr>
		<th>ID</th>
		<th>Nome</th>
		<th>Data creazione</th>
		<th>Stato</th>
		<th>N° pdf OK</th>
		<th>N° pdf KO</th>
		<th>Dimensione zip (KB)</th>
	</tr>
	</thead>
	<tbody>
<c:forEach items="${convertions}" var="conv">
	<tr>
		<td><c:out value="${conv.id}"/></td>
		<td>
			<a href='<c:url value="/conversionDownload">
					<c:param name="conversionId" value="${conv.id}"/>
				</c:url>
					'>
				<c:out value="${conv.label}"/>
			</a>
		</td>
		<td><fmt:formatDate value="${conv.startDate}" pattern="dd/MM/yyyy HH:mm"/></td>
		<td><c:out value="${conv.state}"/></td>
		<td><c:out value="${conv.countCompleted}"/></td>
		<td><c:out value="${conv.countFailed}"/></td>
		<td><fmt:formatNumber maxFractionDigits="0" value="${conv.fileSize/1024}" /></td>
	</tr>
</c:forEach>
	</tbody>
</table>
</div>

</body>
</html>
