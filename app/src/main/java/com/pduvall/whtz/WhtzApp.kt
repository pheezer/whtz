package com.pduvall.whtz

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import dagger.hilt.android.HiltAndroidApp
import okhttp3.OkHttpClient

@HiltAndroidApp
class WhtzApp : Application(), SingletonImageLoader.Factory {
    // Coil loads card art from Scryfall's image CDN, which rejects the default "okhttp/..."
    // User-Agent with HTTP 400, so we give it a client that sends a descriptive User-Agent.
    override fun newImageLoader(context: PlatformContext): ImageLoader {
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", "Whtz/0.1 (personal playtest app)")
                        .build(),
                )
            }
            .build()
        return ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { client })) }
            .build()
    }
}
