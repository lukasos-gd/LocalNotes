package com.lukasosstudios.localnotes.ui.common

import android.content.Context
import android.content.ContextWrapper
import android.content.res.Resources
import androidx.appcompat.app.AppCompatActivity
import com.lukasosstudios.localnotes.util.TranslatingResources
import com.lukasosstudios.localnotes.util.TranslationManager

abstract class TranslatedActivity : AppCompatActivity() {

    override fun attachBaseContext(newBase: Context) {
        TranslationManager.ensureLoaded(newBase)
        val wrapped = object : ContextWrapper(newBase) {
            private val translatingResources: Resources by lazy { TranslatingResources(newBase.resources) }
            override fun getResources(): Resources = translatingResources
        }
        super.attachBaseContext(wrapped)
    }
}
