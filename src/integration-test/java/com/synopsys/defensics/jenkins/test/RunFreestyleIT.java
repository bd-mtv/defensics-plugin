/*
 * Copyright © 2020 Synopsys, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.synopsys.defensics.jenkins.test;

import static java.lang.Thread.sleep;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

import com.cloudbees.plugins.credentials.CredentialsProvider;
import com.cloudbees.plugins.credentials.CredentialsScope;
import com.cloudbees.plugins.credentials.CredentialsStore;
import com.cloudbees.plugins.credentials.domains.Domain;
import com.synopsys.defensics.api.ApiService;
import com.synopsys.defensics.apiserver.model.RunState;
import com.synopsys.defensics.jenkins.result.HtmlReportPublisherTarget.HtmlReportAction;
import com.synopsys.defensics.jenkins.test.utils.DefensicsMockServer;
import com.synopsys.defensics.jenkins.test.utils.ProjectUtils;
import htmlpublisher.HtmlPublisherTarget.HTMLAction;
import hudson.EnvVars;
import hudson.model.FreeStyleBuild;
import hudson.model.FreeStyleProject;
import hudson.model.Result;
import hudson.model.queue.QueueTaskFuture;
import hudson.slaves.EnvironmentVariablesNodeProperty;
import hudson.util.Secret;
import org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.jvnet.hudson.test.JenkinsRule;
import org.mockserver.integration.ClientAndServer;

public class RunFreestyleIT {

  static final String NAME = "My Defensics";
  static final String URL = "http://localhost:1080/";
  static final String TESTPLAN_NAME = "http.testplan";
  private static final boolean CERTIFICATE_VALIDATION_DISABLED = false;
  private static final String CREDENTIAL_ID = "test-credential";
  private static final String AUTH_TOKEN = "test-token";
  @Rule
  public JenkinsRule jenkinsRule = new JenkinsRule();

  private ApiService api;
  private FreeStyleProject project;
  private ClientAndServer mockServer;

  @Before
  public void setup() throws Exception {

    api = new ApiService(URL, AUTH_TOKEN, CERTIFICATE_VALIDATION_DISABLED);

    project = jenkinsRule.createFreeStyleProject();
    CredentialsStore store = CredentialsProvider.lookupStores(jenkinsRule.jenkins)
        .iterator()
        .next();
    StringCredentialsImpl credential = new StringCredentialsImpl(
        CredentialsScope.GLOBAL,
        CREDENTIAL_ID,
        "Test Secret Text",
        Secret.fromString(AUTH_TOKEN));
    store.addCredentials(Domain.global(), credential);

    EnvironmentVariablesNodeProperty prop = new EnvironmentVariablesNodeProperty();
    EnvVars env = prop.getEnvVars();
    env.put("DEFENSICS_MAX_POLLING_INTERVAL", "1");
    jenkinsRule.jenkins.getGlobalNodeProperties().add(prop);
  }

  @After
  public void stopServer() {
    mockServer.stop();
  }

  @Test
  public void testRunBuildStep() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    mockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        TESTPLAN_NAME);
    ProjectUtils.addBuildStep(project, NAME, TESTPLAN_NAME);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }

  @Test
  public void testRunPostBuildStep() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    mockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        TESTPLAN_NAME);
    ProjectUtils.addPostBuildStep(project, NAME, TESTPLAN_NAME);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }

  @Test
  public void testConfigurationRoundTripAndRun() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    mockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        TESTPLAN_NAME);
    ProjectUtils.addBuildStep(project, NAME, TESTPLAN_NAME);

    jenkinsRule.configRoundtrip(project);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.SUCCESS)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }

  @Test
  public void testAbortJob() throws Exception {
    // Create and use new client to prevent Job from completing.
    // client = ApiUtils.getMockClient(JobState.RUNNING, 0);
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "PASS", RunState.COMPLETED);
    mockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        TESTPLAN_NAME);
    ProjectUtils.addBuildStep(project, NAME, TESTPLAN_NAME);

    // Schedule build
    QueueTaskFuture<FreeStyleBuild> runFuture = project.scheduleBuild2(0);

    // Wait for job to start properly, then interrupt build.
    sleep(2000);
    runFuture.cancel(true);

    FreeStyleBuild run = runFuture.get();

    assertThat(run.getResult(), is(equalTo(Result.ABORTED)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
  }

  @Test
  public void testJobFailed() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "PASS", RunState.ERROR);
    mockServer.initServer(this.mockServer);
    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        TESTPLAN_NAME);
    ProjectUtils.addBuildStep(project, NAME, TESTPLAN_NAME);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(0)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class),
        is(nullValue()));
  }

  @Test
  public void testJobFailure() throws Exception {
    mockServer = ClientAndServer.startClientAndServer(1080);
    DefensicsMockServer mockServer = new DefensicsMockServer(true, "FAIL", RunState.COMPLETED);
    mockServer.initServer(this.mockServer);

    ProjectUtils.setupProject(
        jenkinsRule,
        project,
        NAME,
        URL,
        CERTIFICATE_VALIDATION_DISABLED,
        CREDENTIAL_ID,
        TESTPLAN_NAME);
    ProjectUtils.addBuildStep(project, NAME, TESTPLAN_NAME);

    FreeStyleBuild run = project.scheduleBuild2(0).get();

    assertThat(run.getResult(), is(equalTo(Result.FAILURE)));
    assertThat(run.getActions(HtmlReportAction.class).size(), is(equalTo(1)));
    assertThat(run.getActions(HTMLAction.class).size(), is(equalTo(0)));
    assertThat(project.getAction(HtmlReportAction.class).getUrlName(),
        is(equalTo(run.getActions(HtmlReportAction.class).get(0).getUrlName())));
  }


}
