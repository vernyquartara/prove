package it.quartara.boser.action.handlers;

import static org.powermock.api.mockito.PowerMockito.mock;

import javax.persistence.EntityManager;

import org.junit.Test;

public class XlsResultWriterHandlerTest {

	@Test
	public void testHappyPath() {
		EntityManager em = mock(EntityManager.class);
		
		XlsResultWriterHandler handler = spy(new XlsResultWriterHandler());
	}
}
