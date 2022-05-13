package com.shark.svgaplayer

import org.gradle.api.plugins.ExtensionAware
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmOptions
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get


fun Project.setupLibraryModule(
    buildConfig: Boolean = false,
    publish: Boolean = false,
    document: Boolean = publish,
    block: LibraryExtension.() -> Unit = {}
) = setupBaseModule<LibraryExtension> {
    libraryVariants.all {
        generateBuildConfigProvider?.configure { enabled = buildConfig }
    }
    if (publish) {
        if (document) apply(plugin = "org.jetbrains.dokka")
        apply(plugin = "com.vanniktech.maven.publish.base")
        publishing {
            singleVariant("release") {
                withJavadocJar()
                withSourcesJar()
            }
        }
        afterEvaluate {
//            extensions.configure<PublishingExtension> {
//                publications.create<MavenPublication>("release") {
//                    from(components["release"])
//                    // https://github.com/vanniktech/gradle-maven-publish-plugin/issues/326
//                    val id = project.property("POM_ARTIFACT_ID").toString()
//                    artifactId = artifactId.replace(project.name, id)
//                }
//            }
        }
    }
    block()
}


fun Project.setupAppModule(
    block: BaseAppModuleExtension.() -> Unit = {}
) = setupBaseModule<BaseAppModuleExtension> {
    defaultConfig {
        versionCode = project.versionCode
        versionName = project.versionName
        resourceConfigurations += "en"
        vectorDrawables.useSupportLibrary = true
    }
    block()
}


private inline fun <reified T : BaseExtension> Project.setupBaseModule(
    crossinline block: T.() -> Unit = {}
) = extensions.configure<T>("android") {
    compileSdkVersion(31)
    defaultConfig {
        minSdk = 21
        targetSdk = 31
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
        allWarningsAsErrors = true
    }
    packagingOptions {
        resources.pickFirsts += "META-INF/AL2.0"
        resources.pickFirsts += "META-INF/LGPL2.1"
        resources.pickFirsts += "META-INF/*kotlin_module"
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
    block()
}


private fun BaseExtension.kotlinOptions(block: KotlinJvmOptions.() -> Unit) {
    (this as ExtensionAware).extensions.configure("kotlinOptions", block)
}
