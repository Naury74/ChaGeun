package com.naury.chageun.flow

import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.compose.ui.test.printToString
import androidx.test.ext.junit.rules.ActivityScenarioRule
import com.naury.chageun.MainActivity
import com.naury.chageun.R
import com.naury.chageun.core.model.FuelType
import com.naury.chageun.core.model.MaintenanceItem
import com.naury.chageun.core.ui.R as UiR
import com.naury.chageun.core.ui.labelRes
import com.naury.chageun.feature.manage.R as ManageR
import com.naury.chageun.feature.onboarding.R as OnboardingR
import java.text.NumberFormat
import java.time.Year

/** 온보딩으로 등록한 차량. 화면에 보이는 값과 비교할 때 쓴다. */
data class RegisteredVehicle(
    val maker: String,
    val model: String,
    val modelYear: Int,
    val fuel: String,
    val plate: String?,
    val mileage: Long,
) {
    val title: String get() = "$maker $model"
}

/**
 * 실제 MainActivity를 사용자처럼 조작한다.
 *
 * 문구는 Activity의 리소스에서 읽는다. 기기 언어나 앱별 언어가 무엇이든 화면과 같은 문자열로 노드를 찾는다.
 * 저장은 Room에서 비동기로 끝나므로 화면 전환은 [waitFor]로 기다리고 고정 시간 대기는 쓰지 않는다.
 */
class AppRobot(private val rule: AndroidComposeTestRule<ActivityScenarioRule<MainActivity>, MainActivity>) {

    private val activity: MainActivity get() = rule.activity

    fun text(@StringRes id: Int, vararg args: Any): String = activity.getString(id, *args)

    /** 앱과 같은 언어 설정으로 천 단위 구분 기호를 붙인다. */
    fun number(value: Long): String =
        NumberFormat.getIntegerInstance(activity.resources.configuration.locales[0]).format(value)

    fun itemName(item: MaintenanceItem): String = text(item.labelRes)

