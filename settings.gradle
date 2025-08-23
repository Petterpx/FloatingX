pluginManagement {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven { url 'https://jitpack.io' }
    }
}


dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google()
        maven { url "https://jitpack.io" }
    }
}
rootProject.name = "FloatingX"
include ':app'
include ':floatingx'
include ':floatingx_compose'

static Properties readPropertiesIfExist(File propertiesFile) {
    Properties result = new Properties()
    if (propertiesFile.exists()) {
        propertiesFile.withReader('UTF-8') { reader -> result.load(reader) }
    }
    return result
}

gradle.projectsLoaded { project ->
    def rootProject = project.gradle.rootProject
    def rootProjectExt = rootProject.ext
    def localProperties = readPropertiesIfExist(new File(settingsDir, "local.properties"))

    rootProjectExt.isDev = localProperties.getProperty("isDev", "true").toBoolean()
    rootProjectExt.isPublish = rootProject.hasProperty('isPublish') ? rootProject.getProperty('isPublish').toBoolean() : false
    rootProjectExt.versionName = rootProject.hasProperty('versionName') ? rootProject.getProperty('versionName').toString() : gitVersionTag()
    rootProjectExt.versionCode = rootProject.hasProperty('versionCode') ? rootProject.getProperty('versionCode').toString() : gitVersionCode()
    println '------Fx---->'
    println("isDev-> ${rootProjectExt.isDev}")
    println("isPublish-> ${rootProjectExt.isPublish}")
    println("versionName-> ${rootProjectExt.versionName}")
    println("versionCode-> ${rootProjectExt.versionCode}")
    println "------Fx-end----->"
}

static def gitVersionTag() {
    def commit = 'git rev-list --tags --max-count=1'
    def cmd = "git describe --tags ${commit.execute().text}"
    def version = cmd.execute().text.trim()
    return version
}

static def gitVersionCode() {
    def cmd = 'git rev-list HEAD --count'
    return cmd.execute().text.trim().toInteger()
}
