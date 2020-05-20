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

package com.synopsys.defensics.jenkins.configuration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import hudson.util.FormValidation.Kind;
import org.junit.Before;
import org.junit.Test;

public class StepConfigurationValidatorTest {

  StepConfigurationValidator validator;

  @Before
  public void setup() {
    validator = new StepConfigurationValidator();
  }

  @Test
  public void testCreation() {
    assertThat(validator, is(not(nullValue())));
  }

  @Test
  public void testValidateSettingFilePath() {
    assertThat(validator.validateSettingFilePath("/some/path/file.set").kind, is(equalTo(Kind.OK)));
    assertThat(validator.validateSettingFilePath("/test.testplan").kind, is(equalTo(Kind.OK)));
  }

  @Test
  public void testValidateSettingFilePathEmptyPath() {
    assertThat(validator.validateSettingFilePath("").kind, is(equalTo(Kind.ERROR)));
  }

  @Test
  public void testValidateSettingFilePathNoExtension() {
    assertThat(validator.validateSettingFilePath("/").kind, is(equalTo(Kind.ERROR)));
  }

  @Test
  public void testValidateSettingFilePathWrongExtension() {
    assertThat(validator.validateSettingFilePath("file.txt").kind, is(equalTo(Kind.ERROR)));
  }

  @Test
  public void testValidateConfigurationOverrides() {
    validateConfigurationOverrides("", Kind.OK);
    validateConfigurationOverrides("--uri http://127.0.0.1", Kind.OK);
    validateConfigurationOverrides("--test --setting", Kind.OK);
    validateConfigurationOverrides("--max-run-cases 40", Kind.OK);
    validateConfigurationOverrides("--index 15-40 --max-run-cases 15", Kind.OK);
    validateConfigurationOverrides("--index 15-40 --max-run-cases 15 test setting", Kind.OK);
    validateConfigurationOverrides("--suite-setting \"aa aa\"", Kind.OK);
    validateConfigurationOverrides("--exec-instrument \"echo \\\"bar baz\\\"\"\n", Kind.OK);
  }

  @Test
  public void testValidateConfigurationOverridesInvalidFormat() {
    validateConfigurationOverrides("something fishy", Kind.ERROR);
    validateConfigurationOverrides("-almost correct", Kind.ERROR);
  }

  private void validateConfigurationOverrides(String override, Kind expectedResult) {
    assertThat(validator.validateConfigurationOverrides(override).kind,
        is(equalTo(expectedResult)));
  }
}
