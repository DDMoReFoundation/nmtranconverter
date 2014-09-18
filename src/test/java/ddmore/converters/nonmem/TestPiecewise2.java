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
import crx.models.Create_Piecewise2;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail.Severity;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.api.response.ConversionReport.ConversionCode;

public class TestPiecewise2 extends TestBase {
	@Before
	public void setUp() throws Exception {
		Create_Piecewise2.main(null);
		inputXMLFile = "../output/piecewise-2.xml";
		expected_output_file1 = "../output/idx_3_simulation.csv";
		init();
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
