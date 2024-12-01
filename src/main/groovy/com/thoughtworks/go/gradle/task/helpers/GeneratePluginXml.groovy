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

import com.thoughtworks.go.gradle.task.helpers.ReleaseTaskHelper.GoCDPluginExtension
import org.gradle.api.Project

class GeneratePluginXml {
  void apply(Project project) {
    project.afterEvaluate {
      (project.gocdPlugin as GoCDPluginExtension).pluginProject.with {
        def generatedResourcesOutput = it.file 'src/main/resources-generated'

        sourceSets {
          main {
            resources {
              output.dir generatedResourcesOutput, builtBy: 'generateResources'
              srcDirs += generatedResourcesOutput
            }
          }
          test {
            java {
              runtimeClasspath += configurations.compileClasspath
            }
          }
        }

        tasks.register('generateResources') {
          outputs.upToDateWhen { false }
          outputs.dir generatedResourcesOutput

          doFirst {
            delete generatedResourcesOutput
            generatedResourcesOutput.mkdirs()

            new File(generatedResourcesOutput, 'plugin.xml').setText(xmlTemplate(project), "utf-8")
            new File(generatedResourcesOutput, 'plugin.properties').setText(pluginProperties(project), "utf-8")
          }
        }

        tasks.named('processResources') {
          dependsOn 'generateResources'
        }
      }
    }
  }

  private static String xmlTemplate(Project project) {
    GoCDPluginExtension gocdPlugin = project.gocdPlugin
    def targetOsSnippet = ''

    if (gocdPlugin.targetOs) {
      String values = gocdPlugin.targetOs
        .collect({ eachTargetOs -> "<value>${eachTargetOs}</value>" })
        .join("\n${' ' * 10}")

      targetOsSnippet = """\
        <target-os>
          ${values}
        </target-os>""".stripIndent()
    }

    return """\
      <!--
        ~ Copyright ${java.time.Year.now().getValue()} ThoughtWorks, Inc.
        ~
        ~ Licensed under the Apache License, Version 2.0 (the "License");
        ~ you may not use this file except in compliance with the License.
        ~ You may obtain a copy of the License at
        ~
        ~     http://www.apache.org/licenses/LICENSE-2.0
        ~
        ~ Unless required by applicable law or agreed to in writing, software
        ~ distributed under the License is distributed on an "AS IS" BASIS,
        ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        ~ See the License for the specific language governing permissions and
        ~ limitations under the License.
        -->
  
      <go-plugin id="${gocdPlugin.id}" version="1">
        <about>
          <name>${gocdPlugin.name}</name>
          <version>${project.version}</version>
          <target-go-version>${gocdPlugin.goCdVersion}</target-go-version>
          <description>${gocdPlugin.description}</description>
          <vendor>
            <name>${gocdPlugin.vendorName}</name>
            <url>${gocdPlugin.vendorUrl}</url>
          </vendor>
          ${targetOsSnippet}
        </about>
      </go-plugin>
    """.stripIndent()
  }

  private static String pluginProperties(Project project) {
    GoCDPluginExtension gocdPlugin = project.gocdPlugin

    return """\
      #
      # Copyright ${java.time.Year.now().getValue()} ThoughtWorks, Inc.
      #
      # Licensed under the Apache License, Version 2.0 (the "License");
      # you may not use this file except in compliance with the License.
      # You may obtain a copy of the License at
      #
      #     http://www.apache.org/licenses/LICENSE-2.0
      #
      # Unless required by applicable law or agreed to in writing, software
      # distributed under the License is distributed on an "AS IS" BASIS,
      # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
      # See the License for the specific language governing permissions and
      # limitations under the License.
      #
  
      id=${gocdPlugin.id}
      name=${gocdPlugin.name}
      version=${project.version}
      goCdVersion=${gocdPlugin.goCdVersion}
      description=${gocdPlugin.description}
      vendorName=${gocdPlugin.vendorName}
      vendorUrl=${gocdPlugin.vendorUrl}
    """.stripIndent()
  }
}
