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

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class GocdPluginGradleTaskHelpersPluginFunctionalTest extends Specification {
  @TempDir
  private File projectDir

  def setup() {
    new File(projectDir, 'build.gradle') << '''\
      plugins {
          id 'com.thoughtworks.go.gradle.task.helpers'
          id 'java-gradle-plugin'
          id 'groovy'
      }

      repositories {
        mavenCentral()
      }

      gradlePlugin {
          plugins {
              greeting {
                  id = 'test.project.greeting'
                  implementationClass = 'test.project.TestProjectPlugin'
              }
          }
      }

      gocdPlugin {
          id = 'test.project.greeting'
          pluginVersion = '1.2.3'
          goCdVersion = '19.3.0'
          name = 'Greeter plugin'
          description = 'Test plugin'
          vendorName = 'ThoughtWorks, Inc.'
          vendorUrl = 'https://github.com/example/greeter-plugin'

          githubRepo {
              owner = System.getenv('GITHUB_USER') ?: 'bob'
              repo = 'greeter-plugin'
              token = System.getenv('GITHUB_TOKEN') ?: 'bad-token'
          }

          pluginProject = project
          assetsToRelease = [jar]
      }

      version = gocdPlugin.fullVersion(project)
    '''.stripIndent()

    new File(projectDir, 'settings.gradle') << ''

    new File(projectDir, 'src/main/groovy/test/project').mkdirs()
    new File(projectDir, 'src/main/groovy/test/project/TestProjectPlugin.groovy') << '''\
      package test.project

      import org.gradle.api.Project
      import org.gradle.api.Plugin

      class TestProjectPlugin implements Plugin<Project> {
          void apply(Project project) {
              // Register a task
              project.tasks.register("greeting") {
                  doLast {
                      println("Hello from plugin 'test.project.greeting'")
                  }
              }
          }
      }
    '''
  }

  def 'can run task "allDependencies"'() {
    when:
    def result = GradleRunner.create()
      .forwardOutput()
      .withPluginClasspath()
      .withArguments('allDependencies', '--stacktrace')
      .withProjectDir(projectDir)
      .build()

    then:
    result.task(':allDependencies').outcome == SUCCESS
  }

  def 'can print build info'() {
    when:
    def result = GradleRunner.create()
      .forwardOutput()
      .withPluginClasspath()
      .withArguments('--stacktrace')
      .withEnvironment(
        *:System.env,
        'GO_SERVER_URL': 'https://example.site/go'
      )
      .withProjectDir(projectDir)
      .build()

    then:
    result.output.contains '========================================================================'
    result.output.contains 'Gradle version:'
  }

  def 'can print build debug info'() {
    when:
    def result = GradleRunner.create()
      .forwardOutput()
      .withPluginClasspath()
      .withArguments('--stacktrace')
      .withEnvironment(
        *:System.env,
        'GO_SERVER_URL': 'https://example.site/go',
        'DUMP_SYSTEM_ENV': 'true'
      )
      .withProjectDir(projectDir)
      .build()

    then:
    result.output.contains 'JVM properties:'
  }
}
