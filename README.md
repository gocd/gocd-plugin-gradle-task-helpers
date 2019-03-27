# Gradle task helper for building GoCD plugins

This repo contains common gradle tasks if you're building a GoCD plugin using gradle.

## Usage

In your root project's `build.gradle` file add the following line:

```groovy
// use the master (with a query param to bust cache every 60 seconds)
apply from: apply from: "https://raw.githubusercontent.com/gocd/gocd-plugin-gradle-task-helpers/master/helper.gradle?_=${(int) (new Date().toInstant().epochSecond / 60)}"
// use a specific sha
apply from: 'https://raw.githubusercontent.com/gocd/gradle-task-helpers/GIT_COMMITISH/helper.gradle'
```
