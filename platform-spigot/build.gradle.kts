dependencies {
    implementation(project(":core"))
    implementation(libs.adventure.text.minimessage)
    compileOnly(libs.paper.api)
}

tasks.processResources {
    inputs.property("version", project.version)
    filteringCharset = "UTF-8"

    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
