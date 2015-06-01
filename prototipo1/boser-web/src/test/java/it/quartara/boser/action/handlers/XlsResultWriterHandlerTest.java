package it.quartara.boser.action.handlers;

import static it.quartara.boser.model.IndexField.CONTENT;
import static it.quartara.boser.model.IndexField.TITLE;
import static it.quartara.boser.model.IndexField.URL;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.persistence.EntityManager;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({XlsResultWriterHandler.class})
@MockPolicy(Slf4jMockPolicy.class)
public class XlsResultWriterHandlerTest {

	@Test @Ignore
	public void testHappyPath() throws Exception {
		EntityManager em = mock(EntityManager.class);
		String repo = "target/test-output/searchRepo";
		Parameter param = new Parameter();
		param.setValue(repo);
		SearchConfig config = new SearchConfig();
		config.setId(11L);
		SearchKey key = new SearchKey();
		key.setText("key");
		Search search = new Search();
		search.setConfig(config);
		search.setId(19L);
		Date date = new GregorianCalendar(2015, 02, 10, 22, 41).getTime();
		whenNew(Date.class).withNoArguments().thenReturn(date);
		
		SolrDocumentList docs = createSolrDocumentList();

		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		
		XlsResultWriterHandler handler = spy(new XlsResultWriterHandler(em));
		
		handler.handle(search, key, docs);
		
		File expected = new File(repo+"/11/19/2015-03-10-22-41.xls");
		assertTrue(expected.exists());
		assertThat(expected.length(), is(greaterThan(0L)));
		
		InputStream input = new FileInputStream(expected);
		Workbook wb = WorkbookFactory.create(input);
		assertThat(wb.getNumberOfSheets(), equalTo(1));
		Sheet sheet = wb.getSheetAt(0);
		assertThat(sheet.getRow(1), is(not(nullValue())));
		assertThat(sheet.getRow(2), is(not(nullValue())));
		assertThat(sheet.getRow(3), is((nullValue())));
		Row header = sheet.getRow(0);
		assertThat(header, is(not(nullValue())));
//		assertThat(header.getFirstCellNum(), equalTo((short)0));
//		assertThat(header.getLastCellNum(), equalTo((short)7));
		assertThat(header.getCell(0).getStringCellValue(), equalTo("Testata"));
		assertThat(header.getCell(1).getStringCellValue(), equalTo("Tipo"));
		assertThat(header.getCell(2).getStringCellValue(), equalTo("Data"));
		assertThat(header.getCell(3).getStringCellValue(), equalTo("Titolo"));
		assertThat(header.getCell(4).getStringCellValue(), equalTo("Argomento"));
		assertThat(header.getCell(5).getStringCellValue(), equalTo("Modello"));
		assertThat(header.getCell(6).getStringCellValue(), equalTo("Autore"));
		assertThat(header.getCell(7).getStringCellValue(), equalTo("Foto col"));
		Row firstRow = sheet.getRow(1);
		assertThat(firstRow.getCell(0).getStringCellValue(), equalTo("omniauto.it"));
		assertThat(firstRow.getCell(0).getHyperlink().getAddress(), equalTo("http://www.omniauto.it/magazine/28961/school-snow-omniauto-2015-posizione-guida"));
		assertThat(firstRow.getCell(1).getStringCellValue(), equalTo(""));
		assertThat(firstRow.getCell(2).getStringCellValue(), equalTo(""));
		assertThat(firstRow.getCell(3).getStringCellValue(), equalTo("Guida sulla neve | La posizione di guida - OmniAuto.it"));
		assertThat(firstRow.getCell(4).getStringCellValue(), equalTo(""));
		assertThat(firstRow.getCell(5).getStringCellValue(), equalTo(""));
		assertThat(firstRow.getCell(6).getStringCellValue(), equalTo(""));
		assertThat(firstRow.getCell(7).getStringCellValue(), equalTo(""));
		Row secondRow = sheet.getRow(2);
		assertThat(secondRow.getCell(0).getStringCellValue(), equalTo("motormag.it"));
		assertThat(secondRow.getCell(0).getHyperlink().getAddress(), equalTo("http://auto.motormag.it/news/6982/"));
		assertThat(secondRow.getCell(1).getStringCellValue(), equalTo(""));
		assertThat(secondRow.getCell(2).getStringCellValue(), equalTo(""));
		assertThat(secondRow.getCell(3).getStringCellValue(), equalTo("Nuova Audi R8, il mito continua"));
		assertThat(secondRow.getCell(4).getStringCellValue(), equalTo(""));
		assertThat(secondRow.getCell(5).getStringCellValue(), equalTo(""));
		assertThat(secondRow.getCell(6).getStringCellValue(), equalTo(""));
		assertThat(secondRow.getCell(7).getStringCellValue(), equalTo(""));
	}
	
	private SolrDocumentList createSolrDocumentList() {
		String url1 = "http://www.omniauto.it/magazine/28961/school-snow-omniauto-2015-posizione-guida";
		String title1 = "Guida sulla neve | La posizione di guida - OmniAuto.it";
		String content1 = "content1";
		SolrDocument doc1 = new SolrDocument();
		doc1.setField(URL.toString(), url1);
		doc1.setField(TITLE.toString(), title1);
		doc1.setField(CONTENT.toString(), content1);
		
		String url2 = "http://auto.motormag.it/news/6982/";
		String title2 = "Nuova Audi R8, il mito continua";
		String content2 = "content2";
		SolrDocument doc2 = new SolrDocument();
		doc2.setField(URL.toString(), url2);
		doc2.setField(TITLE.toString(), title2);
		doc2.setField(CONTENT.toString(), content2);
		
		SolrDocumentList docs = new SolrDocumentList();
		docs.add(doc1);
		docs.add(doc2);
		
		return docs;
	}
	
	public void testGetLabel() throws Exception {
		XlsResultWriterHandler handler = new XlsResultWriterHandler(null);
		assertEquals("ultimissimeauto.com", (String)Whitebox.invokeMethod(handler, "getLinkLabel", "http://www.ultimissimeauto.com/"));
		assertEquals("motomag.it", (String)Whitebox.invokeMethod(handler, "getLinkLabel", "http://auto.motomag.it/"));
		assertEquals("autovideoblog.it", (String)Whitebox.invokeMethod(handler, "getLinkLabel", "http://www.autovideoblog.it/audi/"));
		assertEquals("autovideoblog.co.uk", (String)Whitebox.invokeMethod(handler, "getLinkLabel", "http://it.autovideoblog.co.uk/weekly/"));
	}

}
