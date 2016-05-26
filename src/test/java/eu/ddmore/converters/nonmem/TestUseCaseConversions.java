/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import crx.converter.engine.ConversionDetail_;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail.Severity;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.api.response.ConversionReport.ConversionCode;

/*
 * This test uses test data from test-models.jar, currently PharmML 0.8.1 test data are not available. But you can add them to locally built test-models
 * under 'PharmML/0.8.1' based on the results from Converters ATH.
 */
@Ignore("TODO check validity of the test as it is redundant with Converters ATH")
@RunWith(Parameterized.class)
public class TestUseCaseConversions extends TestBase {

    public TestUseCaseConversions(String inputXMLFile, String inputDataFile) throws Exception {
        super();
        this.inputDataFile = inputDataFile;
        this.inputXMLFile= inputXMLFile;
        updateSetUpWith(inputXMLFile, inputDataFile);
    }

    @Parameterized.Parameters(name= "{index}: Model {0}")
    public static Iterable<Object[]> getModelsToTest() throws Exception {
        return Arrays.asList(new Object[][] {{"Simeoni.xml","Simulated_PAGE.csv"},
        {"UseCase1.xml", "warfarin_conc.csv"},
        {"UseCase2.xml", "warfarin_conc.csv"},
        {"UseCase3.xml", "warfarin_conc_pca.csv"},
        {"UseCase4.xml", "warfarin_infusion_oral.csv"},
        {"UseCase5.xml", "warfarin_conc_sexf.csv"},
        {"UseCase11.xml", "count.csv"},
        {"UseCase14.xml", "warfarin_TTE_exact.csv"}
        });
    }

    private void updateSetUpWith(String inputXMLFile, String inputDataFile) throws Exception {
        init(inputDataFile, V_0_8_1_SUBDIR);
        init(inputXMLFile, V_0_8_1_SUBDIR);
    }

    @Test
    public void test() {
        assertTrue(dir.exists());
        assertTrue(f.exists());

        ConversionReport report = c.performConvert(f, dir);
        assertNotNull(report);
        assertEquals("Failed for file : "+f.getName(), ConversionCode.SUCCESS, report.getReturnCode());
        List<ConversionDetail> details = report.getDetails(Severity.INFO);
        assertFalse(details.isEmpty());
        assertNotNull(details.get(0));
        ConversionDetail_ detail = (ConversionDetail_) details.get(0);
        f = detail.getFile();
        assertTrue(f.exists());
    }
}
