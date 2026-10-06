package com.naury.chageun.core.auth

import com.google.common.truth.Truth.assertThat
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.naury.chageun.core.domain.auth.AuthError
import org.junit.Test

class AuthErrorsTest {

    @Test
    fun mapsFirebaseExceptions_toGuidedErrors() {
        assertThat(FirebaseNetworkException("offline").toAuthError()).isEqualTo(AuthError.Network)
        assertThat(FirebaseTooManyRequestsException("slow down").toAuthError()).isEqualTo(AuthError.TooManyRequests)
        assertThat(FirebaseAuthWeakPasswordException("ERROR_WEAK_PASSWORD", "weak", "short").toAuthError())
            .isEqualTo(AuthError.WeakPassword)
        assertThat(FirebaseAuthUserCollisionException("ERROR_EMAIL_ALREADY_IN_USE", "used").toAuthError())
            .isEqualTo(AuthError.EmailInUse)
        assertThat(FirebaseAuthInvalidCredentialsException("ERROR_INVALID_CREDENTIAL", "bad").toAuthError())
            .isEqualTo(AuthError.InvalidCredentials)
        assertThat(FirebaseAuthInvalidUserException("ERROR_USER_NOT_FOUND", "none").toAuthError())
            .isEqualTo(AuthError.InvalidCredentials)
        assertThat(FirebaseAuthInvalidUserException("ERROR_USER_DISABLED", "off").toAuthError())
            .isEqualTo(AuthError.UserDisabled)
        assertThat(FirebaseAuthRecentLoginRequiredException("ERROR_REQUIRES_RECENT_LOGIN", "again").toAuthError())
            .isEqualTo(AuthError.RecentLoginRequired)
        assertThat(FirebaseAuthException("ERROR_OPERATION_NOT_ALLOWED", "disabled").toAuthError())
            .isEqualTo(AuthError.Unavailable)
        assertThat(FirebaseException("An internal error has occurred. [ CONFIGURATION_NOT_FOUND ]").toAuthError())
            .isEqualTo(AuthError.Unavailable)
        assertThat(FirebaseException("An internal error has occurred.").toAuthError()).isEqualTo(AuthError.Unknown)
        assertThat(IllegalStateException().toAuthError()).isEqualTo(AuthError.Unknown)
    }
}
