package it.quartara.boser.console.pdfcmgr;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ PDFCManagerHelperTest.class, PDFCManagerJobTest.class,
		PDFCManagerServletTest.class })
public class AllTests {

}
