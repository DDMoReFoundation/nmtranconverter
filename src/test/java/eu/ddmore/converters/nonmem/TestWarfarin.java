/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

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

public class TestWarfarin extends TestBase {
	@Before
	public void setUp() throws Exception {
		inputXMLFile = "Warfarin_ODE/Warfarin-ODE-latest.xml";
		inputDataFile = "Warfarin_ODE/warfarin_conc.csv";
		// Warfarin-ODE-latest_Natallia.xml Warfarin-ODE-latest.xml warfarin_PK_ODE_0.3.1.xml
		init(inputDataFile, V_0_3_1_SUBDIR);
		init(inputXMLFile, V_0_3_1_SUBDIR);

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
