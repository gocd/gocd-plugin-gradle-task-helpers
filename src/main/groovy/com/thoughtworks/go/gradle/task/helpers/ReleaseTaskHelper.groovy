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

import com.github.jk1.license.LicenseReportPlugin
import com.github.jk1.license.filter.LicenseBundleNormalizer
import com.github.jk1.license.render.TextReportRenderer
import groovy.json.JsonSlurper
import groovyx.net.http.ContentType
import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import groovyx.net.http.ParserRegistry
import org.apache.http.HttpResponse
import org.gradle.api.*
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.jvm.tasks.Jar
import org.gradle.language.assembler.tasks.Assemble

import javax.inject.Inject

class ReleaseTaskHelper {
  void apply(Project project) {
    project.with {
      rootProject.ext.defaultAllowedLicenses = writeTempFile(project, 'default-allowed-licenses.json')
      rootProject.ext.licenseNormalizerBundle = writeTempFile(project, 'license-normalizer-bundle.json')

      apply plugin: GoCDPlugin
    }
  }

  static class GitHubRepository {
    String owner
    String repo

    String token
  }

  static class Report {
    String outputDir
    List<Project> projects
    List<String> configurations = ['runtimeClasspath']
    List<String> excludeGroups = []
    List<String> excludes = []
    boolean excludeOwnGroup = true
    boolean excludeBoms = true
    def importers = []
  }

  static class GoCDPluginExtension {
    String id
    String pluginVersion
    String goCdVersion
    String name
    String description
    String vendorName
    String vendorUrl
    List<String> targetOs = []

    boolean prerelease

    Project pluginProject
    Task[] assetsToRelease

    final GitHubRepository githubRepo
    final Report licenseReport

    @Inject
    GoCDPluginExtension(ObjectFactory objectFactory) {
      githubRepo = objectFactory.newInstance(GitHubRepository)
      licenseReport = objectFactory.newInstance(Report)
    }

    String fullVersion(Project project) {
      project.git.distVersion() ? "${pluginVersion}-${project.git.distVersion()}" : pluginVersion
    }

    void githubRepo(Action<? super GitHubRepository> action) {
      action.execute(githubRepo)
    }

    void licenseReport(Action<? super Report> action) {
      action.execute(licenseReport)
    }
  }

  static abstract class ReleaseHelperTask extends DefaultTask {
    @Internal
    final String HEADER_USER_AGENT = 'gocd-gradle-github-release-plugin'
    @Internal
    final String GITHUB_API_BASE_URL = 'https://api.github.com'
    @Internal
    final String GITHUB_API_ACCEPT_HEADER = 'application/vnd.github.v3+json'

    protected static HTTPBuilder createHttpBuilder(url) {
      def http = new HTTPBuilder(url)

      http.parser['text/json'] = { HttpResponse resp ->
        return new JsonSlurper().parseText(
          resp.entity.content.getText(ParserRegistry.getCharset(resp)).trim()
        )
      }

      http.parser['application/json'] = http.parser['text/json']

      return http
    }
  }

  static class VerifyExpRelease extends ReleaseHelperTask {
    @TaskAction
    verify() {
      GoCDPluginExtension gocdPlugin = project.gocdPlugin
      def tagName = "v${gocdPlugin.fullVersion(project)}-exp".toString()
      def path = "/repos/${gocdPlugin.githubRepo.owner}/${gocdPlugin.githubRepo.repo}/releases/tags/${tagName}"

      def http = createHttpBuilder(GITHUB_API_BASE_URL)
      http.request(Method.GET) {
        uri.path += path
        requestContentType = ContentType.JSON

        headers['User-Agent'] = HEADER_USER_AGENT
        headers['Authorization'] = "token ${gocdPlugin.githubRepo.token}"
        headers['Accept'] = GITHUB_API_ACCEPT_HEADER

        def postLogMessage = """\
          GET ${uri.path}
           > User-Agent: ${headers['User-Agent']}
           > Authorization: (not shown)
           > Accept: ${headers.Accept}
        """.stripIndent()
        logger.debug "$postLogMessage"

        response.success = { resp, json ->
          logger.debug "< $resp.statusLine"
          logger.debug 'Response headers: \n' + resp.headers.collect { "< $it" }.join('\n')
          println 'Experimental release is present!!!'
        }

        response.failure = { resp, json ->
          logger.error "Error in $postLogMessage"
          logger.debug 'Response headers: \n' + resp.headers.collect { "< $it" }.join('\n')
          def errorMessage = json ? json.message : resp.statusLine
          def ref = json ? "See $json.documentation_url" : ''
          def errorDetails = json?.errors ? "Details: " + json.errors.collect { it }.join('\n') : ''
          throw new GradleScriptException("Error while fetching experimental release: $errorMessage. $ref. $errorDetails", null)
        }
      }
    }
  }

