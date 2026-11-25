package com.saadm.zenith.ui.common

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.Toast

/** Puts [text] on the clipboard. Android 13 and later confirm this themselves. */
fun Context.copyText(text: String) {
    getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Zenith", text))
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) Toast.makeText(this, "Copied", Toast.LENGTH_SHORT).show()
}

/** Opens the system share sheet with [text]. Nothing is sent until the user picks an app and sends it. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}
