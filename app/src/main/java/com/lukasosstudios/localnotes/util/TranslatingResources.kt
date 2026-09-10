package com.lukasosstudios.localnotes.util

import android.content.res.Resources

class TranslatingResources(base: Resources) :
    Resources(base.assets, base.displayMetrics, base.configuration) {

    private fun overrideFor(id: Int): String? {
        val name = try {
            getResourceEntryName(id)
        } catch (e: Exception) {
            null
        } ?: return null
        return TranslationManager.currentOverrides()[name]
    }

    override fun getString(id: Int): String {
        return overrideFor(id) ?: super.getString(id)
    }

    override fun getString(id: Int, vararg formatArgs: Any?): String {
        val template = overrideFor(id) ?: return super.getString(id, *formatArgs)
        return try {
            String.format(template, *formatArgs)
        } catch (e: Exception) {
            super.getString(id, *formatArgs)
        }
    }

    override fun getText(id: Int): CharSequence {
        return overrideFor(id) ?: super.getText(id)
    }

    override fun getText(id: Int, def: CharSequence): CharSequence {
        return overrideFor(id) ?: super.getText(id, def)
    }
}
