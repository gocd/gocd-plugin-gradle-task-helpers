# Gradle task helper for building GoCD plugins

This repo contains common gradle tasks if you're building a GoCD plugin using gradle.

## Usage

In your root project's `build.gradle` file add the following line:

```gradle
// use the master (with a query param to bust cache every 60 seconds)
apply from: apply from: "https://raw.githubusercontent.com/gocd/gocd-plugin-gradle-task-helpers/master/helper.gradle?_=${(int) (new Date().toInstant().epochSecond / 60)}"
// use a specific sha
apply from: 'https://raw.githubusercontent.com/gocd/gradle-task-helpers/GIT_COMMITISH/helper.gradle'

// specify plugin details
gocdPlugin {
    id = 'com.example.myplugin'
    pluginVersion = '1.2.3'
    goCdVersion = '19.3.0'
    name = 'GoCD Flux Capacitor plugin'
    description = 'Flux capacitors allow you to travel time'
    vendorName = 'ThoughtWorks, Inc.'
    vendorUrl = 'https://github.com/example/gocd-flux-capacitor-plugin'

    // specify what github repository to release to
    githubRepo {
        owner = System.getenv('GITHUB_USER') ?: 'bob'
        repo = 'gocd-flux-capacitor-plugin'
        token = System.getenv('GITHUB_TOKEN') ?: 'bad-token'
    }

    // what is the plugin project.
    // For gradle single project builds, use `project`
    pluginProject = project

    // for multi project builds, use `project(':path-to-my-project')`
    pluginProject = project(':my-project')

    prerelease = !"No".equalsIgnoreCase(System.getenv('PRERELEASE'))

    // specify the `jar` task whose output must be published to github release

    // for single project builds:
    assetsToRelease = [jar]

    // for multi project builds
    assetsToRelease = [
      project(':my-plugin').tasks.findByName('jar'),
      project(':my-cli').tasks.findByName('jar'),
    ]
}

// specify the version of your project
allprojects {
  version = gocdPlugin.fullVersion(project)
}
```
