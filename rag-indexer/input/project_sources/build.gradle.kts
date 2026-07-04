buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.4")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.21")
        classpath("com.google.dagger:dagger-compiler:2.48")
    }
}

plugins {
    id("com.android.application") version "8.1.4" apply false
    id("org.jetbrains.kotlin.android") version "1.9.21" apply false
    id("com.android.library") version "8.1.4" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.21" apply false
    kotlin("kapt") version "1.9.21" apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}

ext {
    set("composeVersion", "1.5.7")
    set("kotlinVersion", "1.9.21")
    set("daggerVersion", "2.48")
    set("retrofitVersion", "2.9.0")
    set("okhttpVersion", "4.12.0")
    set("gsonVersion", "2.10.1")
}