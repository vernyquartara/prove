package it.quartara.boser.servlet;

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doNothing;
import static org.powermock.api.mockito.PowerMockito.doReturn;
import static org.powermock.api.mockito.PowerMockito.spy;
import static org.powermock.api.mockito.PowerMockito.verifyPrivate;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;
import it.quartara.boser.action.handlers.ActionHandler;
import it.quartara.boser.action.handlers.PdfResultWriterHandler;
import it.quartara.boser.action.handlers.SearchResultPersisterHandler;
import it.quartara.boser.action.handlers.TxtResultWriterHandler;
import it.quartara.boser.model.Parameter;
import it.quartara.boser.model.Search;
import it.quartara.boser.model.SearchAction;
import it.quartara.boser.model.SearchConfig;
import it.quartara.boser.model.SearchKey;

import java.io.File;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityTransaction;
import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.client.solrj.impl.HttpSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.SolrParams;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.mockpolicies.Slf4jMockPolicy;
import org.powermock.core.classloader.annotations.MockPolicy;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

@RunWith(PowerMockRunner.class)
@PrepareForTest({SearchServlet.class, HttpSolrServer.class})
@MockPolicy(Slf4jMockPolicy.class)
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
		String repo = "target/test-output/searchRepo";
		Parameter param = new Parameter();
		param.setValue(repo);
		EntityTransaction transaction = mock(EntityTransaction.class);
		
		SearchServlet servlet = spy(new SearchServlet());
		
		when(request.getParameter("searchConfigId")).thenReturn(searchConfigId.toString());
		doReturn(em).when(servlet).getEntityManager();
		when(em.getTransaction()).thenReturn(transaction).thenReturn(transaction); //2 times
		doNothing().when(transaction).begin();
		when(em.find(Parameter.class, "SOLR_URL")).thenReturn(param);
		when(em.find(SearchConfig.class, searchConfigId)).thenReturn(searchConfig);
		doReturn(handler).when(servlet, "createHandlerChain", searchConfig.getActions(), em);
		whenNew(HttpSolrServer.class).withAnyArguments().thenReturn(solr); 
		when(solr.query(any(SolrParams.class))).thenReturn(solrQuery);
		when(solrQuery.getResults()).thenReturn(solrResults);
		when(request.getRequestDispatcher("/searchHome.jsp")).thenReturn(rd);
		when(em.find(Parameter.class, "SEARCH_REPO")).thenReturn(param);
		doReturn(new File("target/test-output/file.zip")).when(servlet, "createZipFile", any(String.class), any(Date.class));
		doNothing().when(transaction).commit();
		
		servlet.doGet(request, response);
		
		verify(handler).handle(any(Search.class), any(SearchKey.class), eq(solrResults));
		verifyPrivate(servlet).invoke("createZipFile", any(String.class), any(Date.class));
		verify(em).merge(any(Search.class));
		verify(em).close();
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
		action.setHandlerClass("a.b.c.Handler");
		Set<SearchAction> actions = new HashSet<SearchAction>();
		actions.add(action);
		config.setActions(actions);
		return config;
	}
	
	@Test
	public void testCreateHandlerChain() throws Exception {
		EntityManager em = mock(EntityManager.class);
		SearchAction action1 = new SearchAction();
		action1.setId(1L);
		action1.setHandlerClass("it.quartara.boser.action.handlers.PdfResultWriterHandler");
		SearchAction action2 = new SearchAction();
		action2.setId(2L);
		action2.setHandlerClass("it.quartara.boser.action.handlers.TxtResultWriterHandler");
		Set<SearchAction> searchActions = new LinkedHashSet<SearchAction>();
		searchActions.add(action1);
		searchActions.add(action2);
		
		SearchServlet servlet = new SearchServlet();
		
		ActionHandler handler = Whitebox.invokeMethod(servlet, "createHandlerChain", searchActions, em);
		assertThat(handler, instanceOf(PdfResultWriterHandler.class));
		ActionHandler nextHandler = Whitebox.getInternalState(handler, "nextHandler");
		assertThat(nextHandler, instanceOf(TxtResultWriterHandler.class));
		ActionHandler lastHandler = Whitebox.getInternalState(nextHandler, "nextHandler");
		assertThat(lastHandler, instanceOf(SearchResultPersisterHandler.class));
	}
	
}
