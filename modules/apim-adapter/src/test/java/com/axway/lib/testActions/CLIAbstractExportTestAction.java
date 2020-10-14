package com.axway.lib.testActions;

import java.io.File;

import com.axway.apim.lib.CoreParameters;
import com.axway.apim.lib.ExportResult;
import com.axway.apim.lib.StandardExportParams;
import com.axway.apim.lib.StandardExportParams.OutputFormat;
import com.consol.citrus.context.TestContext;

public abstract class CLIAbstractExportTestAction extends CLIAbstractTestAction implements TestParams {
	
	protected ExportResult lastResult;

	public CLIAbstractExportTestAction(TestContext context) {
		super(context);
	}
	
	@Override
	public void doExecute(TestContext context, File testDirectory) {
		this.lastResult = runTest(context);
	}
	
	@Override
	protected void addParameters(CoreParameters params, TestContext context) {
		super.addParameters(params, context);
		((StandardExportParams)params).setTarget(context.getVariable(PARAM_TARGET));
		((StandardExportParams)params).setOutputFormat(OutputFormat.valueOf(context.getVariable(PARAM_OUTPUT_FORMAT)));
	}

	public abstract ExportResult runTest(TestContext context);

	public ExportResult getLastResult() {
		return lastResult;
	}
}
