package com.naury.chageun.feature.ai.ondevice

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class AnswerCleanupTest {

    @Test
    fun noRepetition_keepsText() {
        assertThat(cutRepetition("엔진오일은 다음 달에 바꾸면 돼요.\n점검 기록을 남겨 보세요.\n")).isNull()
    }

    @Test
    fun repeatedLine_cutsFromRepeat() {
        val text = "**엔진오일 교환 주기는 다음과 같습니다.**\n\n* 5,000km 주행 후 교환\n\n" +
            "엔진오일 교환 주기는 다음과 같습니다.\n* 5,000"
        assertThat(cutRepetition(text)).isEqualTo("**엔진오일 교환 주기는 다음과 같습니다.**\n\n* 5,000km 주행 후 교환")
    }

    @Test
    fun unfinishedAndShortLines_areIgnored() {
        assertThat(cutRepetition("네.\n네.\n엔진오일 교환 주기는 다음과 같습니다.\n엔진오일 교환 주기는 다음과")).isNull()
    }

    @Test
    fun unfinishedSentence_isDropped() {
        assertThat(dropUnfinishedSentence("엔진오일 기록이 아직 없어요. 기록을 남기면 시기를 알려 드려요. 급출발, 잦은"))
            .isEqualTo("엔진오일 기록이 아직 없어요. 기록을 남기면 시기를 알려 드려요.")
        assertThat(dropUnfinishedSentence("짧아요. 이 문장은 아주 길게 이어지다가 끝나지 않고 잘려 버린 경우예요 그래서"))
            .isEqualTo("짧아요. 이 문장은 아주 길게 이어지다가 끝나지 않고 잘려 버린 경우예요 그래서")
        assertThat(dropUnfinishedSentence("다 끝났어요.\n")).isEqualTo("다 끝났어요.")
        assertThat(dropUnfinishedSentence("정비 기록은 이래요.\n• 엔진오일: 기록 없음\n• 타이어: 기록 없음\n• 배터리: 20"))
            .isEqualTo("정비 기록은 이래요.\n• 엔진오일: 기록 없음\n• 타이어: 기록 없음")
    }
}
