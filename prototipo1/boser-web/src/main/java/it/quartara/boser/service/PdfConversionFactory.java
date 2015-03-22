package it.quartara.boser.service;

import it.quartara.boser.service.impl.PdfConversionServiceImpl;

public class PdfConversionFactory {
	
	/**
	 * 
	 * @return
	 */
	public static PdfConversionService create() {
		return new PdfConversionServiceImpl();
	}

}
