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

@Ignore("TODO check validity of the test as it is redundant with Converters ATH")
public class TestUseCaseConversions extends TestBase {

    @Before
    public void setUp() throws Exception {

        //inputXMLFile = "UseCase1.xml";
        //inputDataFile = "warfarin_conc.csv";

        //inputXMLFile = "UseCase7.xml";
        //inputDataFile = "warfarin_conc_cmt.csv";

        //inputXMLFile = "UseCase15.xml";
        //inputDataFile = "warfarin_conc_cmt.csv";

//        inputXMLFile = "pkmacro/UseCase7_HBN_ADVAN4_2.xml";
//        inputDataFile = "pkmacro/warfarin_conc_cmt.csv";
        
        inputXMLFile = "UseCase10.xml";
        inputDataFile = "warfarin_conc_cmt.csv";

        init(inputDataFile, V_0_8_SUBDIR);
        init(inputXMLFile, V_0_8_SUBDIR);
    }

    private void updateSetUpWith(String inputXMLFile, String inputDataFile) throws Exception {
        init(inputDataFile, V_0_8_SUBDIR);
        init(inputXMLFile, V_0_8_SUBDIR);
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

    @Test
    public void allUseCaseConversionsTest() throws Exception{

        inputXMLFile = "pkmacro/UseCase7_HBN_ADVAN1_2.xml";
        inputDataFile = "pkmacro/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "pkmacro/UseCase7_HBN_ADVAN3_2.xml";
        inputDataFile = "pkmacro/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "pkmacro/UseCase7_HBN_ADVAN4_2.xml";
        inputDataFile = "pkmacro/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "pkmacro/PKmacros_advan1_HBN.xml";
        inputDataFile = "pkmacro/example1.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan1.xml";
        inputDataFile = "pkmacro/example1.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan10.xml";
        inputDataFile = "pkmacro/example10.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan11.xml";
        inputDataFile = "pkmacro/example11.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan12.xml";
        inputDataFile = "pkmacro/example12.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan2.xml";
        inputDataFile = "pkmacro/example2.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan3.xml";
        inputDataFile = "pkmacro/example3.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "pkmacro/PKmacros_advan4.xml";
        inputDataFile = "pkmacro/example4.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "UseCase1.xml";
        inputDataFile = "warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase2.xml";
        inputDataFile = "warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase2_1.xml";
        inputDataFile = "warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase2_2.xml";
        inputDataFile = "warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        //N/A
        inputXMLFile = "UseCase2_5.xml";
        inputDataFile = "warfarin_conc_analytic.csv";
        //updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "UseCase3.xml";
        inputDataFile = "warfarin_conc_pca.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase4.xml";
        inputDataFile = "warfarin_infusion_oral.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase5.xml";
        inputDataFile = "warfarin_conc_sexf.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase6.xml";
        inputDataFile = "warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase7.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase8.xml";
        inputDataFile = "warfarin_conc_bov_P4_sort.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase8_4.xml";
        inputDataFile = "warfarin_conc_bov_P4.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase9.xml";
        inputDataFile = "warfarin_infusion.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase10.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase10_1.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase11.xml";
        inputDataFile = "count.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "UseCase14.xml";
        inputDataFile = "warfarin_TTE_exact.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase15.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase16.xml";
        inputDataFile = "BIOMARKER_simDATA.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase17.xml";
        inputDataFile = "warfarin_conc_SS.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "UseCase17_1.xml";
        inputDataFile = "warfarin_conc_SSADDL.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();
    }

}
