package com.axway.apim.setup.remoteHosts.it.tests;

import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.apim.setup.remoteHosts.it.ExportRemoteHostsTestAction;
import com.axway.apim.setup.remoteHosts.it.ImportRemoteHostsTestAction;
import com.axway.lib.testActions.TestParams;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.message.MessageType;

@Test
public class ImportAndExportRemoteHostsTestIT extends TestNGCitrusTestRunner implements TestParams {
	
	private static String PACKAGE = "/com/axway/apim/setup/remotehost/it/tests/";
	
	@CitrusTest
	@Test @Parameters("context")
	public void runRemoteHostsImportExport(@Optional @CitrusResource TestContext context) throws Exception {
		description("Export/Import RemoteHosts from and into the API-Manager");
		ImportRemoteHostsTestAction importApp = new ImportRemoteHostsTestAction(context);
		ExportRemoteHostsTestAction exportApp = new ExportRemoteHostsTestAction(context);
		
		echo("####### Add Remote-Host 1 #######");
		createVariable(PARAM_CONFIGFILE,  PACKAGE + "remote-host-1.json");
		createVariable(PARAM_EXPECTED_RC, "0");
		createVariable("${remoteHostName}", "sample.remote.host"+importApp.getRandomNum());
		createVariable("${remoteHostPort}", "8888");
		importApp.doExecute(context);
		
		echo("####### Validate remote host has 1 been added correctly #######");
		http(builder -> builder.client("apiManager").send().get("/remotehosts").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$.[?(@.name=='${remoteHostName}')].name", "${remoteHostName}")
				.validate("$.[?(@.name=='${remoteHostName}')].port", "${remoteHostPort}"));
		
		echo("####### Add Remote-Host 2 #######");
		createVariable(PARAM_CONFIGFILE,  PACKAGE + "remote-host-1.json");
		createVariable(PARAM_EXPECTED_RC, "0");
		createVariable("${remoteHostName}", "another.remote.host"+importApp.getRandomNum());
		createVariable("${remoteHostPort}", "9999");
		importApp.doExecute(context);
		
		echo("####### Validate remote host has 2 been added correctly #######");
		http(builder -> builder.client("apiManager").send().get("/remotehosts").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$.[?(@.name=='${remoteHostName}')].name", "${remoteHostName}")
				.validate("$.[?(@.name=='${remoteHostName}')].port", "${remoteHostPort}")
				.extractFromPayload("$.[?(@.name=='${remoteHostName}')].id", "remoteHostId"));
		
		echo("####### Update remote host 2 #######");
		createVariable(PARAM_CONFIGFILE,  PACKAGE + "remote-host-1.json");
		createVariable(PARAM_EXPECTED_RC, "0");
		createVariable("${remoteHostPort}", "5555");
		importApp.doExecute(context);
		
		echo("####### Validate remote host has 2 been updated correctly #######");
		http(builder -> builder.client("apiManager").send().get("/remotehosts").header("Content-Type", "application/json"));
		http(builder -> builder.client("apiManager").receive().response(HttpStatus.OK).messageType(MessageType.JSON)
				.validate("$.[?(@.id=='${remoteHostId}')].name", "${remoteHostName}")
				.validate("$.[?(@.id=='${remoteHostId}')].port", "${remoteHostPort}"));
		
		echo("####### Export remote host 2 #######");
		createVariable(PARAM_EXPECTED_RC, "0");
		createVariable(PARAM_TARGET, exportApp.getTestDirectory().getPath());
		createVariable(PARAM_OUTPUT_FORMAT, "json");
		createVariable(PARAM_NAME, "another*"+importApp.getRandomNum());
		exportApp.doExecute(context);
		
		Assert.assertEquals(exportApp.getLastResult().getExportedFiles().size(), 1, "One remote host is expected");
	}
}
