package it.quartara.boser.action.handlers;

import static it.quartara.boser.model.IndexField.TITLE;
import static it.quartara.boser.model.IndexField.URL;
import static org.apache.poi.ss.usermodel.Cell.*;
import it.quartara.boser.action.ActionException;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchKey;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;

import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

/**
 * Scrive i risultati di ricerca in formato Excel.
 * Dato un elenco di documenti Solr, per ognuno (che rappresenta un link)
 * scrive una riga nel foglio Excel.
 * Le colonne che devono essere valorizzare sono "Testata" (pari al nome del dominio
 * punto estensione, senza terzo livello) e "Titolo" (pari al titolo del documento)
 * Il file viene prodotto nella cartella principale della ricerca
 * effettuata.
 * @author webny
 *
 */
public class XlsResultWriterHandler extends AbstractActionHandler {
	
	static Pattern urlPattern = Pattern.compile("http://[\\w|\\d]+\\.([\\w|d]+\\.[\\w|\\d]+)/.+$");

	public XlsResultWriterHandler(EntityManager em) {
		super(em);
	}

	@Override
	protected void execute(Search search, SearchKey key, SolrDocumentList documents) throws ActionException {
		System.setProperty("java.awt.headless", "true");
		Parameter param = em.find(Parameter.class, "SEARCH_REPO");
		String repo = param.getValue();
		File repoDir = new File(repo+File.separator+search.getConfig().getId()+File.separator+search.getId());
		repoDir.mkdirs();
		
		Date now = new Date();
		DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm");
		File outputFile = new File(repoDir.getAbsolutePath()+File.separator+format.format(now)+".xls");
		FileOutputStream fileOut = null;
	    try {
			fileOut = new FileOutputStream(outputFile);
		} catch (FileNotFoundException e) {
			throw new ActionException("unable to open file: "+outputFile.getAbsolutePath());
		}
	    
	    Workbook wb = new HSSFWorkbook();
	    CreationHelper createHelper = wb.getCreationHelper();
	    Font headerFont = wb.createFont();
	    headerFont.setFontHeightInPoints((short)8);
	    headerFont.setFontName("Verdana");
	    headerFont.setBold(Boolean.TRUE);
	    CellStyle headerStyle = wb.createCellStyle();
	    headerStyle.setFont(headerFont);
	    headerStyle.setFillForegroundColor(HSSFColor.YELLOW.index);
	    headerStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
	    headerStyle.setAlignment(CellStyle.ALIGN_CENTER);
	    headerStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
	    headerStyle.setBorderBottom(CellStyle.BORDER_THIN);
	    headerStyle.setBorderLeft(CellStyle.BORDER_THIN);
	    headerStyle.setBorderTop(CellStyle.BORDER_THIN);
	    headerStyle.setBorderRight(CellStyle.BORDER_THIN);
	    
	    Font defaultFont = wb.createFont();
	    defaultFont.setFontHeightInPoints((short)8);
	    defaultFont.setFontName("Arial");
	    CellStyle defaultCellStyle = wb.createCellStyle();
	    defaultCellStyle.setBorderBottom(CellStyle.BORDER_THIN);
	    defaultCellStyle.setBorderLeft(CellStyle.BORDER_THIN);
	    defaultCellStyle.setBorderTop(CellStyle.BORDER_THIN);
	    defaultCellStyle.setBorderRight(CellStyle.BORDER_THIN);
	    defaultCellStyle.setAlignment(CellStyle.ALIGN_CENTER);
	    defaultCellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
	    defaultCellStyle.setFont(defaultFont);
	    
	    Sheet sheet = wb.createSheet("Foglio1");
	    createHeader(sheet, headerStyle);
	    int rowCounter = 1;
		for (SolrDocument doc : documents) {
			Row row = sheet.createRow(rowCounter++);
			row.setHeightInPoints(30);
			for (int i = 0; i < 8; i++) {
		    	Cell cell = row.createCell(i, CELL_TYPE_STRING);
		    	cell.setCellStyle(defaultCellStyle);
		    	cell.setCellValue("");
		    }
			Hyperlink link = createHelper.createHyperlink(Hyperlink.LINK_URL);
			String url = (String)doc.getFieldValue(URL.toString());
			link.setAddress(url);
			Font linkFont = wb.createFont();
		    linkFont.setUnderline(Font.U_SINGLE);
		    linkFont.setColor(IndexedColors.BLUE.getIndex());
		    CellStyle linkStyle = wb.createCellStyle();
		    linkStyle.setFont(linkFont);
		    Cell cell0 = row.getCell(0);
		    cell0.setHyperlink(link);
		    cell0.setCellValue(getLinkLabel(url));
		    
		    Cell cell3 = row.getCell(3);
		    cell3.setCellValue((String)doc.getFieldValue(TITLE.toString()));
		}
		sheet.autoSizeColumn(0);
		sheet.autoSizeColumn(1);
		sheet.autoSizeColumn(2);
		sheet.autoSizeColumn(3);
		sheet.autoSizeColumn(4);
		sheet.autoSizeColumn(5);
		sheet.autoSizeColumn(6);
		sheet.autoSizeColumn(7);
		try {
			wb.write(fileOut);
			fileOut.close();
			wb.close();
		} catch (IOException e) {
			throw new ActionException("unable to write to file: "+outputFile.getAbsolutePath());
		}
	}

	private String getLinkLabel(String url) throws ActionException {
		Matcher m = urlPattern.matcher(url);
		if (m.matches()) {
			return m.group(1);
		}
		throw new ActionException("unable to match link url in string: " + url);
	}

	private void createHeader(Sheet sheet, CellStyle style) {
		Row row = sheet.createRow(0);
		row.setHeightInPoints(25);
		String[] headers = {"Testata", "Tipo", "Data", "Titolo", "Argomento", "Modello", "Autore", "Foto col" };
		for (int i = 0; i < headers.length; i++) {
			Cell cell = row.createCell(i);
			cell.setCellStyle(style);
			cell.setCellValue(headers[i]);
		}
	}

}
