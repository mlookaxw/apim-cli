package com.axway.apim.export.test.basic;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;

import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import com.axway.apim.api.model.APIQuota;
import com.axway.apim.api.model.AuthenticationProfile;
import com.axway.apim.api.model.CorsProfile;
import com.axway.apim.api.model.SecurityProfile;
import com.axway.apim.api.model.TagMap;
import com.axway.apim.export.test.ExportTestAction;
import com.axway.apim.test.ImportTestAction;
import com.consol.citrus.annotations.CitrusResource;
import com.consol.citrus.annotations.CitrusTest;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.dsl.testng.TestNGCitrusTestRunner;
import com.consol.citrus.functions.core.RandomNumberFunction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Test
public class CompleteAPIExportTestIT extends TestNGCitrusTestRunner {

	private ExportTestAction swaggerExport = new ExportTestAction();
	private ImportTestAction swaggerImport = new ImportTestAction();
	private ObjectMapper mapper = new ObjectMapper();
	
	@CitrusTest
	@Test @Parameters("context")
	public void run(@Optional @CitrusResource TestContext context) throws IOException, InterruptedException {
		description("Import an API, export it afterwards and validate it equals to the imported API");
		variable("apiNumber", RandomNumberFunction.getRandomNumber(3, true));
		variable("apiPath", "/api/test/"+this.getClass().getSimpleName()+"-${apiNumber}");
		variable("apiName", this.getClass().getSimpleName()+"-${apiNumber}");
		variable("state", "published");
		
		echo("####### Importing the API, which should exported in the second step #######");
		createVariable(ImportTestAction.API_DEFINITION,  "/test/export/files/basic/petstore.json");
		createVariable(ImportTestAction.API_CONFIG,  "/test/export/files/basic/complete-config.json");
		createVariable("image", "/com/axway/apim/test/files/basic/API-Logo.jpg");
		createVariable("expectedReturnCode", "0");
		
		swaggerImport.doExecute(context);
		if(APIManagerAdapter.hasAPIManagerVersion("7.7.20200130")) {
			Thread.sleep(1000); // Starting with this version, we need to wait a few milliseconds, otherwise the REST-API doesn't return the complete set of quotas
		}
		
		exportAPI(context, false);
		exportAPI(context, true);
	}
	
