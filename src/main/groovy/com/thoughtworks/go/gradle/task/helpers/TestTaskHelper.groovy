/*
 * Copyright 2019-2023 ThoughtWorks, Inc.
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

package com.thoughtworks.go.gradle.task.helpers

import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.TestResult

class TestTaskHelper {
  void apply(Project project) {
    project.with {
      allprojects {
        tasks.withType(Test).configureEach {
          maxParallelForks = 1

          testLogging {
            def previousFailed = false
            exceptionFormat 'full'

            beforeSuite { suite ->
              if (suite.name.startsWith('Test Run') || suite.name.startsWith('Gradle Worker')) return

              if (suite.parent && suite.className) {
                println(TestHelperANSI.COLOR.ANSI_BOLD_WHITE + suite.name + TestHelperANSI.COLOR.ANSI_RESET)
              }
            }

            beforeTest {
              if (previousFailed) {
                System.err.println('')
              }
            }

            afterTest { descriptor, result ->
              previousFailed = false
              def executionTime = (result.endTime - result.startTime) / 1000
              println("    ${resultIndicator(result)}$TestHelperANSI.COLOR.ANSI_RESET $descriptor.name " +
                "$TestHelperANSI.COLOR.ANSI_YELLOW($executionTime seconds)$TestHelperANSI.COLOR.ANSI_RESET")

              if (result.failedTestCount > 0) {
                previousFailed = true
                println('')
                println(result.exception)
              }
            }

            afterSuite { desc, result ->
              if (desc.parent && desc.className) {
                println('')
              }

              if (!desc.parent) { // will match the outermost suite
                def summaryStyle = summaryStyle(result)

                println("--------------------------------------------------------------------------")
                println("Results: $summaryStyle$result.resultType$TestHelperANSI.COLOR.ANSI_RESET " +
                  "($result.testCount tests, $TestHelperANSI.COLOR.ANSI_GREEN$result.successfulTestCount passed" +
                  "$TestHelperANSI.COLOR.ANSI_RESET, $TestHelperANSI.COLOR.ANSI_RED$result.failedTestCount failed" +
                  "$TestHelperANSI.COLOR.ANSI_RESET, $TestHelperANSI.COLOR.ANSI_YELLOW$result.skippedTestCount skipped" +
                  "$TestHelperANSI.COLOR.ANSI_RESET)")
                println("--------------------------------------------------------------------------")
              }
            }
          }
        }
      }
    }
  }

  private static String summaryStyle(result) {
    def summaryStyle = TestHelperANSI.COLOR.ANSI_WHITE
    switch (result.resultType) {
      case TestResult.ResultType.SUCCESS:
        summaryStyle = TestHelperANSI.COLOR.ANSI_GREEN
        break
      case TestResult.ResultType.FAILURE:
        summaryStyle = TestHelperANSI.COLOR.ANSI_RED
        break
    }
    summaryStyle
  }

  private static String resultIndicator(result) {
    def indicator

    if (result.failedTestCount > 0) {
      indicator = TestHelperANSI.COLOR.ANSI_RED + TestHelperANSI.SYMBOLS.X_MARK
    } else if (result.skippedTestCount > 0) {
      indicator = TestHelperANSI.COLOR.ANSI_YELLOW + TestHelperANSI.SYMBOLS.NEUTRAL_FACE
    } else {
      indicator = TestHelperANSI.COLOR.ANSI_GREEN + TestHelperANSI.SYMBOLS.CHECK_MARK
    }
    indicator
  }

  static class TestHelperANSI {
    static Map<String, String> COLOR = [
      ANSI_BOLD_WHITE: "\u001B[0;1m",
      ANSI_RESET     : "\u001B[0m",
      ANSI_RED       : "\u001B[31m",
      ANSI_GREEN     : "\u001B[32m",
      ANSI_YELLOW    : "\u001B[33m",
      ANSI_WHITE     : "\u001B[37m"
    ]

    static Map<String, String> SYMBOLS = [
      CHECK_MARK  : "\u2714",
      NEUTRAL_FACE: "\u0CA0_\u0CA0",
      X_MARK      : "\u2718"
    ]
  }
}
