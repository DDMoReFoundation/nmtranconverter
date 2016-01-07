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

public class TestAllUseCaseConversions extends TestBase {

    @Before
    public void setUp() throws Exception {
        //General cov issue
        //        inputXMLFile = "usecases/UC001_08_NM_FOCEI.xml";
        //        inputDataFile = "usecases/warfarin_conc.csv";

        //#218 : piecewise issue
        //        inputXMLFile = "usecases/UC009_01_NM_FOCEI.xml";
        //        inputDataFile = "usecases/warfarin_infusion.csv";

        //#278 : covariate issue
        //        inputXMLFile = "usecases/UseCase5_SF278.xml";
        //        inputDataFile = "usecases/warfarin_conc_sexf.csv";

        //discrete TTE 
        //        inputXMLFile = "usecases/UseCase14.xml";
        //        inputDataFile = "usecases/warfarin_TTE_exact.csv";

        //        inputXMLFile = "usecases/UseCase2_1.xml";
        //        inputDataFile = "usecases/warfarin_conc_analytic.csv";

        //#289
        //        inputXMLFile = "usecases/UseCase4_FOCEI.xml";
        //        inputDataFile = "usecases/warfarin_infusion_oral.csv";

        //        inputXMLFile = "Friberg_2009_PANSS/Friberg_Prolactine_CPT2009_HM_Prod4_20150520.xml";
        //        inputDataFile = "Friberg_2009_PANSS/ex_data_prolactin4.csv";

        //        inputXMLFile = "usecases/temp/UseCase14_synonym_example_HBN3.xml";
        //        inputDataFile = "usecases/temp/warfarin_TTE_exact.csv";

        //        init(inputDataFile, V_0_6_SUBDIR); //V_0_4_1_SUBDIR
        //        init(inputXMLFile, V_0_6_SUBDIR);

        //Cat cov mapping SEX
        //        inputXMLFile = "usecases/UseCase5.xml";
        //        inputDataFile = "usecases/warfarin_conc_sexf.csv";

        inputXMLFile = "usecases/UseCase15.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";

                init(inputDataFile, V_0_6_SUBDIR); //V_0_4_1_SUBDIR
                init(inputXMLFile, V_0_6_SUBDIR);

//        init(inputDataFile, V_0_4_1_SUBDIR); //V_0_6_SUBDIR
//        init(inputXMLFile, V_0_4_1_SUBDIR);
    }

    public void newSetUp(String inputXMLFile, String inputDataFile) throws Exception {
        //        init(inputDataFile, V_0_6_SUBDIR); //V_0_4_1_SUBDIR
        //        init(inputXMLFile, V_0_6_SUBDIR);

        init(inputDataFile, V_0_4_1_SUBDIR); //V_0_6_SUBDIR
        init(inputXMLFile, V_0_4_1_SUBDIR);
    }

    //    @Ignore
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
    public void pkMacroConversionsTest() throws Exception{
        inputXMLFile = "usecases/pkmacro/Magni2000_COV.xml";
        inputDataFile = "usecases/pkmacro/magni2000_subjects.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase10_1.xml";
        inputDataFile = "usecases/pkmacro/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase10.xml";
        inputDataFile = "usecases/pkmacro/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase15.xml";
        inputDataFile = "usecases/pkmacro/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase4_1.xml";
        inputDataFile = "usecases/pkmacro/warfarin_infusion_oral.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase7.xml";
        inputDataFile = "usecases/pkmacro/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase7_1.xml";
        inputDataFile = "usecases/pkmacro/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/pkmacro/UseCase8_4.xml";
        inputDataFile = "usecases/pkmacro/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();
    }

    @Test
    public void iovConversionsTest() throws Exception{
        inputXMLFile = "usecases/iov/UseCase8_1107_example1.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example2.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example3.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example4.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example5.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example6.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example7.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example8.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example9.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example10.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example11.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/iov/UseCase8_1107_example12.xml";
        inputDataFile = "usecases/iov/warfarin_conc_bov_P4_5occ.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();
    }

    @Test
    public void allNewGrammarConversionsTest() throws Exception{

        inputXMLFile = "usecases/UseCase1.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 2
        inputXMLFile = "usecases/UseCase2.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase2_5.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //        test();

        inputXMLFile = "usecases/UseCase3.xml";
        inputDataFile = "usecases/warfarin_conc_pca.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase4.xml";
        inputDataFile = "usecases/warfarin_infusion_oral.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase5.xml";
        inputDataFile = "usecases/warfarin_conc_sexf.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase6.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 7
        inputXMLFile = "usecases/UseCase7.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 8
        inputXMLFile = "usecases/UseCase8.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4_sort.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase8_4.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4_sort.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        //// 9
        inputXMLFile = "usecases/UseCase9.xml";
        inputDataFile = "usecases/warfarin_infusion.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase10.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase10_1.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 11
        inputXMLFile = "usecases/UseCase11.xml";
        inputDataFile = "usecases/count.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase14.xml";
        inputDataFile = "usecases/warfarin_TTE_exact.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase15.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 16
        inputXMLFile = "usecases/UseCase16.xml";
        inputDataFile = "usecases/BIOMARKER_simDATA.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "usecases/UseCase17.xml";
        inputDataFile = "usecases/warfarin_conc_SS.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase17_1.xml";
        inputDataFile = "usecases/warfarin_conc_SSADDL.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();
    }

