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

        inputXMLFile = "usecases/UseCase15.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";

        init(inputDataFile, V_0_4_1_SUBDIR);
        init(inputXMLFile, V_0_4_1_SUBDIR);
    }

    public void updateSetUpWith(String inputXMLFile, String inputDataFile) throws Exception {
        init(inputDataFile, V_0_4_1_SUBDIR);
        init(inputXMLFile, V_0_4_1_SUBDIR);
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

        inputXMLFile = "usecases/UseCase1.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase2.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase2_5.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //        test();

        inputXMLFile = "usecases/UseCase3.xml";
        inputDataFile = "usecases/warfarin_conc_pca.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase4.xml";
        inputDataFile = "usecases/warfarin_infusion_oral.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase5.xml";
        inputDataFile = "usecases/warfarin_conc_sexf.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase6.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase7.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase8.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4_sort.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase8_4.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4_sort.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "usecases/UseCase9.xml";
        inputDataFile = "usecases/warfarin_infusion.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase10.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase10_1.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase11.xml";
        inputDataFile = "usecases/count.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase14.xml";
        inputDataFile = "usecases/warfarin_TTE_exact.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase15.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase16.xml";
        inputDataFile = "usecases/BIOMARKER_simDATA.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "usecases/UseCase17.xml";
        inputDataFile = "usecases/warfarin_conc_SS.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase17_1.xml";
        inputDataFile = "usecases/warfarin_conc_SSADDL.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();
    }

}
