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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.HelpTasksPlugin

class GocdPluginGradleTaskHelpersPlugin implements Plugin<Project> {
  void apply(Project project) {
    project.with {
      if (System.env.containsKey('GO_SERVER_URL')) {
        def separator = '=' * 72

        println separator
        println "Gradle version:  ${gradle.gradleVersion}"
        println "JVM:             ${System.properties.'java.version'} (${System.properties.'java.vm.vendor'} ${System.properties.'java.vm.version'})"
        println "OS:              ${System.properties.'os.name'} ${System.properties.'os.version'} ${System.properties.'os.arch'}"
        println separator
        println ''

        if (System.env.containsKey('DUMP_SYSTEM_ENV')) {
          println "       JVM properties: ${System.properties}";
          println "Environment Variables: ${System.env}";
          println separator
          println ''
        }
      }

      subprojects {
        apply plugin: 'java'

        sourceCompatibility = 11
        targetCompatibility = 11
      }

      tasks.register('allDependencies') {
        group = HelpTasksPlugin.HELP_GROUP
        description = 'Print dependency tree of all projects'

        dependsOn allprojects.collect { "${it.path}:dependencies" }
      }

      new VersionHelper().apply project
      new DependencyUpdates().apply project
      new GeneratePluginXml().apply project
      new ArchiveTaskHelper().apply project
      new ReleaseTaskHelper().apply project
      new TestTaskHelper().apply project
      new CompileTaskHelper().apply project
    }
  }
}
