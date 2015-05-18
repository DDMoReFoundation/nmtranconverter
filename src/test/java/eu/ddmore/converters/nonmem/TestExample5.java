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
import org.junit.Ignore;
import org.junit.Test;

import crx.converter.engine.ConversionDetail_;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail.Severity;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.api.response.ConversionReport.ConversionCode;

@Ignore("Currently not supported")
public class TestExample5 extends TestBase {
    @Before
    public void setUp() throws Exception {
        inputXMLFile = "example5/example5_NONMEM.xml"; //example5_NONMEM_03_fixed_noSimpleParam
        inputDataFile = "example5/datasets/example5_NONMEM.csv";

        init(inputDataFile, V_0_6_SUBDIR);
        init(inputXMLFile, V_0_6_SUBDIR);
    }

    //	@Ignore
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
