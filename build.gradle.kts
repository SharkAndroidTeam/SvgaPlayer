import com.shark.svgaplayer.by
import com.shark.svgaplayer.groupId
import com.shark.svgaplayer.versionName

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradlePlugin.android)
        classpath(libs.gradlePlugin.kotlin)
        classpath(libs.gradlePlugin.mavenPublish)
    }
}


@Suppress("DSL_SCOPE_VIOLATION", "UnstableApiUsage")
plugins {
    alias(libs.plugins.binaryCompatibility)
    alias(libs.plugins.dokka)
    alias(libs.plugins.ktlint)
}

extensions.configure<kotlinx.validation.ApiValidationExtension> {
    ignoredProjects += kotlin.arrayOf(
        "app",
    )
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }


    apply(plugin = "org.jlleitschuh.gradle.ktlint")

    extensions.configure<org.jlleitschuh.gradle.ktlint.KtlintExtension> {
        version by rootProject.libs.versions.ktlint
        disabledRules by kotlin.collections.setOf(
            "indent",
            "max-line-length",
            "parameter-list-wrapping"
        )
    }

    tasks.withType<org.jetbrains.dokka.gradle.DokkaTaskPartial>().configureEach {
        dokkaSourceSets.configureEach {
            jdkVersion by 8
            skipDeprecated by true
            suppressInheritedMembers by true

            externalDocumentationLink {
                url by java.net.URL("https://developer.android.com/reference/")
            }
            externalDocumentationLink {
                url by java.net.URL("https://kotlin.github.io/kotlinx.coroutines/kotlinx-coroutines-core/")
                packageListUrl by java.net.URL("https://kotlin.github.io/kotlinx.coroutines/package-list")
            }
            externalDocumentationLink {
                url by java.net.URL("https://square.github.io/okhttp/4.x/")
                packageListUrl by java.net.URL("https://square.github.io/okhttp/4.x/okhttp/package-list")
            }
            externalDocumentationLink {
                url by java.net.URL("https://square.github.io/okio/3.x/okio/")
                packageListUrl by java.net.URL("https://square.github.io/okio/3.x/okio/okio/package-list")
            }
        }
    }

}


tasks {
    val clean by registering(Delete::class) {
        delete(buildDir)
    }
}