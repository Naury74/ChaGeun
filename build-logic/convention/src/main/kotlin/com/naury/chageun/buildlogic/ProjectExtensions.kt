package com.naury.chageun.buildlogic

import org.gradle.api.Project
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

val Project.libs: VersionCatalog
    get() = extensions.getByType<VersionCatalogsExtension>().named("libs")

fun VersionCatalog.library(alias: String) = findLibrary(alias).get()

fun VersionCatalog.versionInt(alias: String) = findVersion(alias).get().requiredVersion.toInt()

fun Project.namespaceFromPath(): String = "com.naury.chageun." + path.removePrefix(":").replace(':', '.').replace('-', '.')
