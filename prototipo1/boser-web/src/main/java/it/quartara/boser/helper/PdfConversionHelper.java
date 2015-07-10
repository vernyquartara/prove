package it.quartara.boser.helper;

import it.quartara.boser.model.ExecutionState;
import it.quartara.boser.model.PdfConversion;
import it.quartara.boser.model.PdfConversionItem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Hyperlink;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfConversionHelper {
	
	private static final Logger log = LoggerFactory.getLogger(PdfConversionHelper.class);

	/**
	 * Crea una copia del foglio xls che ha originato la conversione, colorando
	 * in rosso le caselle relative agli articoli che non è stato possibile
	 * convertire in pdf a causa di errori.
	 * 
	 * @param currentConversion
	 * @throws IOException
	 * @throws InvalidFormatException
	 */
	public static void createXlsReport(PdfConversion currentConversion) throws IOException, InvalidFormatException {
		Map<String, ExecutionState> urlStateMap = new HashMap<String, ExecutionState>();
		for (PdfConversionItem item : currentConversion.getItems()) {
			urlStateMap.put(item.getUrl(), item.getState());
		}
		String originalXlsFileName = currentConversion.getXlsFileName();
		String originalXlsFilePath = new File(currentConversion.getDestDir()).getParent();
		File originalXlsFile = new File(originalXlsFilePath+File.separator+originalXlsFileName);
		FileUtils.copyFileToDirectory(originalXlsFile, new File(currentConversion.getDestDir()));
		File xlsReport = new File(currentConversion.getDestDir(), originalXlsFileName);
		FileInputStream in = new FileInputStream(xlsReport);
		Workbook wb = WorkbookFactory.create(in);
		Sheet sheet = wb.getSheetAt(0);
		Font defaultFont = wb.createFont();
	    defaultFont.setFontHeightInPoints((short)8);
	    defaultFont.setFontName("Arial");
		CellStyle cellStyle = wb.createCellStyle();
		cellStyle.setFont(defaultFont);
		cellStyle.setFillForegroundColor(HSSFColor.RED.index);
		cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
		cellStyle.setBorderBottom(CellStyle.BORDER_THIN);
		cellStyle.setBorderLeft(CellStyle.BORDER_THIN);
		cellStyle.setBorderTop(CellStyle.BORDER_THIN);
		cellStyle.setBorderRight(CellStyle.BORDER_THIN);
		cellStyle.setAlignment(CellStyle.ALIGN_CENTER);
		cellStyle.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
		for (Row row : sheet) {
			if (row.getPhysicalNumberOfCells()>0) {
				Cell cell = row.getCell(0);
				if (cell!=null) {
					Hyperlink link = cell.getHyperlink();
					if (link != null && urlStateMap.get(link.getAddress())==ExecutionState.ERROR) {
						cell.setCellStyle(cellStyle);
					}
				}
			}
		}
		in.close();
		FileOutputStream out = new FileOutputStream(xlsReport);
		wb.write(out);
		out.close();
		wb.close();
	}
	
	/**
	 * Crea un file zip del contenuto della cartella specificata,
	 * prendendo in considerazione esclusivamente le estensioni xls e pdf.
	 * @param dirToZip
	 * @return
	 * @throws IOException
	 */
	public static File createZipFile(String dirToZip) throws IOException {
		String zipFileName = dirToZip.substring(0, dirToZip.lastIndexOf("/"))
				+ "/" + dirToZip.substring(dirToZip.lastIndexOf("/") + 1)
				+ ".zip";
		File zipFile = new File(zipFileName);
		log.debug("avvio creazione file zip: {}", zipFile.getAbsolutePath());

		FileOutputStream fos = new FileOutputStream(zipFile);
		ZipOutputStream zos = new ZipOutputStream(fos);

		byte[] buffer = new byte[65536];
		File[] files = new File(dirToZip).listFiles(new FilenameFilter() {

			@Override
			public boolean accept(File dir, String name) {
				return name.toLowerCase().endsWith(".pdf")
						|| name.toLowerCase().endsWith(".xls");
			}
		});
		for (File file : files) {
			log.debug("aggiunta file: {}", file.getName());
			ZipEntry ze = new ZipEntry(file.getName());
			zos.putNextEntry(ze);
			FileInputStream inputFile = new FileInputStream(file);
			int len;
			while ((len = inputFile.read(buffer)) > 0) {
				zos.write(buffer, 0, len);
			}
			inputFile.close();
			zos.closeEntry();
		}
		zos.close();
		log.debug("file zip creato");
		return zipFile;
	}
}
