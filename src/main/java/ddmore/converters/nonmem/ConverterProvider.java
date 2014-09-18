
package ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import crx.converter.engine.Lexer;
import crx.converter.engine.ScriptDefinition;
import ddmore.converters.nonmem.statements.DataStatement;
import ddmore.converters.nonmem.statements.EstimationStatement;
import ddmore.converters.nonmem.statements.SimulationStatement;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.domain.LanguageVersionImpl;
import eu.ddmore.convertertoolbox.domain.VersionImpl;

public class ConverterProvider extends Lexer {
	String model_filename = new String(); 
	public ConverterProvider() throws IOException, NullPointerException {
		super();
		
		// Register lexer/parser dependency.
		Parser p = new Parser();
		setParser(p);
		p.setLexer(this);
		
		VersionImpl source_version = new VersionImpl(0, 3, 1);
		source = new LanguageVersionImpl("PharmML", source_version);
		
		VersionImpl target_version = new VersionImpl(3, 0, 2);
		target = new LanguageVersionImpl("nonmem", target_version);
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
//		writeFunction
		String output_filename = parser.getScriptFilename(outputDirectory.getAbsolutePath());
		PrintWriter fout = new PrintWriter(output_filename);
		
		createPKPDScript(fout, outputDirectory);
		
		parser.writeEOF(fout);
		fout.close();
		fout = null;
		parser.cleanUp();
		
		File f = new File(output_filename);
		
		return f;
		
	}
	
	private void createPKPDScript(PrintWriter fout, File output_dir) throws IOException {
		if (fout == null || output_dir == null) return;
		
		ScriptDefinition scriptDefinition = getScriptDefinition();
		fout.write(getProblemStatement());
		//Initialise data statement before generating input statement and data statement
		DataStatement datastatement = new DataStatement(scriptDefinition,getDataFiles(),model_filename);
		
		fout.write(getInputStatement(datastatement));
		fout.write(getDataStatement(datastatement));
		
		EstimationStatement estStatement = new EstimationStatement(scriptDefinition);
		
		if(!estStatement.getEstimationSteps().isEmpty()){
			fout.write(estStatement.getEstimationStatement().toString());
			fout.write(estStatement.getCovStatement());	
		}
		
		fout.write(getSimulationStatement());

		parser.writeParameters(fout);
		
//		parser.getThetasStatement();
//		parser.getOmegasStatement();
//		parser.getSigmasStatement();
//		parser.getPriorStatement();
//		parser.getPredStatement(); 
//		parser.getTableStatement();

	}
	
	private String getInputStatement(DataStatement datastatement) {
		StringBuilder stringBuilder = new StringBuilder();
		stringBuilder.append("\n$INPUT ");
		for (String nextColumn : datastatement.getInputHeaders()){
			stringBuilder.append(nextColumn+ " ");
		}
		return stringBuilder.toString();
	}

	private String getProblemStatement(){
		String title = getModelName();
		String format = "\n$PROBLEM \t%s\n";
		return (title != null)?String.format(format, title):String.format(format);
	}
	
	private String getDataStatement(DataStatement datastatement){
		return datastatement.getDataStatement();	
	}
	
	private String getSimulationStatement(){
		SimulationStatement simulationStatement =new SimulationStatement(getSimulationStep());
		if(simulationStatement.getSimulationStep()!=null){
			return simulationStatement.getSimulationStatement();
		}
		return new String();
	}
}
