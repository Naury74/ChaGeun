package com.naury.chageun.core.auth

import com.google.common.truth.Truth.assertThat
import java.io.File
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProfilePhotoCacheTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val directory by lazy { File(folder.root, "profile") }
    private val cache by lazy { ProfilePhotoCache(directory) }

    @Test
    fun fileName_doesNotContainTheUrl() {
        val file = cache.store("https://lh3.googleusercontent.com/a/secret-id=s96-c", byteArrayOf(1))

        assertThat(file.name).doesNotContain("secret")
        assertThat(file.name).matches("avatar-[0-9a-f]{16}\\.jpg")
    }

    @Test
    fun store_leavesOnlyTheNewPhoto_andNoPartialFile() {
        cache.store("https://example.com/a.jpg", byteArrayOf(1))
        val second = cache.store("https://example.com/b.jpg", byteArrayOf(2))

        assertThat(directory.listFiles()?.toList()).containsExactly(second)
        assertThat(cache.cached("https://example.com/a.jpg")).isNull()
        assertThat(cache.cached("https://example.com/b.jpg")).isEqualTo(second)
    }

    @Test
    fun clear_removesFolder() {
        cache.store("https://example.com/a.jpg", byteArrayOf(1))

        cache.clear()

        assertThat(directory.exists()).isFalse()
    }
}
