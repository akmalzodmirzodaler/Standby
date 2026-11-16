package tj.mirzodaler.standby.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat

data class NowPlaying(
    val title: String,
    val artist: String,
    val app: String,
    val art: Bitmap?,
    val playing: Boolean,
    val positionMs: Long,      // позиция на момент lastUpdate
    val lastUpdate: Long,      // SystemClock.elapsedRealtime()
    val speed: Float,
    val durationMs: Long,
    private val controller: MediaController
) {
    /** Текущая позиция с учётом времени, прошедшего с последнего обновления. */
    fun livePosition(): Long {
        if (!playing) return positionMs
        val p = positionMs + ((SystemClock.elapsedRealtime() - lastUpdate) * speed).toLong()
        return if (durationMs > 0) p.coerceIn(0, durationMs) else p
    }

    fun playPause() = if (playing) controller.transportControls.pause() else controller.transportControls.play()
    fun next() = controller.transportControls.skipToNext()
    fun previous() = controller.transportControls.skipToPrevious()
}

object MediaAccess {
    fun isGranted(ctx: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(ctx).contains(ctx.packageName)
}

/** Следит за активными медиасессиями и отдаёт текущий трек. */
class MediaWatcher(
    private val ctx: Context,
    private val onChange: (NowPlaying?) -> Unit
) {
    private val msm = ctx.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(ctx, MediaListener::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var controller: MediaController? = null

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = publish()
        override fun onPlaybackStateChanged(state: PlaybackState?) = publish()
        override fun onSessionDestroyed() = refreshSessions()
    }

    private val sessionsListener =
        MediaSessionManager.OnActiveSessionsChangedListener { list -> pick(list.orEmpty()) }

    fun start() {
        if (!MediaAccess.isGranted(ctx)) {
            onChange(null); return
        }
        runCatching {
            msm.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
        }
        refreshSessions()
    }

    fun stop() {
        runCatching { msm.removeOnActiveSessionsChangedListener(sessionsListener) }
        controller?.unregisterCallback(callback)
        controller = null
    }

    private fun refreshSessions() {
        val list = runCatching { msm.getActiveSessions(component) }.getOrDefault(emptyList())
        pick(list)
    }

    /** Предпочитаем то, что играет сейчас; иначе — последнюю активную сессию. */
    private fun pick(list: List<MediaController>) {
        val best = list.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: list.firstOrNull()
        if (best?.sessionToken != controller?.sessionToken) {
            controller?.unregisterCallback(callback)
            controller = best
            best?.registerCallback(callback, handler)
        }
        publish()
    }

    private fun publish() {
        val c = controller
        val meta = c?.metadata
        if (c == null || meta == null) {
            onChange(null); return
        }
        val state = c.playbackState
        onChange(
            NowPlaying(
                title = meta.getString(MediaMetadata.METADATA_KEY_TITLE).orEmpty(),
                artist = meta.getString(MediaMetadata.METADATA_KEY_ARTIST)
                    ?: meta.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST).orEmpty(),
                app = appName(c.packageName),
                art = meta.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                    ?: meta.getBitmap(MediaMetadata.METADATA_KEY_ART),
                playing = state?.state == PlaybackState.STATE_PLAYING,
                positionMs = state?.position ?: 0,
                lastUpdate = state?.lastPositionUpdateTime ?: SystemClock.elapsedRealtime(),
                speed = state?.playbackSpeed?.takeIf { it > 0f } ?: 1f,
                durationMs = meta.getLong(MediaMetadata.METADATA_KEY_DURATION),
                controller = c
            )
        )
    }

    private fun appName(pkg: String): String = when (pkg) {
        "com.spotify.music" -> "Spotify"
        "com.google.android.apps.youtube.music" -> "YouTube Music"
        "com.google.android.youtube" -> "YouTube"
        "com.yandex.music", "ru.yandex.music" -> "Яндекс Музыка"
        "com.sec.android.app.music" -> "Samsung Music"
        else -> runCatching {
            ctx.packageManager.getApplicationLabel(ctx.packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }
}
