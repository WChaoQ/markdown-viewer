import java.time.Clock
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.net.URL

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.wchaoq.markdownviewer"
    compileSdk = 36

    defaultConfig {
        val buildTime = LocalDateTime.now(Clock.systemUTC())
            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'"))
        buildConfigField("String", "BUILD_TIME", "\"$buildTime\"")
        applicationId = "com.wchaoq.markdownviewer"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

android.sourceSets.getByName("main").assets.srcDir(layout.buildDirectory.dir("generated/markdownRenderer/assets"))

dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.10.00"))
    implementation("androidx.activity:activity-compose:1.11.0")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    implementation("org.jetbrains:markdown:0.7.16")
    implementation("androidx.webkit:webkit:1.14.0")
    implementation("org.jsoup:jsoup:1.19.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}

val markdownAssetDirectory = layout.buildDirectory.dir("generated/markdownRenderer/assets")
val markdownRendererAssets = listOf(
    "markdown/mermaid.min.js" to "https://cdn.jsdelivr.net/npm/mermaid@11.4.1/dist/mermaid.min.js",
    "markdown/katex/katex.min.js" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/katex.min.js",
    "markdown/katex/auto-render.min.js" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/contrib/auto-render.min.js",
    "markdown/katex/katex.min.css" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/katex.min.css",
    "markdown/licenses/KaTeX-LICENSE.txt" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/LICENSE",
    "markdown/licenses/Mermaid-LICENSE.txt" to "https://cdn.jsdelivr.net/npm/mermaid@11.4.1/LICENSE",
    "markdown/katex/fonts/KaTeX_AMS-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_AMS-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Caligraphic-Bold.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Caligraphic-Bold.woff2",
    "markdown/katex/fonts/KaTeX_Caligraphic-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Caligraphic-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Fraktur-Bold.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Fraktur-Bold.woff2",
    "markdown/katex/fonts/KaTeX_Fraktur-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Fraktur-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Main-Bold.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Main-Bold.woff2",
    "markdown/katex/fonts/KaTeX_Main-BoldItalic.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Main-BoldItalic.woff2",
    "markdown/katex/fonts/KaTeX_Main-Italic.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Main-Italic.woff2",
    "markdown/katex/fonts/KaTeX_Main-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Main-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Math-BoldItalic.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Math-BoldItalic.woff2",
    "markdown/katex/fonts/KaTeX_Math-Italic.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Math-Italic.woff2",
    "markdown/katex/fonts/KaTeX_SansSerif-Bold.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_SansSerif-Bold.woff2",
    "markdown/katex/fonts/KaTeX_SansSerif-Italic.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_SansSerif-Italic.woff2",
    "markdown/katex/fonts/KaTeX_SansSerif-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_SansSerif-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Script-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Script-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Size1-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Size1-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Size2-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Size2-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Size3-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Size3-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Size4-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Size4-Regular.woff2",
    "markdown/katex/fonts/KaTeX_Typewriter-Regular.woff2" to "https://cdn.jsdelivr.net/npm/katex@0.16.22/dist/fonts/KaTeX_Typewriter-Regular.woff2"
)

val downloadMarkdownRendererAssets = tasks.register("downloadMarkdownRendererAssets") {
    outputs.dir(markdownAssetDirectory)
    outputs.upToDateWhen {
        markdownRendererAssets.all { (path, _) ->
            markdownAssetDirectory.get().file(path).asFile.let { it.isFile && it.length() > 0L }
        }
    }
    doLast {
        val root = markdownAssetDirectory.get().asFile
        markdownRendererAssets.forEach { (path, sourceUrl) ->
            val target = root.resolve(path)
            if (!target.isFile || target.length() == 0L) {
                target.parentFile.mkdirs()
                val temporary = target.resolveSibling(target.name + ".download")
                URL(sourceUrl).openConnection().apply {
                    connectTimeout = 20_000
                    readTimeout = 60_000
                    setRequestProperty("User-Agent", "MarkdownViewer-Asset-Downloader")
                }.getInputStream().use { input ->
                    temporary.outputStream().use { output -> input.copyTo(output) }
                }
                check(temporary.length() > 1024L) { "Renderer asset download was incomplete: $sourceUrl" }
                check(temporary.renameTo(target)) { "Unable to place renderer asset: $target" }
            }
        }
    }
}
tasks.named("preBuild").configure { dependsOn(downloadMarkdownRendererAssets) }
