package com.naury.chageun.feature.history

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.naury.chageun.core.model.Attachment
import java.io.File

/** 소리 하나를 재생하는 엔진. 실제 플레이어 없이 화면을 테스트할 수 있도록 추상화했다. */
internal interface SoundEngine {
    val positionMs: Long

    /**
     * [path]를 처음부터 재생한다. 다른 앱이 소리를 가져가거나 이어폰을 빼서 저절로 멈추면 [onPaused],
     * 끝까지 들으면 [onEnded]가 불린다.
     */
    fun play(path: String, onPaused: () -> Unit, onEnded: () -> Unit)

    fun pause()

    fun resume()

    fun release()
}

/** 화면 테스트에서 실제 플레이어 대신 넣을 수 있게 엔진 만드는 법을 둔다. */
internal val LocalSoundEngineFactory = staticCompositionLocalOf<(Context) -> SoundEngine> { ::ExoSoundEngine }

/**
 * 상세 화면에서 한 번에 소리 하나만 재생한다. 다른 소리를 누르면 앞의 것은 멈춘다.
 * 플레이어는 처음 재생할 때 만들고, 화면을 떠나면 놓는다.
 */
@Stable
internal class SoundPlayerState(private val createEngine: () -> SoundEngine) {
    /** 재생 중이거나 잠시 멈춘 소리. 처음 위치로 돌아가면 null이다. */
    var currentId by mutableStateOf<String?>(null)
        private set
    var isPlaying by mutableStateOf(false)
        private set

    private var currentPath: String? = null
    private var engine: SoundEngine? = null

    val positionMs: Long get() = engine?.positionMs ?: 0L

    fun isPlaying(sound: Attachment) = isPlaying && currentId == sound.id

    fun toggle(sound: Attachment) {
        val isCurrent = currentId == sound.id && currentPath == sound.filePath
        when {
            isCurrent && isPlaying -> pause()
            isCurrent -> {
                engine?.resume()
                isPlaying = true
            }
            else -> start(sound)
        }
    }

    fun pause() {
        engine?.pause()
        isPlaying = false
    }

    /** 재생하던 소리가 지워졌거나 다시 편집돼 파일이 바뀌었으면 멈추고 처음으로 돌린다. */
    fun sync(sounds: List<Attachment>) {
        val id = currentId ?: return
        if (sounds.none { it.id == id && it.filePath == currentPath }) reset()
    }

    fun release() {
        engine?.release()
        engine = null
        currentId = null
        currentPath = null
        isPlaying = false
    }

    private fun start(sound: Attachment) {
        val player = engine ?: createEngine().also { engine = it }
        currentId = sound.id
        currentPath = sound.filePath
        isPlaying = true
        player.play(sound.filePath, onPaused = { isPlaying = false }, onEnded = ::reset)
    }

    private fun reset() {
        engine?.pause()
        currentId = null
        currentPath = null
        isPlaying = false
    }
}

/** 상세 화면이 사라지면 플레이어를 놓고, 앱을 잠시 떠나면 소리가 계속 나지 않게 멈춘다. */
@Composable
internal fun rememberSoundPlayer(): SoundPlayerState {
    val context = LocalContext.current.applicationContext
    val factory = LocalSoundEngineFactory.current
    val state = remember(factory) { SoundPlayerState { factory(context) } }
    DisposableEffect(state) { onDispose { state.release() } }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { state.pause() }
    return state
}

/**
 * Media3 ExoPlayer로 앱 저장소의 m4a를 재생한다. 다른 앱 소리와 번갈아 나도록 오디오 포커스를 따르고,
 * 이어폰을 빼면 멈춘다.
 */
private class ExoSoundEngine(context: Context) : SoundEngine {
    private var onPaused: () -> Unit = {}
    private var onEnded: () -> Unit = {}
    private val player = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
            true,
        )
        .setHandleAudioBecomingNoisy(true)
        .build()
        .apply {
            addListener(
                object : Player.Listener {
                    override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                        if (!playWhenReady && reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) onPaused()
                    }

                    override fun onPlaybackStateChanged(playbackState: Int) {
                        if (playbackState == Player.STATE_ENDED) onEnded()
                    }
                },
            )
        }

    override val positionMs: Long get() = player.currentPosition

    override fun play(path: String, onPaused: () -> Unit, onEnded: () -> Unit) {
        this.onPaused = onPaused
        this.onEnded = onEnded
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
        player.prepare()
        player.playWhenReady = true
    }

    override fun pause() {
        player.pause()
    }

    override fun resume() {
        if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
        player.play()
    }

    override fun release() {
        player.release()
    }
}
