pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven{
            url = uri("https://maven.pkg.github.com/guiborges007/Aplicativo-de-automacao-avancada")
            credentials{
                username = "guiborges007"
                password = "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"
            }
        }
    }
}

rootProject.name = "ExemploGPS"
include(":app")
include(":MinhaBiblioteca")
