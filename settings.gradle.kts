pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
    // O bloco 'versionCatalogs' foi removido.
    // O Gradle já carrega automaticamente o arquivo 'gradle/libs.versions.toml'
    // e cria o catálogo 'libs' para você.
}

rootProject.name = "TaskList"
include(":app")
