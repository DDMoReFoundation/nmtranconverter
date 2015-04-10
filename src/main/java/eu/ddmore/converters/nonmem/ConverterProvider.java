/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import crx.converter.engine.Lexer;
import crx.converter.engine.ScriptDefinition;
import crx.converter.engine.parts.EstimationStep;
import eu.ddmore.converters.nonmem.statements.DataStatement;
import eu.ddmore.converters.nonmem.statements.EstimationStatement;
import eu.ddmore.converters.nonmem.statements.InputStatement;
import eu.ddmore.converters.nonmem.statements.ProblemStatement;
import eu.ddmore.converters.nonmem.statements.SimulationStatement;
import eu.ddmore.converters.nonmem.statements.TableStatement;
import eu.ddmore.converters.nonmem.utils.Formatter;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.domain.LanguageVersionImpl;
import eu.ddmore.convertertoolbox.domain.VersionImpl;


public class ConverterProvider extends Lexer {
    String model_filename = new String();
    private static final String SCRIPT_FILE_SUFFIX = "ctl";

    public ConverterProvider() throws IOException, NullPointerException {
        super();

        // Register lexer/parser dependency.
        Parser p = new Parser();
        setParser(p);
        p.setLexer(this);

        VersionImpl source_version = new VersionImpl(0, 3, 1);
        source = new LanguageVersionImpl("PharmML", source_version);

        VersionImpl target_version = new VersionImpl(7, 2, 0);
        target = new LanguageVersionImpl("NMTRAN", target_version);

        converterVersion = new VersionImpl(1, 0, 0);
    }

    @Override
    public ConversionReport performConvert(File src, File outputDirectory) {
        getScriptDefinition().flushAllSymbols();
        try {
            parser.setRunId(m.generateRunId());
        } catch (Exception e) {
            return getExceptionReport(e);
        }
        try {
            setOutputDirectory(outputDirectory);
        } catch (Exception e) {
            return getExceptionReport(e);
        }
        try {
            loadPharmML(src);
        } catch (Exception e) {
            return getExceptionReport(e);
        }
        // Parse each of the available structural blocks.
        try {
            createBlocks(outputDirectory);
            File f = createScript(src, outputDirectory);
            return getCrxSuccessReport(f);
        } catch (Exception e) {
            return getExceptionReport(e);
        }
    }

    private File createScript(File src, File outputDirectory) throws Exception {
        if (outputDirectory == null) throw new NullPointerException("The output directroy is NULL");
        model_filename = src.getName().replace(".xml", ""); 
        String output_filename = getScriptFilename(outputDirectory.getAbsolutePath(),model_filename);
        PrintWriter fout = new PrintWriter(output_filename);

        parser.writePreMainBlockElements(fout,src);
        createPKPDScript(fout,src, outputDirectory);

        parser.writeEOF(fout);
        fout.close();
        fout = null;
        parser.cleanUp();
        File f = new File(output_filename);
        return f;
    }

    public String getScriptFilename(String output_dir, String modelFileName) {
        String format = "%s/%s.%s";
        return String.format(format, output_dir, modelFileName, SCRIPT_FILE_SUFFIX);
    }

    private void createPKPDScript(PrintWriter fout, File src, File output_dir) throws IOException {
        if (fout == null || output_dir == null) return;

        ScriptDefinition scriptDefinition = getScriptDefinition();
        ProblemStatement problemStatement = new ProblemStatement(getModelName());
        problemStatement.write(fout);
        fout.write(Formatter.endline());

        //Initialise data statement before generating input statement and data statement
        InputStatement inputStatement;
        DataStatement dataStatement;

        if (getDataFiles().getExternalDataSets().isEmpty()) {
            inputStatement = new InputStatement(scriptDefinition);
            dataStatement = new DataStatement(scriptDefinition, src);
        } else {
            inputStatement = new InputStatement(getDataFiles().getExternalDataSets());
            dataStatement = new DataStatement(getDataFiles().getExternalDataSets(),src);
        }
        fout.write(Formatter.endline());
        fout.write(inputStatement.getStatement());
        fout.write(Formatter.endline());
        fout.write(dataStatement.getStatement());
        fout.write(getSimulationStatement());

        parser.writeParameterStatement(fout);

        EstimationStatement estStatement = new EstimationStatement(scriptDefinition);
        if(!estStatement.getEstimationSteps().isEmpty()){
            fout.write(estStatement.getEstimationStatement().toString());
            fout.write(estStatement.getCovStatement());	
        }
        TableStatement tableStatement = new TableStatement(scriptDefinition,inputStatement);
        fout.write(tableStatement.getStatements().toString());
    }

    private String getSimulationStatement(){
        SimulationStatement simulationStatement =new SimulationStatement(getSimulationStep());
        if(simulationStatement.getSimulationStep()!=null){
            return simulationStatement.getSimulationStatement();
        }
        return new String();
    }

    @Override
    protected void initialise() {
        EstimationStep.setUseDefaultParameterEstimate(true);
        EstimationStep.setDefaultParameterEstimateValue(1.0);
    }
}