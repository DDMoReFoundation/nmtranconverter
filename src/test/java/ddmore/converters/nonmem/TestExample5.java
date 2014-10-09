/*******************************************************************************
 * Copyright (C) 2014 Cyprotex Discovery Ltd - All rights reserved.
 ******************************************************************************/

package ddmore.converters.nonmem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import crx.converter.engine.ConversionDetail_;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail.Severity;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.api.response.ConversionReport.ConversionCode;

public class TestExample5 extends TestBase {
	@Before
	public void setUp() throws Exception {
		inputXMLFile = "warfarin_PK_ODE/warfarin_PK_ODE_0.4.xml";
		init(inputXMLFile, V_0_4_SUBDIR);
	}

	@Test
	public void test() {
		assertTrue(dir.exists());
		assertTrue(f.exists());
		
		ConversionReport report = c.performConvert(f, dir);
		assertNotNull(report);
		assertEquals(report.getReturnCode(), ConversionCode.SUCCESS);
		List<ConversionDetail> details = report.getDetails(Severity.INFO);
		assertFalse(details.isEmpty());
		assertNotNull(details.get(0));
		ConversionDetail_ detail = (ConversionDetail_) details.get(0);
		f = detail.getFile();
		assertTrue(f.exists());
	}
}
