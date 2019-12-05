package com.cloud.build.cloud.build;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.cloudbuild.v1.CloudBuild;
import com.google.api.services.cloudbuild.v1.model.*;
import com.google.api.services.storage.Storage;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.GeneralSecurityException;
import java.util.*;

@SpringBootApplication
public class Application {
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
	private static final String APPLICATION_NAME = "CloudBuild";
	private static HttpTransport httpTransport;
	// your google service account that accesses your API
	private static final String SERVICE_ACCOUNT_ID = "962935543753-compute@developer.gserviceaccount.com";
	private static final String[] SCOPES = {"https://www.googleapis.com/auth/cloud-platform"};
	// generate p12 from google cloud developer console, credentials, create credential of you google service account and select P12 file.
	private static final String P12_FILE_LOCATION = "src/main/resources/cloudserviceacc.p12";
	private static Credential credential;
	// google api project name
	private static final String PROJECT_ID = "testprojectdocker";

	static{
		try {
			httpTransport = GoogleNetHttpTransport.newTrustedTransport();
			credential = new GoogleCredential.Builder()
					.setTransport(httpTransport)
					.setJsonFactory(JSON_FACTORY)
					.setServiceAccountId(SERVICE_ACCOUNT_ID)
					.setServiceAccountScopes(Arrays.asList(SCOPES))
					.setServiceAccountPrivateKeyFromP12File( new java.io.File(System.getProperty("user.dir"),P12_FILE_LOCATION))
					.build();
		} catch (GeneralSecurityException | IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws URISyntaxException, Exception {

		Build build = new Build();
		// create cmd to build docker image using BuildStep
		BuildStep buildDockerImageStep  = new BuildStep();

		// access docker engine from cloud builder
		buildDockerImageStep.setName("gcr.io/cloud-builders/docker");
		// build docker image
		List<String> buildDockerImageCmd = Arrays.asList("build","--tag","gcr.io/testprojectdocker/testproject2", ".");
		buildDockerImageStep.setArgs(buildDockerImageCmd);

		//add buildDockerImageStep to build steps
		List<BuildStep> buildImageStep = new ArrayList<>();
		buildImageStep.add(buildDockerImageStep);
		build.setSteps(buildImageStep);
		// set the source from where to build the project
		Source source = new Source();
		RepoSource repoSource = new RepoSource();
		repoSource.setBranchName("master");
		repoSource.setProjectId("testprojectdocker");
		repoSource.setRepoName("javapipeline");
		source.setRepoSource(repoSource);
		build.setSource(source);


		//push docker image to Container repository
		List<String> images = Collections.singletonList("gcr.io/testprojectdocker/testproject2");
		build.setImages(images);


		// create cloud build instance to execute request to the google cloud build
		CloudBuild cloudBuild = createCloudBuilder();

		// get the builds from response
		ListBuildsResponse buildsResponse = cloudBuild.projects().builds().list(PROJECT_ID).execute();
		// get the list of builds
		List<Build> buildsList = buildsResponse.getBuilds();
		//execute build post build docker image and push it to testproject2 container register
		cloudBuild.projects().builds().create(PROJECT_ID,build).execute();


		for(Build b: buildsList){
			System.out.println(b.getCreateTime());
		}
		SpringApplication.run(Application.class, args);
	}

	//create cloud builder with the credentials
	private static CloudBuild createCloudBuilder() throws Exception {
		return new CloudBuild.Builder(httpTransport,JSON_FACTORY,credential).setApplicationName(APPLICATION_NAME).build();
	}

}
