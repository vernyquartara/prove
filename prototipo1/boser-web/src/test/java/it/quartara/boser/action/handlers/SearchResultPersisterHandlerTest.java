package it.quartara.boser.action.handlers;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import it.quartara.boser.model.SearchKey;
import it.quartara.boser.model.SearchResult;

import javax.persistence.EntityManager;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

public class SearchResultPersisterHandlerTest {

	private static final String URL = "url";
	private static final String TITLE = "title";
	private static final String CONTENT = "content";
	
	@Test
	public void testHandle2Documents() {
		EntityManager em = mock(EntityManager.class);
		SearchKey key = new SearchKey();
		key.setText("key1");
		
		String url1 = "http://....1";
		String title1 = "result1";
		String content1 = "content1";
		SolrDocument doc1 = new SolrDocument();
		doc1.setField(URL, url1);
		doc1.setField(TITLE, title1);
		doc1.setField(CONTENT, content1);
		
		String url2 = "http://....2";
		String title2 = "result2";
		String content2 = "content2";
		SolrDocument doc2 = new SolrDocument();
		doc2.setField(URL, url2);
		doc2.setField(TITLE, title2);
		doc2.setField(CONTENT, content2);
		
		SolrDocumentList docs = new SolrDocumentList();
		docs.add(doc1);
		docs.add(doc2);
	
		SearchResultPersisterHandler handler = new SearchResultPersisterHandler(em);
		handler.handle(null, key, docs);
		
		ArgumentCaptor<SearchResult> argument = ArgumentCaptor.forClass(SearchResult.class);
		verify(em, times(2)).persist(argument.capture());
		
		assertThat(argument.getAllValues().get(0).getUrl(), equalTo(url1));
		assertThat(argument.getAllValues().get(0).getTitle(), equalTo(title1));
		assertThat(argument.getAllValues().get(0).getContent(), equalTo(content1));
		assertThat(argument.getAllValues().get(0).getKey(), equalTo(key));
		
		assertThat(argument.getAllValues().get(1).getUrl(), equalTo(url2));
		assertThat(argument.getAllValues().get(1).getTitle(), equalTo(title2));
		assertThat(argument.getAllValues().get(1).getContent(), equalTo(content2));
		assertThat(argument.getAllValues().get(1).getKey(), equalTo(key));
	}
}