  static class ReleaseTask extends ReleaseHelperTask {
    @TaskAction
    release() {
      GoCDPluginExtension gocdPlugin = project.gocdPlugin

      def sha256Sums = gocdPlugin.assetsToRelease*.outputs*.files*.files.flatten()
        .findAll { File it -> it.name.matches(".*\\.(SHA-256|SHA256)") }

      def path = "/repos/${gocdPlugin.githubRepo.owner}/${gocdPlugin.githubRepo.repo}/releases"

      def lastCommit = project.git.gitRevision()
      def lastTag = project.git.getLastTag(gocdPlugin.prerelease)
      def changelogHeader = lastTag ? "### Changelog ${lastTag}..${lastCommit.substring(0, 7)}" : "### Changelog"
      def changeLog = project.git.getCommitsSinceLastTag(lastTag).replaceAll("\"", "")
      def supportedGoCDVersionNote = "**Note:** *Supported GoCD server version: ${gocdPlugin.goCdVersion} or above.*"
      def checksumNote = """**SHA256 Checksums**\n```\n${
        sha256Sums.collect({ File eachFile -> eachFile.getText("utf-8").trim() }).join("\n")
      }\n```""".trim()
      def tagName = gocdPlugin.prerelease ? "${gocdPlugin.fullVersion(project)}-exp" : gocdPlugin.fullVersion(project)


      def postBody = [
        tag_name        : "v${tagName}" as String,
        target_commitish: project.git.gitRevision() as String,
        name            : "${gocdPlugin.prerelease ? 'Experimental: ' : ''}${gocdPlugin.fullVersion(project)}" as String,
        body            : """\n$changelogHeader\n\n${changeLog}\n\n${
          supportedGoCDVersionNote
          checksumNote
        }""".stripIndent().trim() as String,
        prerelease      : gocdPlugin.prerelease
      ]

      def http = createHttpBuilder(GITHUB_API_BASE_URL)
      http.request(Method.POST) {
        uri.path += path
        requestContentType = ContentType.JSON
        body = postBody

        headers['User-Agent'] = HEADER_USER_AGENT
        headers['Authorization'] = "token ${gocdPlugin.githubRepo.token}"
        headers['Accept'] = GITHUB_API_ACCEPT_HEADER

        def postLogMessage = "POST ${uri.path}\n" +
          " > User-Agent: ${headers['User-Agent']}\n" +
          ' > Authorization: (not shown)\n' +
          " > Accept: ${headers.Accept}\n" +
          " > body: $body\n"
        logger.debug "$postLogMessage"

        response.success = { resp, json ->
          logger.debug "< $resp.statusLine"
          logger.debug 'Response headers: \n' + resp.headers.collect { "< $it" }.join('\n')
          if (gocdPlugin.assetsToRelease != null) {
            postAssets(json.upload_url)
          }
        }

        response.failure = { resp, json ->
          logger.error "Error in $postLogMessage"
          logger.debug 'Response headers: \n' + resp.headers.collect { "< $it" }.join('\n')
          def errorMessage = json ? json?.message : resp.statusLine
          def ref = json ? "See $json.documentation_url" : ''
          def errorDetails = json?.errors ? 'Details: ' + json.errors.collect { it }.join('\n') : ''
          throw new GradleScriptException("$errorMessage. $ref. $errorDetails", null)
        }
      }
    }

