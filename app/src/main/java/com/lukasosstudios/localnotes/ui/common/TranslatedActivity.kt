package com.lukasosstudios.localnotes.ui.common

import android.content.Context
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import com.lukasosstudios.localnotes.util.TranslatingResources
import com.lukasosstudios.localnotes.util.TranslationManager

abstract class TranslatedActivity : AppCompatActivity() {

    private var cachedBase: Resources? = null
    private var cachedWrapped: TranslatingResources? = null

    override fun attachBaseContext(newBase: Context) {
        TranslationManager.ensureLoaded(newBase)
        super.attachBaseContext(newBase)
    }

    override fun getResources(): Resources {
        val base = super.getResources()
        val wrapped = cachedWrapped
        if (wrapped != null && base === cachedBase) return wrapped

        val fresh = TranslatingResources(base)
        cachedBase = base
        cachedWrapped = fresh
        return fresh
    }
}
