# Gradle subtree example

This repository is an example on how to use gradle with a multi-module project composed of a autonomous sub-tree repository


## Use case

In some cases, we want to have a part of a project open source and an other part closed source. It might for a developer to synchronize both projects and in that case it is common to use a git sub-tree, the open source project is added in a folder of the closed source project and all development happens in the closed source and is back ported using git to the open source one.

It might be tricky to have the closed source project (alias the mono-repo) to handle this sub-tree well when building it using gradle moreover when it is a multi-module project. A solution provided by gradle the the [includeBuild method](https://blog.gradle.org/introducing-composite-builds). This is an other approach where the sub-tree is seen by gradle as it was part of the project directly.

## How it works

The main idea is that the sub-tree project declare an additional 'root' project and the root of the mono-repo will set the path of all project inside this new root relativly to the sub-tree folder.

### Setup the sub-tree

The sub-tree do not declare its projects in `settings.gradle` but in an other file, here `oss-settings.gradle`
All declared projects have are children of a new root project, here `:my-project` and this project is also included.

The `settings.gradle` import this file and change the paths:

```groovy
apply from: 'oss-settings.gradle'

def fixPath
fixPath = { project ->
    String relativeProjectPath = project.projectDir.path.replace(settingsDir.path, "")
    project.projectDir = new File(relativeProjectPath.replace("/my-project/", ''))
    project.children.each fixPath
}

rootProject.children.each fixPath
```

dependencies can be expressed as usual but it must include this new root project.
```groovy
dependencies {
    compile project(':my-project:module-a')
}
```

### Setup the mono-repo

Once the sub-tree itself is setup few tweaks must be made to the mono-repo project

`settings.gradle` must apply the `oss-settings.gradle`, set the path correctly, add include the root project of the sub-tree then add its projects as usual.

```groovy
apply from: 'sub-tree/oss-settings.gradle'

def fixPath
fixPath = { project ->
    String relativeProjectPath = project.projectDir.path.replace(settingsDir.path, "")
    project.projectDir = new File(relativeProjectPath.replace("/my-project/", 'sub-tree/'))
    project.children.each fixPath
}
rootProject.children.each fixPath

include ':my-project'
project(':my-project').projectDir = "$rootDir/sub-tree" as File


include ':other-module'
```

### Results

In this example project, running the `run` task in the sub-tree directory gives:

```
$ gradle run
> Task :my-project:run
oss project: Hello World
```

and running the same task in the mono-repo:

```
$ gradle run
> Task :other-module:run
com project: Hello World

> Task :my-project:module-b:run
oss project: Hello World
```

## Plugins sharing

We often use custom plugin directly in the buildSrc to share build logic. To keep this behavior in that case can be done as follow:

in the sub-tree project add you `buildSrc` directory and add an extra build file to declare the plugin:

```groovy
apply plugin: 'groovy'
apply plugin: 'java-gradle-plugin'

gradlePlugin {
    plugins {
        ossPlugin {
            id = "oss-plugin"
            implementationClass = "OSSPlugin"
        }
    }
}
```

and in the mono-repo the buildSrc project must include this project as a `runtime` dependency

build.gradle
```groovy
dependencies {
    runtime subprojects
}
```

settings.gradle
```
include ':oss-buildSrc'

project(':oss-buildSrc').projectDir = "$rootDir/../sub-tree/buildSrc" as File
```


With this technique plugins from the plugins of the sub-tree project can be used in the mono-repo projects and the mono-repo can declare its own plugins:

When running the tasks from the sub-tree:
```
> Task :my-project:module-b:customOSSTask
OSSPlugin is applied
```


When running the tasks from mono-repo:
```
> Task :other-module:customComTask
ComPlugin is applied

> Task :other-module:customOSSTask
OSSPlugin is applied

> Task :my-project:module-b:customOSSTask
OSSPlugin is applied
```


## Differences with `includeBuild` approach

The include  build appreach described [here](https://blog.gradle.org/introducing-composite-builds) is great, but it has a drawback:

If you run at the root of the project `gradle test` it will run test only on projects "natively" in the root project. It can be ok when you only need to include libraries with their own life cycle but in our use case we want to have a single task that build and test everything.


With includ build we would have
```
$ gradle run
> Task :other-module:run
com project: Hello World
```

With our approach all tasks with the given name will be launched
```
$ gradle run
> Task :other-module:run
com project: Hello World

> Task :my-project:module-b:run
oss project: Hello World
```
