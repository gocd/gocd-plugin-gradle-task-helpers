# Gradle task helper for building GoCD plugins

This repo contains common gradle tasks if you're building a GoCD plugin using gradle.

## Usage

In your root project's `build.gradle` file add the following line:

```gradle
// use the master (with a query param to bust cache every 60 seconds)
apply from: "https://raw.githubusercontent.com/gocd/gocd-plugin-gradle-task-helpers/master/helper.gradle?_=${(int) (new Date().toInstant().epochSecond / 60)}"
// OR use a specific sha
apply from: 'https://raw.githubusercontent.com/gocd/gradle-task-helpers/GIT_COMMITISH/helper.gradle'

// specify plugin details
gocdPlugin {
    id = 'com.example.myplugin'
    pluginVersion = '1.2.3'
    goCdVersion = '19.3.0'
    name = 'GoCD Flux Capacitor plugin'
    description = 'Flux capacitors allow you to travel time'
    vendorName = 'Thoughtworks, Inc.'
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

## License Verification

The license verification is via the `com.github.jk1:gradle-license-report` dependency. The following files are being utilized:

1. The `normalizing-licenses-bundle.json` normalizes a range of license based on a `name`, `url` or `file-content` pattern. The resulting license name should always be a standard name. See [this](https://spdx.org/licenses/) for the list of standard licenses.

2. The `default-allowed-licenses.json` contains the list of license names (standard names) after the normalization that are allowed to be a part of the plugin. Any plugin not supporting one of these will break the build and an exception will be thrown regarding the same.

For more info on license checks, see [this](https://github.com/jk1/Gradle-License-Report#license-data-grouping)
