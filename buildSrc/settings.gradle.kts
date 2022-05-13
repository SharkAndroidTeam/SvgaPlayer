@Suppress("UnstableApiUsage")
dependencyResolutionManagement {
    defaultLibrariesExtensionName.set("projectLibs")
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
