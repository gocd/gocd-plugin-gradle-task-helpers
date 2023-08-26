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

import org.gradle.api.GradleScriptException
import org.gradle.api.Project

import java.text.DecimalFormat

class VersionHelper {
  Project project

  void apply(Project project) {
    this.project = project

    project.with {
      allprojects {
        ext.git = [
          gitRevision: this.&gitRevision,
          distVersion: this.&distVersion,
          getLastTag: this.&getLastTag,
          getCommitsSinceLastTag: this.&getCommitsSinceLastTag,
          readableFileSize: this.&readableFileSize,
        ]
      }
    }
  }

  static String gitRevision() {
    def process = 'git log -n 1 --format=%H'.execute()
    process.waitFor()
    return process.text.stripIndent().trim()
  }

  static String distVersion() {
    def process = 'git rev-list HEAD --count'.execute()
    process.waitFor()
    return process.text.stripIndent().trim()
  }

  static String getLastTag(boolean isExperimental) {
    try {
      def command = ['git', 'describe', '--tags'] +
        (!isExperimental ? ['--match=*[^-exp]'] : []) +
        ['--abbrev=0']

      def process = command.join(' ').execute()
      process.waitFor()
      return process.text.trim()
    } catch (Exception ignored) {
      return null
    }
  }

  String getCommitsSinceLastTag(String from) {
    def hashStdOut = new ByteArrayOutputStream()
    def hashErrOut = new ByteArrayOutputStream()

    try {
      project.exec {
        commandLine ['git', 'log', '--no-merges', '--pretty="%h - %s"'] +
          (from ? ["${from}..HEAD"] : [])
        standardOutput = hashStdOut
        errorOutput = hashErrOut
      }
    } catch (Exception e) {
      throw new GradleScriptException(hashErrOut.toString(), e)
    }

    return hashStdOut.toString().trim()
  }

  static String readableFileSize(long size) {
    if (size <= 0) return '0'
    final String[] units = ['B', 'KB', 'MB', 'GB', 'TB']
    int digitGroups = (int) (Math.log10(size) / Math.log10(1024))
    return new DecimalFormat('#,##0.#').format(size / Math.pow(1024, digitGroups)) + ' ' + units[digitGroups]
  }
}
