package com.ailua.wowbis

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache

object IconLoader {
    private val cache = LruCache<String, Bitmap>(96)

    fun load(ctx: Context, name: String?): Bitmap? {
        if (name.isNullOrEmpty()) return null
        cache.get(name)?.let { return it }
        return try {
            ctx.assets.open("icons/$name.jpg").use { input ->
                val bmp = BitmapFactory.decodeStream(input)
                if (bmp != null) cache.put(name, bmp)
                bmp
            }
        } catch (e: Exception) {
            null
        }
    }
}
