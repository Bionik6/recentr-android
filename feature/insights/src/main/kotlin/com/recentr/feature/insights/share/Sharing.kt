package com.recentr.feature.insights.share

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import com.recentr.feature.insights.R
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Where a share card goes (Figma 110 "Targets"). */
internal enum class ShareTarget(val packageName: String?) {
    Stories("com.instagram.android"),
    Post("com.twitter.android"),
    Facebook("com.facebook.katana"),
    CopyLink(null),
    Save(null),
}

/**
 * Shares, copies or saves a rendered share card. Images go through the module's FileProvider
 * (cache/share); a target app that is not installed falls back to the system share sheet.
 */
internal object Sharing {

    suspend fun send(context: Context, target: ShareTarget, card: Bitmap, text: String) {
        when (target) {
            ShareTarget.CopyLink -> copy(context, text)
            ShareTarget.Save -> save(context, card)
            else -> share(context, card, text, target.packageName)
        }
    }

    private suspend fun share(context: Context, card: Bitmap, text: String, packageName: String?) {
        val uri = withContext(Dispatchers.IO) { cacheImage(context, card) }
        val send = Intent(Intent.ACTION_SEND)
            .setType("image/png")
            .putExtra(Intent.EXTRA_STREAM, uri)
            .putExtra(Intent.EXTRA_TEXT, text)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        send.clipData = ClipData.newRawUri(null, uri)
        val direct = packageName?.takeIf { context.packageManager.getLaunchIntentForPackage(it) != null }
        try {
            if (direct != null) {
                context.startActivity(Intent(send).setPackage(direct))
                return
            }
        } catch (_: ActivityNotFoundException) {
            // The app cannot take an image: use the share sheet.
        }
        context.startActivity(Intent.createChooser(send, null))
    }

    private fun copy(context: Context, text: String) {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText(context.getString(R.string.share_title), text))
        // Android 13+ confirms the copy itself.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            Toast.makeText(context, R.string.share_copied, Toast.LENGTH_SHORT).show()
        }
    }

    private suspend fun save(context: Context, card: Bitmap) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            // Writing to the shared gallery needs a storage permission before Android 10.
            share(context, card, "", null)
            return
        }
        val saved = withContext(Dispatchers.IO) {
            runCatching {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, "recentr-${System.currentTimeMillis()}.png")
                    put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/Recentr")
                }
                val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: error("No media store")
                resolver.openOutputStream(uri)?.use { card.compress(Bitmap.CompressFormat.PNG, 100, it) } ?: error("No stream")
            }.isSuccess
        }
        Toast.makeText(context, if (saved) R.string.share_saved else R.string.share_save_failed, Toast.LENGTH_SHORT).show()
    }

    private fun cacheImage(context: Context, card: Bitmap): Uri {
        val directory = File(context.cacheDir, "share").apply { mkdirs() }
        val file = File(directory, "recentr-share.png")
        file.outputStream().use { card.compress(Bitmap.CompressFormat.PNG, 100, it) }
        return FileProvider.getUriForFile(context, "${context.packageName}.insights.share", file)
    }
}
