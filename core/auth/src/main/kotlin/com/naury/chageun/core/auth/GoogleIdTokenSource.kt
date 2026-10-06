package com.naury.chageun.core.auth

import android.annotation.SuppressLint
import android.content.Context
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.naury.chageun.core.common.logging.AppLogger
import com.naury.chageun.core.domain.auth.GoogleIdTokenResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/** "Google로 계속하기" 버튼에서 계정을 고르게 하고 ID 토큰을 받아 온다. */
interface GoogleIdTokenSource {
    /** 이 빌드에 Google 로그인용 웹 클라이언트 ID가 있는지. */
    val isConfigured: Boolean

    /** 계정 선택 창을 띄우므로 Activity Context로 불러야 한다. */
    suspend fun request(activityContext: Context): GoogleIdTokenResult
}

internal class CredentialGoogleIdTokenSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val logger: AppLogger,
) : GoogleIdTokenSource {

    private val webClientId: String? by lazy { context.webClientId() }

    override val isConfigured: Boolean get() = webClientId != null

    override suspend fun request(activityContext: Context): GoogleIdTokenResult {
        val clientId = webClientId ?: return GoogleIdTokenResult.Unavailable
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(GetSignInWithGoogleOption.Builder(clientId).build())
            .build()
        return try {
            val credential = CredentialManager.create(
                activityContext,
            ).getCredential(activityContext, request).credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                GoogleIdTokenResult.Token(GoogleIdTokenCredential.createFrom(credential.data).idToken)
            } else {
                GoogleIdTokenResult.Failed
            }
        } catch (_: GetCredentialCancellationException) {
            GoogleIdTokenResult.Cancelled
        } catch (_: NoCredentialException) {
            GoogleIdTokenResult.NoAccount
        } catch (e: GetCredentialException) {
            logger.warn("auth_google_credential_failed", error = e)
            GoogleIdTokenResult.Failed
        } catch (e: GoogleIdTokenParsingException) {
            logger.warn("auth_google_token_parse_failed", error = e)
            GoogleIdTokenResult.Failed
        }
    }
}

/**
 * google-services 플러그인은 콘솔에 OAuth 클라이언트가 있을 때만 `default_web_client_id`를 만든다.
 * 없는 빌드도 컴파일되도록 이름으로 찾는다.
 */
@SuppressLint("DiscouragedApi")
private fun Context.webClientId(): String? {
    val id = resources.getIdentifier("default_web_client_id", "string", packageName)
    return if (id == 0) null else getString(id).takeIf { it.isNotBlank() }
}
