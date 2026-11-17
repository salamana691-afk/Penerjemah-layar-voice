package com.autodub.edge.engine.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.autodub.edge.core.constants.AppConstants
import com.autodub.edge.domain.model.AudioQueuePolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class QueuedAudioTrack(
    val id: String,
    val file: File,
    val speakerName: String,
    val volume: Float,
    val speed: Float
)

data class AudioPlaybackState(
    val isPlaying: Boolean = false,
    val isPaused: Boolean = false,
    val currentSpeaker: String = "",
    val queueSize: Int = 0,
    val lastError: String? = null
)

class AudioPlaybackManager(
    private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var player: ExoPlayer? = null
    private val queue = ArrayDeque<QueuedAudioTrack>()
    private var activeTrack: QueuedAudioTrack? = null

    private val _state = MutableStateFlow(AudioPlaybackState())
    val state: StateFlow<AudioPlaybackState> = _state.asStateFlow()

    private fun ensurePlayer(): ExoPlayer {
        val existing = player
        if (existing != null) return existing

        return ExoPlayer.Builder(context.applicationContext).build().apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        onTrackFinished()
                    }
                }

                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    _state.value = _state.value.copy(
                        isPlaying = isPlaying,
                        isPaused = !isPlaying && activeTrack != null && playbackState == Player.STATE_READY
                    )
                }

                override fun onPlayerError(error: PlaybackException) {
                    _state.value = _state.value.copy(
                        lastError = "Gagal memutar audio: ${error.errorCodeName}"
                    )
                    onTrackFinished()
                }
            })
            player = this
        }
    }

    fun enqueue(
        track: QueuedAudioTrack,
        policy: AudioQueuePolicy
    ) {
        scope.launch {
            when (policy) {
                AudioQueuePolicy.REPLACE_CURRENT -> {
                    clearQueueInternal()
                    activeTrack?.let { deleteTempFileIfNotCached(it.file) }
                    activeTrack = null
                    playTrackNow(track)
                }
                AudioQueuePolicy.FINISH_CURRENT -> {
                    if (activeTrack == null) {
                        playTrackNow(track)
                    } else {
                        clearQueueInternal()
                        queue.addLast(track)
                        publishState()
                    }
                }
                AudioQueuePolicy.QUEUE_ALL -> {
                    if (activeTrack == null) {
                        playTrackNow(track)
                    } else {
                        while (queue.size >= AppConstants.MAX_AUDIO_QUEUE_SIZE) {
                            val dropped = queue.removeFirst()
                            deleteTempFileIfNotCached(dropped.file)
                        }
                        queue.addLast(track)
                        publishState()
                    }
                }
            }
        }
    }

    private fun playTrackNow(track: QueuedAudioTrack) {
        if (!track.file.exists()) {
            onTrackFinished()
            return
        }
        val exo = ensurePlayer()
        activeTrack = track
        exo.volume = track.volume.coerceIn(0f, 1f)
        exo.playbackParameters = PlaybackParameters(track.speed.coerceIn(0.5f, 2.0f))
        val mediaItem = MediaItem.fromUri(Uri.fromFile(track.file))
        exo.setMediaItem(mediaItem)
        exo.prepare()
        exo.playWhenReady = true
        publishState(lastError = null)
    }

    private fun onTrackFinished() {
        activeTrack?.let { deleteTempFileIfNotCached(it.file) }
        activeTrack = null

        val next = if (queue.isNotEmpty()) queue.removeFirst() else null
        if (next != null) {
            playTrackNow(next)
        } else {
            publishState()
        }
    }

    fun pause() {
        scope.launch {
            player?.pause()
            _state.value = _state.value.copy(isPlaying = false, isPaused = activeTrack != null)
        }
    }

    fun resume() {
        scope.launch {
            if (activeTrack != null) {
                player?.play()
                _state.value = _state.value.copy(isPlaying = true, isPaused = false)
            }
        }
    }

    fun skipCurrent() {
        scope.launch {
            player?.stop()
            onTrackFinished()
        }
    }

    fun stopAndClear() {
        scope.launch {
            player?.stop()
            player?.clearMediaItems()
            clearQueueInternal()
            activeTrack?.let { deleteTempFileIfNotCached(it.file) }
            activeTrack = null
            publishState()
        }
    }

    fun updateVolume(volume: Float) {
        scope.launch {
            player?.volume = volume.coerceIn(0f, 1f)
        }
    }

    fun updatePlaybackSpeed(speed: Float) {
        scope.launch {
            player?.playbackParameters = PlaybackParameters(speed.coerceIn(0.5f, 2.0f))
        }
    }

    fun release() {
        scope.launch {
            clearQueueInternal()
            activeTrack?.let { deleteTempFileIfNotCached(it.file) }
            activeTrack = null
            player?.release()
            player = null
            publishState()
        }
    }

    private fun clearQueueInternal() {
        while (queue.isNotEmpty()) {
            deleteTempFileIfNotCached(queue.removeFirst().file)
        }
    }

    private fun deleteTempFileIfNotCached(file: File) {
        if (file.parentFile?.name == AppConstants.TTS_CACHE_DIR) {
            return
        }
        runCatching {
            if (file.exists()) {
                file.delete()
            }
        }
    }

    private fun publishState(lastError: String? = _state.value.lastError) {
        _state.value = AudioPlaybackState(
            isPlaying = activeTrack != null && (player?.isPlaying == true),
            isPaused = activeTrack != null && (player?.isPlaying == false),
            currentSpeaker = activeTrack?.speakerName ?: "",
            queueSize = queue.size,
            lastError = lastError
        )
    }
}
