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
import org.gradle.api.tasks.compile.JavaCompile

class CompileTaskHelper {
  void apply(Project project) {
    project.with {
      allprojects {
        tasks.withType(JavaCompile).configureEach {
          options.with {
            deprecation = true
            debug = true
            warnings = false
            encoding = 'UTF-8'
            compilerArgs += '-Xlint:-deprecation'
          }
        }
      }
    }
  }
}
