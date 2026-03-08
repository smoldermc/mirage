dependencies {
    api(libs.slf4j.api)
    implementation(libs.caffeine)
    implementation(libs.configurate.yaml)
    implementation(libs.mineskin.client)
    implementation(libs.mineskin.client.jsoup)
    runtimeOnly(libs.sqlite.jdbc)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}