    // 연식 칩 같은 가로 목록을 끝까지 넘기면 이미 고른 칩이 화면 밖으로 밀리므로 세로 목록만 스크롤한다.
    private val verticalList =
        hasScrollToNodeAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)
    private val horizontalList = SemanticsMatcher.keyIsDefined(SemanticsProperties.HorizontalScrollAxisRange)

    /** 칩·카드처럼 고르는 항목을 누르고 실제로 선택됐는지 확인한다. */
    fun select(label: String) {
        clickText(label)
        waitFor(hasText(label) and isSelected())
    }

    fun exists(matcher: SemanticsMatcher): Boolean = rule.onAllNodes(matcher).fetchSemanticsNodes().isNotEmpty()

    fun waitFor(matcher: SemanticsMatcher, timeoutMillis: Long = TIMEOUT_MILLIS) {
        try {
            rule.waitUntil(timeoutMillis) { exists(matcher) }
        } catch (timeout: ComposeTimeoutException) {
            dumpScreen()
            throw timeout
        }
    }

    /** CI 기기에서는 화면을 볼 수 없으므로 기다리다 실패한 순간의 화면 구조를 logcat에 남긴다. */
    private fun dumpScreen() {
        val roots = rule.onAllNodes(isRoot())
        // 한 번에 남기면 logcat 한 줄 길이 제한에 잘리므로 줄마다 나눠 남긴다.
        roots.fetchSemanticsNodes().indices.forEach { index ->
            roots[index].printToString().lineSequence().forEach { Log.d(TREE_LOG_TAG, it) }
        }
    }

    /** Lazy 목록 아래쪽 항목은 스크롤해야 그려지므로 기다리는 동안 목록을 내려 본다. */
    fun waitForScrolling(matcher: SemanticsMatcher, timeoutMillis: Long = TIMEOUT_MILLIS) {
        try {
            rule.waitUntil(timeoutMillis) { exists(matcher) || scrollListsTo(matcher) }
        } catch (timeout: ComposeTimeoutException) {
            dumpScreen()
            throw timeout
        }
    }

    private fun scrollListsTo(matcher: SemanticsMatcher): Boolean {
        val lists = rule.onAllNodes(verticalList)
        for (index in 0 until lists.fetchSemanticsNodes().size) {
            runCatching { lists[index].performScrollToNode(matcher) }
            if (exists(matcher)) return true
        }
        return false
    }

    fun waitForText(text: String, timeoutMillis: Long = TIMEOUT_MILLIS) = waitFor(hasText(text), timeoutMillis)

    fun waitUntilGone(matcher: SemanticsMatcher, timeoutMillis: Long = TIMEOUT_MILLIS) {
        rule.waitUntil(timeoutMillis) { !exists(matcher) }
    }

    /**
     * 노드를 화면 안으로 가져온다. Lazy 목록의 항목은 아직 그려지지 않았을 수 있으므로
     * 화면의 Lazy 목록마다 그 항목까지 스크롤해 본다.
     */
    fun bringIntoView(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        if (!exists(matcher)) {
            val lists = rule.onAllNodes(verticalList)
            val count = lists.fetchSemanticsNodes().size
            for (index in 0 until count) {
                runCatching { lists[index].performScrollToNode(matcher) }
                if (exists(matcher)) break
            }
        }
        val node = rule.onAllNodes(matcher).onFirst()
        // 가로 목록 안의 항목은 performScrollTo가 가로로만 움직인다. 화면이 짧으면 그 줄이 하단 고정 버튼 뒤에
        // 가려진 채로 눌리므로, 먼저 가로 목록을 세로 화면 안으로 가져온다.
        val row = horizontalList and hasAnyDescendant(matcher) and hasAnyAncestor(hasScrollAction())
        if (exists(row)) rule.onAllNodes(row).onFirst().performScrollTo()
        // 스크롤하지 않는 영역(하단 버튼, 내비게이션 바)에 있는 노드는 그대로 둔다.
        if (exists(matcher and hasAnyAncestor(hasScrollAction()))) node.performScrollTo()
        return node.assertExists()
    }

    fun click(matcher: SemanticsMatcher) {
        waitForScrolling(matcher)
        bringIntoView(matcher).performClick()
    }

    fun clickText(text: String) = click(hasText(text))

    fun clickText(@StringRes id: Int, vararg args: Any) = clickText(text(id, *args))

    /** 내비게이션 바(넓은 창에서는 레일)의 탭. 같은 글자의 화면 제목과 구분하려고 선택 가능한 노드만 찾는다. */
    fun openTab(@StringRes labelRes: Int) = click(hasText(text(labelRes)) and isSelectable())

    fun inputText(field: SemanticsMatcher, value: String) {
        waitFor(field)
        bringIntoView(field).performTextReplacement(value)
    }

    /**
     * 온보딩을 끝까지 진행해 차량을 등록하고 홈이 뜰 때까지 기다린다.
     * [plate]가 null이면 번호판 없이 등록한다.
     */
    fun registerVehicle(plate: String? = TEST_PLATE, mileage: Long = TEST_MILEAGE): RegisteredVehicle {
        clickText(OnboardingR.string.onboarding_start)

        if (plate != null) {
            val plateField =
                hasSetTextAction() and hasContentDescription(text(OnboardingR.string.onboarding_plate_field))
            waitFor(plateField)
            rule.onNode(plateField).performTextInput(plate)
            clickText(OnboardingR.string.onboarding_next)
        } else {
            clickText(OnboardingR.string.onboarding_plate_skip)
        }

        val maker = text(UiR.string.maker_hyundai)
        select(maker)
        val model = activity.resources.getStringArray(UiR.array.models_hyundai).first()
        select(model)
        // 연식 칩은 가로 Lazy 목록이라 처음부터 보이는 최근 연식을 고른다.
        val modelYear = Year.now().value - 1
        select(modelYear.toString())
        val fuel = text(FuelType.Gasoline.labelRes)
        select(fuel)
        clickText(OnboardingR.string.onboarding_next)

        clickText(OnboardingR.string.onboarding_photo_later)

        inputText(
            hasSetTextAction() and hasContentDescription(text(OnboardingR.string.onboarding_mileage_field)),
            mileage.toString(),
        )
        clickText(OnboardingR.string.onboarding_next)

        // 빠른 정비 입력은 모두 '모름'으로 둔다.
        waitForText(text(OnboardingR.string.onboarding_quick_title))
        clickText(OnboardingR.string.onboarding_next)

        // 시스템 권한 창이 테스트를 가리지 않도록 알림은 나중에 켠다.
        clickText(OnboardingR.string.onboarding_notifications_later)

        waitFor(hasText(text(R.string.nav_manage)) and isSelectable(), REGISTER_TIMEOUT_MILLIS)
        return RegisteredVehicle(maker, model, modelYear, fuel, plate, mileage)
    }

    /** 관리 탭에서 항목 상세를 연다. 좁은 창은 목록 대신 상세가, 넓은 창은 목록 옆에 상세가 보인다. */
    fun openManageItem(item: MaintenanceItem) {
        openTab(R.string.nav_manage)
        click(hasText(itemName(item)) and hasClickAction())
        waitFor(recordActionButton)
    }

    /** 상세의 '교체했어요'로 교체 기록 시트를 열고 주행거리를 넣는다. */
    fun startServiceRecord(item: MaintenanceItem, mileage: Long) {
        click(recordActionButton)
        waitForText(text(ManageR.string.record_title, itemName(item)))
        inputText(hasSetTextAction() and hasText(text(ManageR.string.record_mileage)), mileage.toString())
    }

    fun saveServiceRecord() = click(hasText(text(ManageR.string.record_save)) and hasClickAction())

    /** 저장 완료 화면을 확인하고 시트를 닫는다. */
    fun finishSavedRecord(item: MaintenanceItem) {
        waitForText(text(ManageR.string.record_saved_title, itemName(item)))
        clickText(ManageR.string.record_done)
        waitUntilGone(hasText(text(ManageR.string.record_saved_title, itemName(item))))
    }

    val recordActionButton: SemanticsMatcher
        get() = hasText(text(ManageR.string.record_action)) and hasClickAction()

    companion object {
        /** API 26 에뮬레이터는 첫 Compose 화면과 Room 쓰기가 느려 넉넉히 기다린다. */
        const val TIMEOUT_MILLIS = 15_000L
        private const val TREE_LOG_TAG = "ChageunScreen"
        const val REGISTER_TIMEOUT_MILLIS = 30_000L
        const val TEST_PLATE = "12가3456"
        const val TEST_MILEAGE = 50_000L
    }
}
