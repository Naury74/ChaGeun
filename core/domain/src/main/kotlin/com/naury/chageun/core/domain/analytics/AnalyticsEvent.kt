package com.naury.chageun.core.domain.analytics

import com.naury.chageun.core.model.AuthMethod

/**
 * 기획서 29.1 제품 이벤트.
 *
 * 파라미터는 enum·Boolean·정수에서만 만든다. 자유 문자열을 받는 생성자가 없으므로 차량번호, VIN, 소유주명,
 * 질문·메모 원문, 이미지 URI 같은 24.4 금지값이 구조적으로 들어갈 수 없다.
 */
sealed interface AnalyticsEvent {
    val name: String
    val params: Map<String, String> get() = emptyMap()

    data object OnboardingStarted : AnalyticsEvent {
        override val name = "onboarding_started"
    }

    /** V1은 공식 조회가 없어 모든 등록이 수동이다 (ADR-004). 자동 조회가 붙으면 비율 비교에 쓴다. */
    data object ManualRegistrationUsed : AnalyticsEvent {
        override val name = "vehicle_manual_registration_used"
    }

    data class OnboardingCompleted(val withPlate: Boolean, val knownServiceCount: Int) : AnalyticsEvent {
        override val name = "onboarding_completed"
        override val params = mapOf(
            "with_plate" to withPlate.toString(),
            "known_services" to knownServiceCount.toString(),
        )
    }

    data class HomeActionOpened(val action: HomeAction) : AnalyticsEvent {
        override val name = "home_action_opened"
        override val params = mapOf("action" to action.key)
    }

    data class MaintenanceRecordAdded(val withCost: Boolean) : AnalyticsEvent {
        override val name = "maintenance_record_added"
        override val params = mapOf("with_cost" to withCost.toString())
    }

    data object FuelRecordAdded : AnalyticsEvent {
        override val name = "fuel_record_added"
    }

    data object CheckRecordAdded : AnalyticsEvent {
        override val name = "check_record_added"
    }

    data class MileageUpdated(val isCorrection: Boolean) : AnalyticsEvent {
        override val name = "mileage_updated"
        override val params = mapOf("is_correction" to isCorrection.toString())
    }

    data class ReminderEnabled(val kind: ReminderKind) : AnalyticsEvent {
        override val name = "reminder_enabled"
        override val params = mapOf("kind" to kind.key)
    }

    data object RecallOpened : AnalyticsEvent {
        override val name = "recall_opened"
    }

    data object AiQuestionStarted : AnalyticsEvent {
        override val name = "ai_question_started"
    }

    data class AiShareCompleted(val target: AiTarget) : AnalyticsEvent {
        override val name = "ai_share_completed"
        override val params = mapOf("target" to target.key)
    }

    /** GA4 권장 이벤트 이름을 그대로 쓴다. 이메일·UID는 보내지 않는다. */
    data class SignUp(val method: AuthMethod) : AnalyticsEvent {
        override val name = "sign_up"
        override val params = mapOf("method" to method.key)
    }

    data class Login(val method: AuthMethod) : AnalyticsEvent {
        override val name = "login"
        override val params = mapOf("method" to method.key)
    }

    data object CloudBackupCreated : AnalyticsEvent {
        override val name = "cloud_backup_created"
    }

    data object CloudBackupRestored : AnalyticsEvent {
        override val name = "cloud_backup_restored"
    }
}

private val AuthMethod.key: String
    get() = when (this) {
        AuthMethod.Email -> "email"
        AuthMethod.Google -> "google"
    }

enum class HomeAction(val key: String) {
    RecordService("record_service"),
    UpdateMileage("update_mileage"),
    OpenHistory("open_history"),
    AskAi("ask_ai"),
    OpenInspection("open_inspection"),
}

enum class ReminderKind(val key: String) { MaintenanceAndInspection("maintenance"), Mileage("mileage") }

enum class AiTarget(val key: String) { ChatGpt("chatgpt"), Claude("claude"), Gemini("gemini"), Other("other") }

interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

/** 외부 전송 SDK가 없을 때와 테스트·Preview에서 쓴다. */
object NoOpAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}
