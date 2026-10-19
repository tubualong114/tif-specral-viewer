plugins {
    id("com.gradleup.shadow") version "8.3.5"
    id("qupath-conventions")
}

qupathExtension {
    name = "qupath-extension-spectrum-viewer"
    group = "io.github.qupath"
    version = "0.2.0-SNAPSHOT"
    description = "Inspect raw per-channel pixel spectra in QuPath"
    automaticModule = "io.github.qupath.extension.spectrum"
}

dependencies {
    shadow(libs.bundles.qupath)
    shadow(libs.bundles.logging)

    testImplementation(libs.bundles.qupath)
    // The convention's JavaFX modules are compile-only for the extension itself.
    testImplementation(files(sourceSets.main.get().compileClasspath))
    testImplementation(libs.junit)
    testRuntimeOnly(libs.junit.platform)
}

tasks.test {
    useJUnitPlatform()
    exclude("**/SpectrumInspectorUiTest.class")
}

tasks.register<Test>("uiTest") {
    description = "Run the QuPath/JavaFX integration test (requires a graphical desktop)."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform()
    include("**/SpectrumInspectorUiTest.class")
    systemProperty("java.util.prefs.PreferencesFactory", "io.github.qupath.extension.spectrum.MemoryPreferencesFactory")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("-Xlint:deprecation", "-Xlint:unchecked"))
}
