plugins {
    id("java")
}

group = "net.meowsers"
version = "1.0"

val lwjglVersion = "3.4.3"
val jomlVersion = "1.10.9"
val imguiVersion = "1.92.7.1"

// Detect os for lwjgl
val lwjglNatives = Pair(
    System.getProperty("os.name")!!,
    System.getProperty("os.arch")!!
).let { (name, arch) ->
    val isArm = arch.startsWith("arm") || arch.startsWith("aarch64")
    when {
        arrayOf("Linux", "SunOS", "Unit").any { name.startsWith(it) } ->
            if (isArm) "natives-linux-arm64" else "natives-linux"
        arrayOf("Mac OS X", "macOS").any { name.startsWith(it) } ->
            if (isArm) "natives-macos-arm64" else "natives-macos"
        arrayOf("Windows").any { name.startsWith(it) } ->
            if (isArm) "natives-windows-arm64" else if (arch.contains("64")) "natives-windows" else "natives-windows-x86"
        else -> "natives-macos-arm64"
    }
}

val nativePlatforms = listOf(
    "natives-windows",
    "natives-windows-arm64",
    "natives-linux",
    "natives-linux-arm64",
    "natives-macos",
    "natives-macos-arm64"
)

repositories {
    mavenCentral()
}

dependencies {
    // JUnit dependencies
    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // LWJGL dependencies
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-assimp")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-openal")
    implementation("org.lwjgl:lwjgl-opengl")
    implementation("org.lwjgl:lwjgl-stb")

    // Current host platform native dependencies
    implementation("org.lwjgl:lwjgl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-assimp::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-openal::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-opengl::$lwjglNatives")
    implementation("org.lwjgl:lwjgl-stb::$lwjglNatives")

    // Multiplatform runtimes for portability's sake
    for (platform in nativePlatforms) {
        runtimeOnly("org.lwjgl:lwjgl::$platform")
        runtimeOnly("org.lwjgl:lwjgl-assimp::$platform")
        runtimeOnly("org.lwjgl:lwjgl-glfw::$platform")
        runtimeOnly("org.lwjgl:lwjgl-openal::$platform")
        runtimeOnly("org.lwjgl:lwjgl-opengl::$platform")
        runtimeOnly("org.lwjgl:lwjgl-stb::$platform")
    }

    // JOML
    implementation("org.joml:joml:$jomlVersion")

    // ImGui
    implementation("io.github.spair:imgui-java-binding:$imguiVersion")
    implementation("io.github.spair:imgui-java-lwjgl3:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-windows:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-linux:$imguiVersion")
    implementation("io.github.spair:imgui-java-natives-macos:$imguiVersion")

    // GSON
    implementation("com.google.code.gson:gson:2.14.0")
}

// Java compilation optimization & parallel process forking
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.isFork = true
    options.forkOptions.jvmArgs = listOf("-Xms512m", "-Xmx2048m")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    jvmArgs("-XstartOnFirstThread", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")

    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs("-XstartOnFirstThread", "--enable-native-access=ALL-UNNAMED", "--sun-misc-unsafe-memory-access=allow")
}

tasks.register<JavaExec>("setupSlang") {
    group = "setup"
    description = "Downloads and sets up the portable Slang compiler for the current platform if not present"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.meowsers.Peach.Utils.SlangManager")
}

tasks.register<JavaExec>("runPeach") {
    group = "application"
    description = "Runs the engine"

    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("net.meowsers.Main")
}