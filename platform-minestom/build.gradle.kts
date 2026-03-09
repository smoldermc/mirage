dependencies {
    implementation(project(":mirage-core"))
    implementation(libs.adventure.text.minimessage)
    runtimeOnly(libs.slf4j.simple)
    compileOnly(libs.minestom)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.minestom.testing)
}

extensions.configure<JavaPluginExtension> {
    toolchain.languageVersion.set(JavaLanguageVersion.of(25))
}

tasks.withType<JavaCompile>().configureEach {
    options.release.set(25)
}

tasks.register<JavaExec>("runManualServer") {
    group = "verification"
    description = "Runs a barebones Minestom server with Mirage installed for manual in-game testing."
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("fr.smolder.mirage.minestom.MinestomManualTestServer")
    workingDir = rootProject.projectDir

    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(25))
    })
}
