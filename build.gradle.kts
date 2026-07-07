plugins {
    java
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
    implementation("org.postgresql:postgresql:42.7.4")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(26))
    }
}

sourceSets {
    main {
        java.setSrcDirs(listOf("."))
    }
}

application {
    mainClass.set("Server")
}

tasks.test {
    useJUnitPlatform()
}
