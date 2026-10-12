package com.naury.chageun.core.ui.sound

import com.naury.framekit.android.input.EditorInput
import com.naury.framekit.android.input.MediaKind
import com.naury.framekit.ui.audioeditor.contract.AudioEditorConfig
import com.naury.framekit.ui.audioeditor.contract.AudioEditorRequest
import com.naury.framekit.ui.audioeditor.contract.AudioExportConfig
import com.naury.framekit.ui.audioeditor.contract.AudioTool
import com.naury.framekit.ui.audioeditor.contract.RecordMode
import com.naury.framekit.ui.config.EditorUiConfig
import java.io.File

/** 녹음 편집기에 넘길 소리. FrameKit 타입을 모듈 밖에 드러내지 않으려고 따로 둔다. */
internal sealed interface SoundEditSource {
    /** 녹음 화면에서 바로 녹음한 뒤 편집한다. */
    data object Record : SoundEditSource

    /** 시스템 파일 선택기로 소리 파일 하나를 고른 뒤 편집한다. */
    data object Pick : SoundEditSource

    /** 앱 저장소의 소리 파일을 다시 편집한다. */
    data class Existing(val path: String) : SoundEditSource
}

/**
 * [source]를 여는 녹음 편집기 요청. 차 옆에서 휴대폰을 들고 녹음하므로 누르고 있지 않아도 되는 TAP 방식만 두고,
 * 녹음은 [maxRecordingMs]에 닿으면 저절로 멈춘다. 녹음 뒤에는 앞뒤를 잘라 낼 수 있게 편집 화면으로 간다.
 * 도구는 자르기·볼륨·나누기만 두어 정비소에 들려줄 부분만 남기기 쉽게 한다.
 */
internal fun soundEditRequest(source: SoundEditSource, maxRecordingMs: Long, ui: EditorUiConfig): AudioEditorRequest {
    val input = when (source) {
        SoundEditSource.Record -> EditorInput.Capture(MediaKind.AUDIO)
        SoundEditSource.Pick -> EditorInput.Pick(MediaKind.AUDIO, 1)
        is SoundEditSource.Existing -> EditorInput.FileSource(File(source.path).absolutePath)
    }
    return AudioEditorRequest(
        input = input,
        config = AudioEditorConfig(
            recordMode = RecordMode.TAP,
            maxRecordingMs = maxRecordingMs.coerceIn(MIN_RECORDING_MS, MAX_RECORDING_MS),
            editAfterRecording = true,
            enabledTools = SOUND_TOOLS,
            maxSourceCount = 1,
        ),
        export = AudioExportConfig(bitrate = SOUND_BITRATE),
        ui = ui,
    )
}

internal val SOUND_TOOLS = setOf(AudioTool.RANGE, AudioTool.VOLUME, AudioTool.SPLIT)

// 엔진·브레이크 소리를 알아듣기에 충분하고 3분이 2MB 남짓이다.
internal const val SOUND_BITRATE = 96_000

// FrameKit이 받는 녹음 길이 범위(1초~60분).
private const val MIN_RECORDING_MS = 1_000L
private const val MAX_RECORDING_MS = 60 * 60 * 1_000L
