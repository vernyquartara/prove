package it.quartara.boser.action.handlers;

import static it.quartara.boser.model.IndexField.CONTENT;
import static it.quartara.boser.model.IndexField.TITLE;
import static it.quartara.boser.model.IndexField.URL;

import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;

public class ActionHandlerTestHelper {
	
	public static SolrDocumentList createSolrDocumentList() {
		String url1 = "http://....1";
		String title1 = "title1";
		String content1 = "content1";
		SolrDocument doc1 = new SolrDocument();
		doc1.setField(URL.toString(), url1);
		doc1.setField(TITLE.toString(), title1);
		doc1.setField(CONTENT.toString(), content1);
		
		String url2 = "http://....2";
		String title2 = "title2";
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

}