	private void exportAPI(TestContext context, boolean ignoreAdminAccount) throws FileNotFoundException, IOException {
		variable("exportLocation", "citrus:systemProperty('java.io.tmpdir')");
		variable(ExportTestAction.EXPORT_API,  "${apiPath}");

		// These are the folder and filenames generated by the export tool 
		createVariable("exportFolder", "api-test-${apiName}");
		createVariable("exportAPIName", "${apiName}.json");
		
		if(ignoreAdminAccount) {
			echo("####### Exporting the API with Org-Admin permissions only #######");
			createVariable("exportLocation", "${exportLocation}/orgAdmin");
			createVariable("apiManagerUser", "${oadminUsername1}"); // This is an org-admin user
			createVariable("apiManagerPass", "${oadminPassword1}");
			createVariable("ignoreAdminAccount", "true"); // Don't use the Admin-Account given in the env.properties for this test
		} else {
			createVariable("exportLocation", "${exportLocation}/ignoreAdminAccount");
			echo("####### Exporting the API with Admin permissions #######");
		}
		
		echo("####### Export the API from the API-Manager #######");
		createVariable("expectedReturnCode", "0");
		swaggerExport.doExecute(context);
		
		String exportedAPIConfigFile = context.getVariable("exportLocation")+"/api.custom-host.com/"+context.getVariable("exportFolder")+"/api-config.json";
		
		echo("####### Reading exported API-Config file: '"+exportedAPIConfigFile+"' #######");
		JsonNode exportedAPIConfig = mapper.readTree(new FileInputStream(new File(exportedAPIConfigFile)));
		JsonNode importedAPIConfig = mapper.readTree(this.getClass().getResourceAsStream("/test/export/files/basic/complete-config.json"));

		assertEquals(exportedAPIConfig.get("path").asText(), 				context.getVariable("apiPath"));
		assertEquals(exportedAPIConfig.get("name").asText(), 				context.getVariable("apiName"));
		assertEquals(exportedAPIConfig.get("state").asText(), 				context.getVariable("state"));
		assertEquals(exportedAPIConfig.get("version").asText(), 			"1.0.0");
		assertEquals(exportedAPIConfig.get("organization").asText(),		"API Development "+context.getVariable("orgNumber"));
		assertEquals(exportedAPIConfig.get("summary").asText(), 			"My complete API-Summary");
		assertEquals(exportedAPIConfig.get("descriptionType").asText(), 	"manual");
		assertEquals(exportedAPIConfig.get("descriptionManual").asText(), 	"This is my __markdown__ based API description.");
		assertEquals(exportedAPIConfig.get("descriptionManual").asText(), 	"This is my __markdown__ based API description.");
		assertEquals(exportedAPIConfig.get("vhost").asText(), 				"api.custom-host.com");
		//assertEquals(exportedAPIConfig.get("backendBasepath").asText(), 	"http://any.server.com:7676");
		
		List<SecurityProfile> importedSecurityProfiles = mapper.convertValue(importedAPIConfig.get("securityProfiles"), new TypeReference<List<SecurityProfile>>(){});
		List<SecurityProfile> exportedSecurityProfiles = mapper.convertValue(exportedAPIConfig.get("securityProfiles"), new TypeReference<List<SecurityProfile>>(){});
		assertEquals(importedSecurityProfiles, exportedSecurityProfiles, "SecurityProfiles are not equal.");
		
		List<SecurityProfile> importedAuthenticationProfiles = mapper.convertValue(importedAPIConfig.get("authenticationProfiles"), new TypeReference<List<AuthenticationProfile>>(){});
		List<SecurityProfile> exportedAuthenticationProfiles = mapper.convertValue(exportedAPIConfig.get("authenticationProfiles"), new TypeReference<List<AuthenticationProfile>>(){});
		assertEquals(importedAuthenticationProfiles, exportedAuthenticationProfiles, "AuthenticationProfiles are not equal.");
		
		TagMap<String, String[]> importedTags = mapper.convertValue(importedAPIConfig.get("tags"), new TypeReference<TagMap<String, String[]>>(){});
		TagMap<String, String[]> exportedTags = mapper.convertValue(exportedAPIConfig.get("tags"), new TypeReference<TagMap<String, String[]>>(){});
		assertEquals(importedTags, exportedTags, "Tags are not equal.");
		
		List<CorsProfile> importedCorsProfiles = mapper.convertValue(importedAPIConfig.get("corsProfiles"), new TypeReference<List<CorsProfile>>(){});
		List<CorsProfile> exportedCorsProfiles = mapper.convertValue(exportedAPIConfig.get("corsProfiles"), new TypeReference<List<CorsProfile>>(){});
		assertEquals(importedCorsProfiles, exportedCorsProfiles, "CorsProfiles are not equal.");

		if(!ignoreAdminAccount) {
			APIQuota importedAppQuota = mapper.convertValue(importedAPIConfig.get("applicationQuota"), new TypeReference<APIQuota>(){});
			APIQuota exportedAppQuota = mapper.convertValue(exportedAPIConfig.get("applicationQuota"), new TypeReference<APIQuota>(){});
			assertEquals(importedAppQuota, exportedAppQuota, "applicationQuota are not equal.");
			
			APIQuota importedSystemQuota = mapper.convertValue(importedAPIConfig.get("systemQuota"), new TypeReference<APIQuota>(){});
			APIQuota exportedSystemQuota = mapper.convertValue(exportedAPIConfig.get("systemQuota"), new TypeReference<APIQuota>(){});
			assertEquals(importedSystemQuota, exportedSystemQuota, "systemQuota are not equal.");
		}

		assertEquals(exportedAPIConfig.get("caCerts").size(), 				4);
		
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("certFile").asText(), 				"swagger.io.crt");
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("inbound").asBoolean(), 			false);
		assertEquals(exportedAPIConfig.get("caCerts").get(0).get("outbound").asBoolean(), 			true);
		
		assertEquals(exportedAPIConfig.get("caCerts").get(3).get("certFile").asText(), 				"GlobalSign.crt");
		assertEquals(exportedAPIConfig.get("caCerts").get(3).get("inbound").asBoolean(), 			true);
		assertEquals(exportedAPIConfig.get("caCerts").get(3).get("outbound").asBoolean(), 			false);
		
		assertTrue(new File(context.getVariable("exportLocation")+"/api.custom-host.com/"+context.getVariable("exportFolder")+"/swagger.io.crt").exists(), "Certificate swagger.io.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/api.custom-host.com/"+context.getVariable("exportFolder")+"/GoDaddySecureCertificateAuthority-G2.crt").exists(), "Certificate GoDaddySecureCertificateAuthority-G2.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/api.custom-host.com/"+context.getVariable("exportFolder")+"/GoDaddyRootCertificateAuthority-G2.crt").exists(), "Certificate GoDaddyRootCertificateAuthority-G2.crt is missing");
		assertTrue(new File(context.getVariable("exportLocation")+"/api.custom-host.com/"+context.getVariable("exportFolder")+"/GlobalSign.crt").exists(), "Certificate GlobalSign.crt is missing");
		
		assertTrue(new File(context.getVariable("exportLocation")+"/api.custom-host.com/"+context.getVariable("exportFolder")+"/"+context.getVariable("exportAPIName")).exists(), "Exported Swagger-File is missing");		
	}
}
