plugins {
    kotlin("jvm") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
    kotlin("plugin.serialization") version "1.9.21"
}

group = "com.sensedev"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")
    
    // Kotlin parsing
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:1.9.21")
    
    // Java parsing
    implementation("com.github.javaparser:javaparser-core:3.25.7")
    
    // JSON serialization for caching
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // HTTP client for AI providers
    implementation("io.ktor:ktor-client-core:2.3.7")
    implementation("io.ktor:ktor-client-cio:2.3.7")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.7")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.7")
    
    // Markdown rendering for AI chat - temporarily disabled due to version compatibility
    // implementation("com.mikepenz:multiplatform-markdown-renderer:0.16.0")
    // implementation("com.mikepenz:multiplatform-markdown-renderer-m2:0.16.0")
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "SenseDev"
            packageVersion = "0.1.0"
            description = "SenseDev AI-Assisted Code Analysis"
            vendor = "SenseDev"
            
            windows {
                iconFile.set(project.file("src/main/resources/SenseDev_icon.ico"))
                // Ensure console is hidden for production build, or keep it true for debug preview?
                // Plan says "Developer Preview quality", so maybe console is okay, but goal is "double-clickable .exe".
                // Usually users don't want console.
                console = false
            }
        }
    }
}

task("verifyEngine", JavaExec::class) {
    mainClass.set("verification.VerifyEngineKt")
    classpath = sourceSets["main"].runtimeClasspath
}
