pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven {
            name = "Menkalian-Artifactory"
            url = uri("https://artifactory.menkalian.de/artifactory/menkalian")
        }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Menkalian-Artifactory"
            url = uri("https://artifactory.menkalian.de/artifactory/menkalian")
        }
        maven {
            name = "JitPack"
            url = uri("https://jitpack.io")
        }
    }
}

rootProject.name = "WorKlock"
include(":app")
