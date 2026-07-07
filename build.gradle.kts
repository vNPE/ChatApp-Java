plugins {
    application
    java
}

repositories { mavenCentral() }

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")

    val fxVer = "27-ea+23"
    implementation("org.openjfx:javafx-base:$fxVer:linux")
    implementation("org.openjfx:javafx-controls:$fxVer:linux")
    implementation("org.openjfx:javafx-graphics:$fxVer:linux")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(26)) }
}

sourceSets {
    main { java.setSrcDirs(listOf(".")) }
}

application {
    mainClass.set("Server")
}

tasks.register("runClient", JavaExec::class) {
    group = "application"
    dependsOn("classes")

    mainClass.set("ClientFx")

    val runtimeCp = configurations.runtimeClasspath.get()
    val javafxJars = runtimeCp.filter { it.name.startsWith("javafx-") && it.name.endsWith("-linux.jar") }
    val nonJavafx = runtimeCp.filterNot { it in javafxJars }

    classpath = files(sourceSets.main.get().output) + files(nonJavafx)

    jvmArgs(
        "--module-path", javafxJars.asPath,
        "--add-modules", "javafx.base,javafx.controls,javafx.graphics"
    )
}
