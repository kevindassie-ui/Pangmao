pluginManagement {
    val localProxy = System.getenv("PANGMAO_MAVEN_PROXY")
    repositories {
        if (localProxy != null) {
            maven {
                name = "LocalOfficialRepositoriesProxy"
                url = uri("$localProxy/all")
                isAllowInsecureProtocol = true
            }
        } else {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
    }
}

dependencyResolutionManagement {
    val localProxy = System.getenv("PANGMAO_MAVEN_PROXY")
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        if (localProxy != null) {
            maven {
                name = "LocalOfficialRepositoriesProxy"
                url = uri("$localProxy/all")
                isAllowInsecureProtocol = true
            }
        } else {
            google()
            mavenCentral()
        }
    }
}

rootProject.name = "Pangmao"
include(":app")
