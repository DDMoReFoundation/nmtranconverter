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

//@Ignore("TODO check validity of the test as it is redundant with Converters ATH")
public class TestUseCaseConversions extends TestBase {

    private static final String JENKINS_DIR = "jenkins/";
    @Before
    public void setUp() throws Exception {

        //DDMORE-1701
        //inputXMLFile = JENKINS_DIR+"UseCase3.xml";
        //inputDataFile = JENKINS_DIR+"warfarin_conc_pca.csv";

        //depot macro
        //inputXMLFile = JENKINS_DIR+"UseCase4_3.xml";
        //inputDataFile = JENKINS_DIR+"warfarin_infusion_oral.csv";

        //inputXMLFile = JENKINS_DIR+"UseCase5.xml";
        //inputDataFile = JENKINS_DIR+"warfarin_conc_sexf.csv";

        //inputXMLFile = JENKINS_DIR+"UseCase10.xml";
        //inputDataFile = JENKINS_DIR+"warfarin_conc_cmt.csv";

        //DDMORE-1703
//        inputXMLFile = JENKINS_DIR+"UseCase3_1.xml";
//        inputDataFile = JENKINS_DIR+"warfarin_conc_pca_PKparam.csv";

        //DDMORE-1728
        inputXMLFile = JENKINS_DIR+"UseCase1_2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";

        init(inputDataFile, V_0_8_1_SUBDIR);
        init(inputXMLFile, V_0_8_1_SUBDIR);
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

    @Test
    public void allJenkinsUseCaseTests() throws Exception{

        inputXMLFile = JENKINS_DIR+"UseCase1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase1_2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";//"warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase2_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";//"warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase2_2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_analytic.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase3.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_pca.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase3_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_pca_PKparam.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase4.xml";
        inputDataFile = JENKINS_DIR+"warfarin_infusion_oral.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase4_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_infusion_oral.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase4_2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_infusion_oral.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase4_3.xml";
        inputDataFile = JENKINS_DIR+"warfarin_infusion_oral.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase5.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_sexf.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase5_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_sexf.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase5_2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_sexf.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase6.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase6_2.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase6_3.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase7.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";//"warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase8.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_bov_P4_sort.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase8_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_bov_P4_sort.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase9.xml";
        inputDataFile = JENKINS_DIR+"warfarin_infusion.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase10.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase10_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_cmt.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase11.xml";
        inputDataFile = JENKINS_DIR+"count.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase11_1.xml";
        inputDataFile = JENKINS_DIR+"count.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase12.xml";
        inputDataFile = JENKINS_DIR+"binary.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase12_1.xml";
        inputDataFile = JENKINS_DIR+"binary.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase12_2.xml";
        inputDataFile = JENKINS_DIR+"binomial.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase13.xml";
        inputDataFile = JENKINS_DIR+"category.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase13_1.xml";
        inputDataFile = JENKINS_DIR+"OrderedCategorical.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase14.xml";
        inputDataFile = JENKINS_DIR+"warfarin_TTE_exact.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase15.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_lnDV.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase16.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_blq.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase17.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_SS.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase17_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_SSADDL.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase19_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc_Parent_Metabolite.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase21.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase21_1.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = JENKINS_DIR+"UseCase20.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase23.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = JENKINS_DIR+"UseCase24.xml";
        inputDataFile = JENKINS_DIR+"warfarin_conc.csv";
        updateSetUpWith(inputXMLFile, inputDataFile);
        //test();
    }

    @Ignore
    @Test
    public void allUseCaseConversionsTest() throws Exception{

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
        test();

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
    }

}
