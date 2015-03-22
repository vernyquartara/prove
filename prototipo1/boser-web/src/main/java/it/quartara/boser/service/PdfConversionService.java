package it.quartara.boser.service;

import java.io.File;

public interface PdfConversionService {

	/**
	 * 
	 * @param destDir
	 * @param url
	 * @return
	 */
	File convertToPdf(String destDir, String url);
	/**
	 * 
	 * @param destDir
	 * @param url
	 * @param fileNamePrefix
	 * @return
	 */
	File convertToPdf(String destDir, String url, String fileNamePrefix);
}
