package com.lukasosstudios.localnotes.ui.common

import android.app.Activity
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.lukasosstudios.localnotes.R
import com.lukasosstudios.localnotes.databinding.DialogConfirmBinding

/**
 * A confirmation dialog styled like the rest of the app (rounded card,
 * app colors, custom buttons) instead of the stock platform AlertDialog.
 */
object ConfirmDialog {

    fun show(
        activity: Activity,
        title: String,
        message: String,
        icon: Int = R.drawable.ic_trash,
        destructive: Boolean = false,
        positiveLabel: String,
        negativeLabel: String? = activity.getString(R.string.cancel),
        cancelable: Boolean = true,
        onConfirm: () -> Unit
    ) {
        val binding = DialogConfirmBinding.inflate(LayoutInflater.from(activity))
        val dialog = Dialog(activity)
        dialog.setContentView(binding.root)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(cancelable)

        binding.dialogTitle.text = title
        binding.dialogMessage.text = message
        binding.dialogIcon.setImageResource(icon)

        val iconBg = if (destructive) R.drawable.bg_dialog_icon_destructive else R.drawable.bg_dialog_icon
        binding.dialogIconFrame.setBackgroundResource(iconBg)
        val iconTint = if (destructive) R.color.destructive else R.color.accentForeground
        binding.dialogIcon.setColorFilter(ContextCompat.getColor(activity, iconTint))

        binding.dialogPositiveLabel.text = positiveLabel
        binding.dialogPositiveButton.setBackgroundResource(
            if (destructive) R.drawable.bg_dialog_button_destructive else R.drawable.bg_dialog_button_primary
        )
        binding.dialogPositiveButton.setOnClickListener {
            dialog.dismiss()
            onConfirm()
        }

        if (negativeLabel == null) {
            binding.dialogNegativeButton.visibility = View.GONE
        } else {
            binding.dialogNegativeLabel.text = negativeLabel
            binding.dialogNegativeButton.setOnClickListener { dialog.dismiss() }
        }

        dialog.show()
    }
}
