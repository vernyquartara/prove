package it.quartara.boser.action.handlers;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import java.io.File;

import javax.persistence.EntityManager;

import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(TxtResultWriterHandler.class)
public class TxtResultWriterHandlerTest {
	
	@Test
	public void test() throws Exception {
		EntityManager em = mock(EntityManager.class);
		String repo = "target/test-output/searchRepo";
		Parameter param = new Parameter();
		param.setValue(repo);
		SearchConfig config = new SearchConfig();
		config.setId(99L);
		Search search = new Search();
		search.setConfig(config);
		search.setId(101L);
		
		SearchKey key = new SearchKey();
		key.setText("key1");
		
		SolrDocumentList docs = ActionHandlerTestHelper.createSolrDocumentList();
		
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		
		TxtResultWriterHandler handler = new TxtResultWriterHandler(em);
		handler.handle(search, key, docs);
		
		File generated = new File(repo+File.separator+config.getId()+File.separator+search.getId()+File.separator+key.getText()+".txt");
		assertTrue(generated.exists());
		File expected = new File("src/test/resources/TxtResults.txt");
		assertTrue(FileUtils.contentEquals(generated, expected));
	}

}