    def postAssets(String uploadUrl) {
      GoCDPluginExtension gocdPlugin = project.gocdPlugin

      gocdPlugin.assetsToRelease.each { Task assetTask ->
        assetTask.outputs.files.forEach { File file ->
          def name = file.name

          def upload = uploadUrl.replace('{?name,label}', "?name=${name}&label=${name}")
          logger.debug "upload url: ${upload}"

          if (!file.exists()) {
            return
          }

          def url = new URL(upload as String)
          def host = "${url.protocol}://" + (url.port > 0 ? ':' + url.port + '' : '')
          def path = url.path
          def http = createHttpBuilder(host)

          def map = URLConnection.getFileNameMap()
          def contentType = map.getContentTypeFor(file.absolutePath)

          http.ignoreSSLIssues()
          http.request(Method.POST) { req ->
            uri.path = path
            uri.query = [
              name: name,
            ]
            send ContentType.BINARY, file.bytes

            headers['User-Agent'] = HEADER_USER_AGENT
            headers['Authorization'] = "token ${gocdPlugin.githubRepo.token}"
            headers['Accept'] = GITHUB_API_ACCEPT_HEADER
            headers['Content-Type'] = contentType


            response.success = { resp, json ->
              logger.debug "$json"
            }

            response.failure = { resp, json ->
              logger.error "$json"
            }
          }
        }
      }
    }
  }

  static class GoCDPlugin implements Plugin<Project> {
    void apply(Project project) {
      project.with {
        allprojects {
          apply plugin: LicenseReportPlugin
        }

        def gocdPluginExtension = extensions.create('gocdPlugin', GoCDPluginExtension)

        tasks.register('showLicenseReportErrors') {
          doLast {
            def reportText = tasks.named('checkLicenseTask').get().outputs.files.singleFile.text
            def reportContents = new JsonSlurper().parseText(reportText)

            if (reportContents.containsKey('dependenciesWithoutAllowedLicenses')) {
              if (!reportContents.dependenciesWithoutAllowedLicenses.empty) {
                println 'Failed to validate licenses for the following modules:'
                println reportText
              }
            } else {
              throw new GradleException("Could not find key `dependenciesWithoutAllowedLicenses` in license report: ${reportText}")
            }
          }
        }

        tasks.named('checkLicense') {
          finalizedBy 'showLicenseReportErrors'
        }

        tasks.register('githubRelease', ReleaseTask) {
          dependsOn allprojects*.tasks*.withType(Assemble)
        }

        tasks.register('verifyExpRelease', VerifyExpRelease)

        pluginManager.withPlugin('base') {
          tasks.named('assemble') {
            dependsOn 'checkLicense'
          }
        }

        afterEvaluate {
          initializeLicenseReportingLate(gocdPluginExtension, project)
        }
      }
    }

    private static void initializeLicenseReportingLate(GoCDPluginExtension gocdPluginExtension, Project project) {
      Project pluginProject = gocdPluginExtension.pluginProject

      project.with {
        pluginProject.extensions.licenseReport.outputDir = gocdPluginExtension.licenseReport.outputDir ?: "$buildDir/license"
        pluginProject.extensions.licenseReport.projects = gocdPluginExtension.licenseReport.projects ?: allprojects
        pluginProject.extensions.licenseReport.configurations = gocdPluginExtension.licenseReport.configurations ?: ['runtimeClasspath']
        pluginProject.extensions.licenseReport.excludeGroups = gocdPluginExtension.licenseReport.excludeGroups
        pluginProject.extensions.licenseReport.excludeOwnGroup = gocdPluginExtension.licenseReport.excludeOwnGroup
        pluginProject.extensions.licenseReport.excludeBoms = gocdPluginExtension.licenseReport.excludeBoms
        pluginProject.extensions.licenseReport.excludes = gocdPluginExtension.licenseReport.excludes
        pluginProject.extensions.licenseReport.renderers = [new TextReportRenderer()]
        pluginProject.extensions.licenseReport.importers = gocdPluginExtension.licenseReport.importers
        pluginProject.extensions.licenseReport.filters = new LicenseBundleNormalizer(bundlePath: rootProject.licenseNormalizerBundle)
        pluginProject.extensions.licenseReport.allowedLicensesFile = rootProject.defaultAllowedLicenses

        allprojects {
          tasks.withType(Jar).configureEach {
            into('license-report') {
              from "$pluginProject.extensions.licenseReport.outputDir/THIRD-PARTY-NOTICES.txt"
            }
          }
        }
      }
    }
  }

  private static File writeTempFile(Project project, String fileName) {
    def content = project.file(ReleaseTaskHelper.classLoader.getResource(fileName)).text
    def name = fileName.substring(0, fileName.lastIndexOf('.'))
    def ext = fileName.substring(fileName.lastIndexOf('.'))
    project.buildDir.mkdirs()
    def file = File.createTempFile(name, ext, project.buildDir)
    file.write content
    file.deleteOnExit()
    file
  }
}
