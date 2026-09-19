plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.3.20" apply false
    // Kotlin is pinned to 2.3.20 so it matches KSP 2.3.12 (newest KSP build); Room + Hilt use KSP.
    id("org.jetbrains.kotlin.plugin.serialization") version "2.3.20" apply false
    id("com.google.devtools.ksp") version "2.3.12" apply false
    id("com.google.dagger.hilt.android") version "2.60.1" apply false
}
