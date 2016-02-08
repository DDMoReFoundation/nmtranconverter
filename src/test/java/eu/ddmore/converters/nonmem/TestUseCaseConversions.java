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

public class TestUseCaseConversions extends TestBase {

    @Before
    public void setUp() throws Exception {

        inputXMLFile = "UseCase7.xml";
        inputDataFile = "warfarin_conc_cmt.csv";

//        inputXMLFile = "UseCase3_511.xml";
//        inputDataFile = "warfarin_conc_pca.csv";

        init(inputDataFile, V_0_8_SUBDIR);
        init(inputXMLFile, V_0_8_SUBDIR);
        //init(inputDataFile, V_0_4_1_SUBDIR+USECASE_DIR);
        //init(inputXMLFile, V_0_4_1_SUBDIR+USECASE_DIR);
    }

    /**
     * Used while executing more than one use cases are getting converted.
     * @param inputXMLFile
     * @param inputDataFile
     * @throws Exception
     */
    public void updateSetUpWith(String inputXMLFile, String inputDataFile) throws Exception {
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

        /*
         * 
         * Err2
         * java.lang.IllegalArgumentException: No enum constant eu.ddmore.converters.nonmem.statements.PkMacroAnalyser.PkMacroAttribute.ADM
            at java.lang.Enum.valueOf(Enum.java:236)
            at eu.ddmore.converters.nonmem.statements.PkMacroAnalyser$PkMacroAttribute.valueOf(PkMacroAnalyser.java:1)

           Err3
            java.lang.NullPointerException: The PharmML element mapped to a data column cannot be found in the model (column='DV')
            at crx.converter.engine.parts.BaseTabularDataset$ElementMapping.<init>(BaseTabularDataset.java:60)
            at crx.converter.engine.parts.BaseStep.processColumnMapping(BaseStep.java:532)

           Err4
            java.lang.UnsupportedOperationException: Observation scope parameter type unsupported (VariableDefinition)
            at crx.converter.engine.parts.ObservationBlock.initErrorModel(ObservationBlock.java:726)
            at crx.converter.engine.parts.ObservationBlock.<init>(ObservationBlock.java:196)
         * 
         */

        inputXMLFile = "UseCase1.xml";
        inputDataFile = "warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        //err1
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

        //err1
        inputXMLFile = "UseCase3.xml";
        inputDataFile = "warfarin_conc_pca.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        //err1
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

        //err2
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

        //err2
        inputXMLFile = "UseCase10.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        //err2
        inputXMLFile = "UseCase10_1.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        //err3
        inputXMLFile = "UseCase11.xml";
        inputDataFile = "count.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        //err3
        inputXMLFile = "UseCase14.xml";
        inputDataFile = "warfarin_TTE_exact.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        //err2
        inputXMLFile = "UseCase15.xml";
        inputDataFile = "warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        //err4
        inputXMLFile = "UseCase16.xml";
        inputDataFile = "BIOMARKER_simDATA.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

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
