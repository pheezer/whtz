package com.pduvall.whtz.data.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import com.pduvall.whtz.R
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

enum class Sfx { DRAW, SHUFFLE, SWOOSH, BUBBLE }

/** Low-latency one-shot sound effects via SoundPool. Clips live in res/raw. */
@Singleton
class SoundManager @Inject constructor(@ApplicationContext context: Context) {

    private val pool = SoundPool.Builder()
        .setMaxStreams(4)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded = HashSet<Int>()
    private val ids: Map<Sfx, Int>

    init {
        // Register the listener before loading so no completion is missed.
        pool.setOnLoadCompleteListener { _, sampleId, status ->
            if (status == 0) synchronized(loaded) { loaded += sampleId }
        }
        ids = mapOf(
            Sfx.DRAW to pool.load(context, R.raw.draw_sound, 1),
            Sfx.SHUFFLE to pool.load(context, R.raw.shuffle, 1),
            Sfx.SWOOSH to pool.load(context, R.raw.swoosh, 1),
            Sfx.BUBBLE to pool.load(context, R.raw.double_bubble, 1),
        )
    }

    fun play(sfx: Sfx) {
        val id = ids[sfx] ?: return
        val ready = synchronized(loaded) { id in loaded }
        if (ready) pool.play(id, 1f, 1f, 1, 0, 1f)
    }
}
