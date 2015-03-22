package it.quartara.boser.service.impl;

import it.quartara.boser.helper.UrlHelper;
import it.quartara.boser.service.PdfConversionService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.Serializable;

import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.fop.svg.PDFTranscoder;
import org.fit.cssbox.demo.ImageRenderer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PdfConversionServiceImpl implements PdfConversionService, Serializable {
	
	private static final long serialVersionUID = -6957321264439905450L;
	private static final Logger log = LoggerFactory.getLogger(PdfConversionServiceImpl.class);

	@Override
	public File convertToPdf(String destDir, String url) {
		return convertToPdf(destDir, url, null);
	}

	@Override
	public File convertToPdf(String destDir, String url, String fileNamePrefix) {
		String fileName = (fileNamePrefix != null ? fileNamePrefix+"_" : "")
						  +UrlHelper.getLastPart(url)+".pdf";
		File destFile = new File(destDir, fileName);
		try {
			ImageRenderer imageRenderer = new ImageRenderer();
			Transcoder transcoder = new PDFTranscoder();
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			imageRenderer.renderURL(url, outputStream, ImageRenderer.Type.SVG);
			byte[] svgBytes = outputStream.toByteArray();
			ByteArrayInputStream inputStream = new ByteArrayInputStream(svgBytes);
			TranscoderInput transcoderInput = new TranscoderInput(inputStream);
	        TranscoderOutput transcoderOutput = new TranscoderOutput(new FileOutputStream(destFile));
			transcoder.transcode(transcoderInput, transcoderOutput);
		} catch (Exception e) {
			log.error("problema di conversione in pdf per l'url: "+url, e);
			return null;
		}
		return destFile;
	}

}
