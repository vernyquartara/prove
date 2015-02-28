package it.quartara.boser.servlet;

import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

import java.util.HashSet;
import java.util.Set;

import it.quartara.boser.action.handlers.ActionHandler;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.SearchAction;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;
import it.quartara.boser.model.SearchResult;

import javax.persistence.EntityManager;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchServlet.class, HttpSolrServer.class})
public class SearchServletTest {

	@Test
	public void testDoGet() throws Exception {
		HttpServletRequest request = mock(HttpServletRequest.class);
		HttpServletResponse response = mock(HttpServletResponse.class);
		RequestDispatcher rd = mock(RequestDispatcher.class);
		EntityManager em = mock(EntityManager.class);
		Long searchConfigId = 10L;
		SearchConfig searchConfig = createSearchConfig(searchConfigId);
		HttpSolrServer solr = mock(HttpSolrServer.class);
		QueryResponse solrQuery = mock(QueryResponse.class);
		SolrDocumentList solrResults = mock(SolrDocumentList.class);
		ActionHandler handler = mock(ActionHandler.class);
		Parameter param = new Parameter();
		
		SearchServlet servlet = spy(new SearchServlet());
		
		when(request.getParameter("searchConfigId")).thenReturn(searchConfigId.toString());
		doReturn(em).when(servlet).getEntityManager();
		when(em.find(Parameter.class, "SOLR_URL")).thenReturn(param);
		when(em.find(SearchConfig.class, searchConfigId)).thenReturn(searchConfig);
		when(servlet, "createHandlerChain", searchConfig.getActions(), em).thenReturn(handler);
		whenNew(HttpSolrServer.class).withAnyArguments().thenReturn(solr); 
		when(solr.query(any(SolrParams.class))).thenReturn(solrQuery);
		when(solrQuery.getResults()).thenReturn(solrResults);
		when(request.getRequestDispatcher("/searchHome.jsp")).thenReturn(rd);
		
		servlet.doGet(request, response);
		
		verify(handler).handle(eq(searchConfig), any(SearchKey.class), eq(solrResults));
		verify(rd).forward(request, response);
	}

	private SearchConfig createSearchConfig(Long searchConfigId) {
		SearchConfig config = new SearchConfig();
		config.setId(searchConfigId);
		SearchKey key = new SearchKey();
		key.setText("key1");
		Set<SearchKey> keys = new HashSet<SearchKey>();
		keys.add(key);
		config.setKeys(keys);
		SearchAction action = new SearchAction();
		action.setImpl("a.b.c.Action");
		Set<SearchAction> actions = new HashSet<SearchAction>();
		actions.add(action);
		config.setActions(actions);
		return config;
	}
	
	@Test
	public void testCreateHandlerChain() {
		fail();
	}
	
}
