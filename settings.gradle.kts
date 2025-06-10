pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
<<<<<<< HEAD
        jcenter()
=======
>>>>>>> f196124c92308be08870bf4c05ede937a693f7b3
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
<<<<<<< HEAD
        jcenter()
=======
>>>>>>> f196124c92308be08870bf4c05ede937a693f7b3
    }
}

rootProject.name = "NFC Shopping App"
include(":app")
