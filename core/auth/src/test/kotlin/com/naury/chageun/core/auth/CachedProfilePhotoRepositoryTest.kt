package com.naury.chageun.core.auth

import com.google.common.truth.Truth.assertThat
import com.naury.chageun.core.testing.FakeAuthRepository
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class CachedProfilePhotoRepositoryTest {

    @get:Rule
    val folder = TemporaryFolder()

    private val auth = FakeAuthRepository()
    private val requested = mutableListOf<String>()
    private var response: ByteArray? = PHOTO

    private val directory by lazy { File(folder.root, "profile") }

    private fun TestScope.repository() = CachedProfilePhotoRepository(
        authRepository = auth,
        cache = ProfilePhotoCache(directory),
        downloader = { url ->
            requested += url
            response
        },
        ioDispatcher = StandardTestDispatcher(testScheduler),
    )

    @Test
    fun signedOut_hasNoPhoto() = runTest {
        assertThat(repository().photoPath.first()).isNull()
        assertThat(requested).isEmpty()
    }

    @Test
    fun googleSignIn_downloadsPhotoIntoOneFile() = runTest {
        auth.googlePhotoUrl = FIRST_URL
        auth.signInWithGoogle("token")

        val path = repository().photoPath.first()

        assertThat(path).isNotNull()
        assertThat(File(checkNotNull(path)).readBytes()).isEqualTo(PHOTO)
        assertThat(File(path).parentFile).isEqualTo(directory)
        assertThat(directory.listFiles()?.size).isEqualTo(1)
    }

    @Test
    fun sameUrl_usesCachedFile_withoutDownloadingAgain() = runTest {
        auth.googlePhotoUrl = FIRST_URL
        auth.signInWithGoogle("token")
        val first = repository().photoPath.first()

        response = null // 오프라인
        val second = repository().photoPath.first()

        assertThat(second).isEqualTo(first)
        assertThat(requested).containsExactly(FIRST_URL)
    }

    @Test
    fun changedUrl_replacesPhoto_withNewPath() = runTest {
        auth.googlePhotoUrl = FIRST_URL
        auth.signInWithGoogle("token")
        val first = checkNotNull(repository().photoPath.first())

        auth.googlePhotoUrl = SECOND_URL
        response = NEW_PHOTO
        auth.signInWithGoogle("token")
        val second = checkNotNull(repository().photoPath.first())

        assertThat(second).isNotEqualTo(first)
        assertThat(File(first).exists()).isFalse()
        assertThat(File(second).readBytes()).isEqualTo(NEW_PHOTO)
        assertThat(directory.listFiles()?.map(File::getPath)).containsExactly(second)
    }

    @Test
    fun downloadFailure_fallsBackToNoPhoto_andDropsOldPhoto() = runTest {
        auth.googlePhotoUrl = FIRST_URL
        auth.signInWithGoogle("token")
        val first = checkNotNull(repository().photoPath.first())

        auth.googlePhotoUrl = SECOND_URL
        response = null
        auth.signInWithGoogle("token")

        assertThat(repository().photoPath.first()).isNull()
        assertThat(File(first).exists()).isFalse()
    }

    @Test
    fun signOut_removesPhoto() = runTest {
        auth.googlePhotoUrl = FIRST_URL
        auth.signInWithGoogle("token")
        val repository = repository()
        val path = checkNotNull(repository.photoPath.first())

        auth.signOut()

        assertThat(repository.photoPath.first()).isNull()
        assertThat(File(path).exists()).isFalse()
        assertThat(directory.exists()).isFalse()
    }

    @Test
    fun accountWithoutPhoto_neverDownloads() = runTest {
        auth.signInWithGoogle("token")

        assertThat(repository().photoPath.first()).isNull()
        assertThat(requested).isEmpty()
    }

    private companion object {
        const val FIRST_URL = "https://lh3.googleusercontent.com/a/first=s96-c"
        const val SECOND_URL = "https://lh3.googleusercontent.com/a/second=s96-c"
        val PHOTO = byteArrayOf(1, 2, 3)
        val NEW_PHOTO = byteArrayOf(4, 5, 6, 7)
    }
}
