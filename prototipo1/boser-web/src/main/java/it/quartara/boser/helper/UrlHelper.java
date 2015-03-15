package it.quartara.boser.helper;

import org.apache.commons.lang3.StringUtils;

public class UrlHelper {
	
	/**
	 * Restituisce la parte finale della url in input.
	 * @param url
	 * @return
	 */
	public static String getLastPart(String url) {
		String result = url;
		if (url.endsWith("/")) {
			result = StringUtils.removeEnd(url, "/");
		}
		return result.substring(result.lastIndexOf("/")+1);
	}

}
