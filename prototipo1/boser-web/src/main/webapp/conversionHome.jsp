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
		<p class="istruzioni">carica un foglio Excel per avviare la conversione in pdf</p>
		<form action="/xlsToPdf" method="post" enctype="multipart/form-data">
			<input type="file" name="file" size="50" />
			<input type="hidden" name="crawlerId" value="1" />
			<select name="scale" title="seleziona il fattore di ingrandimento">
				<option value="1.0f">100%</option>
				<option value="1.25f">125%</option>
				<option value="1.5f">150%</option>
				<option value="1.75f">175%</option>
				<option value="2.0f">200%</option>
			</select>
			<input type="submit" value="Upload File" />
		</form>
	</div>
	<div id="logo" onclick="showCredits();">boser</div>
</div>
<div id="tabella">
	<p>Conversioni Effettuate  (ricarica la pagina (F5) per aggiornare i dati)</p>
	<table>
		<thead>
			<tr>
				<th>ID</th>
				<th>Nome</th>
				<th>Avviato il</th>
				<th>Stato</th>
				<th>tot. articoli</th>
				<th>in lavorazione</th>
				<th>ult. agg.</th>
				<th>pdf OK</th>
				<th>pdf KO</th>
				<th>KB zip</th>
			</tr>
		</thead>
		<tbody>
		<c:forEach items="${convertions}" var="conv">
			<tr>
				<td class="conv_id"><c:out value="${conv.id}"/></td>
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
				<td><c:out value="${conv.countTotal}"/></td>
				<td><c:out value="${conv.countRemaining}"/></td>
				<td><fmt:formatDate value="${conv.lastUpdate}" pattern="HH:mm:ss"/></td>
				<td><c:out value="${conv.countCompleted}"/></td>
				<td><c:out value="${conv.countFailed}"/></td>
				<td><fmt:formatNumber maxFractionDigits="0" value="${conv.fileSize/1024}" /></td>
			</tr>
		</c:forEach>
		</tbody>
	</table>
</div>

<div id="legenda">
	<ol>
		<li>ID: codice identificativo</li>
		<li>Nome: nome del file</li>
		<li>Avviato il: data di inserimento della richiesta di lavorazione</li>
		<li>Stato: lo stato della lavorazione</li>
		<li>tot. articoli: numero totale di articoli trovati nel foglio Excel di input</li>
		<li>in lavorazione: numero di articoli attualmente in elaborazione</li>
		<li>ult. agg.: orario ultimo articolo elaborato</li>
		<li>pdf OK: numero di pdf creati correttamente</li>
		<li>pdf KO: numero di pdf non creati a causa di errori</li>
		<li>KB zip: dimensione in kilobyte del file zip</li>
	</ol>
	<p>NB: il numero dei pdf OK e KO viene calcolato solo al termine della lavorazione</p>
</div>

</body>
</html>
