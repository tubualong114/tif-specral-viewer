plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("qupath-conventions")
}

qupathExtension {
    name = "qupath-extension-spectrum-viewer"
    group = "io.github.qupath"
    version = "0.1.0-SNAPSHOT"
    description = "Inspect raw per-channel pixel spectra in QuPath"
    automaticModule = "io.github.qupath.extension.spectrum"
}

dependencies {
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)

    testImplementation(libs.bundles.qupath)
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.platform)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
