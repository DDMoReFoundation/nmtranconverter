/*******************************************************************************
 * Copyright (C) 2015 Mango Solutions Ltd - All rights reserved.
 ******************************************************************************/
package eu.ddmore.converters.nonmem;

import java.io.File;
import java.io.IOException;
import java.util.List;

import crx.converter.engine.ConversionDetail_;
import crx.converter.spi.IScriptMaker;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail;
import eu.ddmore.convertertoolbox.api.response.ConversionReport;
import eu.ddmore.convertertoolbox.api.response.ConversionDetail.Severity;
import eu.ddmore.convertertoolbox.api.response.ConversionReport.ConversionCode;

public class ScriptMaker implements IScriptMaker {
	@Override
	public File createScript(File f, File d) throws NullPointerException, IOException {
		
		File script_file = null;
		ConverterProvider c = new ConverterProvider();
		ConverterProvider.getManager().setFixedRunId(true);
		c.setUseCrxImplConversionReport(true);
		
		ConversionReport report = c.performConvert(f, d);
		if (report == null) throw new NullPointerException("Script creation failed.");
		if (report.getReturnCode() == ConversionCode.SUCCESS) {
			List<ConversionDetail> details = report.getDetails(Severity.INFO);
			if (!details.isEmpty()) {
				ConversionDetail_ detail = (ConversionDetail_) details.get(0);
				script_file = detail.getFile();
			}
		}
		
		return script_file; 
	}
}
