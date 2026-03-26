plugins {
    id("com.android.application") version "9.1.0" apply false
    id("org.jetbrains.kotlin.android") version "2.2.10" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    // Must match Kotlin patch line (KSP 2.3.x targets Kotlin 2.3 and breaks Room with 2.2.x).
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.10" apply false
    id("com.google.devtools.ksp") version "2.3.2" apply false
}
