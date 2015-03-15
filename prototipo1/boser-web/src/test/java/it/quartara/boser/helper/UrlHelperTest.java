package it.quartara.boser.helper;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class UrlHelperTest {

	@Test
	public void testGetLastPart() {
		assertThat(UrlHelper.getLastPart("http://www.ultimissimeauto.com/"), 
				   equalTo("www.ultimissimeauto.com"));
		
		assertThat(UrlHelper.getLastPart("http://www.omniauto.it/magazine/28341/2012-2014-mahindra-prodotte"), 
				   equalTo("2012-2014-mahindra-prodotte"));
		
		assertThat(UrlHelper.getLastPart("http://www.omniauto.it/magazine/28341/articolo/"), 
				   equalTo("articolo"));
		
		assertThat(UrlHelper.getLastPart("http://www.omniauto.it/magazine/"), 
				equalTo("magazine"));
		
		assertThat(UrlHelper.getLastPart("http://www.omniauto.it/magazine"), 
				equalTo("magazine"));
	}
}
