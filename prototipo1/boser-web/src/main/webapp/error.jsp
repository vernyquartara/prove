<%@ page isErrorPage="true" language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<html>
<head>
<title>Boser | Errore!</title>
</head>
<body>
	<h1>Errore!</h1>
	<h2>copia il testo seguente e spediscilo a webny23@gmail.com</h2>
	<table width="100%" border="1">
		<tr valign="top">
			<td width="40%"><b>Error:</b></td>
			<td>${pageContext.exception}</td>
		</tr>
		<tr valign="top">
			<td><b>URI:</b></td>
			<td>${pageContext.errorData.requestURI}</td>
		</tr>
		<tr valign="top">
			<td><b>Status code:</b></td>
			<td>${pageContext.errorData.statusCode}</td>
		</tr>
		<tr valign="top">
			<td><b>Stack trace:</b></td>
			<td>
				<c:forEach var="trace" items="${pageContext.exception.stackTrace}">
					<p>${trace}</p>
				</c:forEach>
			</td>
		</tr>
	</table>
</body>
</html>