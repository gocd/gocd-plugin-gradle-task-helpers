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
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Jar

class ArchiveTaskHelper {
  void apply(Project project) {
    project.with {
      allprojects {
        afterEvaluate {
          // generate checksums for all archives
          tasks.withType(AbstractArchiveTask).configureEach {
            includeEmptyDirs = false
            duplicatesStrategy = DuplicatesStrategy.EXCLUDE
            preserveFileTimestamps = false
            reproducibleFileOrder = true

            ['MD5', 'SHA1', 'SHA-256'].each { algo ->
              outputs.files "${archiveFile.get()}.${algo}"
              doLast {
                ant.checksum file: archiveFile.get() as String, format: 'MD5SUM', algorithm: algo
              }
            }
          }

          // add relevant metadata
          tasks.withType(Jar).configureEach {
            doFirst {
              manifest {
                attributes(
                  'Go-Version': rootProject.extensions.gocdPlugin.goCdVersion,
                  'Plugin-Revision': rootProject.extensions.gocdPlugin.pluginVersion,
                  'Implementation-Title': project.name,
                  'Implementation-Version': project.version,
                  'Source-Compatibility': project.sourceCompatibility,
                  'Target-Compatibility': project.targetCompatibility,
                  'Git-SHA': rootProject.git.gitRevision(),
                )
              }
            }
          }
        }
      }
    }
  }
}