    //    @Ignore
    @Test
    public void allConversionsTest() throws Exception{

        //// 1
        inputXMLFile = "usecases/UseCase1.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase1_10.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase1_11.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase1_7.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase1_8.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase1_9.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 2
        inputXMLFile = "usecases/UseCase2.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase2_1.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        //Not in Use
        inputXMLFile = "usecases/UseCase2_2.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase2_5.xml";
        inputDataFile = "usecases/warfarin_conc_analytic.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        //// 3
        inputXMLFile = "usecases/UseCase3.xml";
        inputDataFile = "usecases/warfarin_conc_pca.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 4
        inputXMLFile = "usecases/UseCase4.xml";
        inputDataFile = "usecases/warfarin_infusion_oral.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "usecases/UseCase4_1.xml";
        inputDataFile = "usecases/warfarin_infusion_oral.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 5
        inputXMLFile = "usecases/UseCase5.xml";
        inputDataFile = "usecases/warfarin_conc_sexf.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase5_1.xml";
        inputDataFile = "usecases/warfarin_conc_sex.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "usecases/UseCase5_2.xml";
        inputDataFile = "usecases/warfarin_conc_sex.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        inputXMLFile = "usecases/UseCase5_3.xml";
        inputDataFile = "usecases/warfarin_conc_sex.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();

        //// 6
        inputXMLFile = "usecases/UseCase6.xml";
        inputDataFile = "usecases/warfarin_conc.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 7
        inputXMLFile = "usecases/UseCase7.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 8
        inputXMLFile = "usecases/UseCase8.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase8_1.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase8_3.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase8_4.xml";
        inputDataFile = "usecases/warfarin_conc_bov_P4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 9
        inputXMLFile = "usecases/UseCase9.xml";
        inputDataFile = "usecases/warfarin_infusion.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 10
        inputXMLFile = "usecases/UseCase10.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase10_1.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 11
        inputXMLFile = "usecases/UseCase11.xml";
        inputDataFile = "usecases/count.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase11_1.xml";
        inputDataFile = "usecases/count.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 12
        inputXMLFile = "usecases/UseCase12.xml";
        inputDataFile = "usecases/binary.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase12_1.xml";
        inputDataFile = "usecases/binary.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase12_2.xml";
        inputDataFile = "usecases/binary.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 13
        inputXMLFile = "usecases/UseCase13.xml";
        inputDataFile = "usecases/category.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase13_1.xml";
        inputDataFile = "usecases/category.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase14.xml";
        inputDataFile = "usecases/warfarin_TTE_exact.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase14_1.xml";
        inputDataFile = "usecases/warfarin_TTE_intervalCensored.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase14_2.xml";
        inputDataFile = "usecases/warfarin_TTE_intervalCensored.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 15
        inputXMLFile = "usecases/UseCase15.xml";
        inputDataFile = "usecases/warfarin_conc_cmt.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 16
        inputXMLFile = "usecases/UseCase16.xml";
        inputDataFile = "usecases/BIOMARKER_simDATA.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "usecases/UseCase17.xml";
        inputDataFile = "usecases/warfarin_conc_SSADDL.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        //// 19 - removed for now
        //        inputXMLFile = "usecases/UseCase19.xml";
        //        inputDataFile = "usecases/warfarin_conc_pca.csv";
        //        newSetUp(inputXMLFile, inputDataFile);
        //        test();

        ////PANSS
        inputXMLFile = "Friberg_2009_PANSS/Friberg_2009_Schizophrenia_Asenapine_PANSS_HM_20150520_Prod4.xml";
        inputDataFile = "Friberg_2009_PANSS/PANSS_Friberg2009_simdata_2.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();
        //
        inputXMLFile = "Friberg_2009_PANSS/Friberg_Prolactine_CPT2009_HM_Prod4_20150520.xml";
        inputDataFile = "Friberg_2009_PANSS/ex_data_prolactin4.csv";
        newSetUp(inputXMLFile, inputDataFile);
        //test();
        //
        inputXMLFile = "Friberg_2009_PANSS/Inverse_binomial_mod.xml";
        inputDataFile = "Friberg_2009_PANSS/Count.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "Friberg_2009_PANSS/Poisson_AOM_mod.xml";
        inputDataFile = "Friberg_2009_PANSS/Count.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();

        inputXMLFile = "Friberg_2009_PANSS/UPDRS1.xml";
        inputDataFile = "Friberg_2009_PANSS/UPDRS.csv";
        newSetUp(inputXMLFile, inputDataFile);
        test();
    }
}
