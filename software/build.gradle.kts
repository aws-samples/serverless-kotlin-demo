import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import com.github.jk1.license.render.ReportRenderer
import com.github.jk1.license.render.InventoryHtmlReportRenderer
import com.github.jk1.license.filter.DependencyFilter
import com.github.jk1.license.filter.LicenseBundleNormalizer

plugins {
    kotlin("jvm") version "1.6.0"
    kotlin("plugin.serialization") version "1.5.31"
    id("com.github.johnrengelman.shadow") version "5.1.0"
    id("com.github.jk1.dependency-license-report") version "2.0"
}

licenseReport {
    renderers = arrayOf<ReportRenderer>(InventoryHtmlReportRenderer("report.html","Backend"))
    filters = arrayOf<DependencyFilter>(LicenseBundleNormalizer())
}

group = "com.amazonaws.sample"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.amazonaws:aws-lambda-java-core:1.2.1")
    implementation("com.amazonaws:aws-lambda-java-events:3.11.0")
    implementation("aws.sdk.kotlin:dynamodb:0.9.4-beta")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.1")
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "11"
}