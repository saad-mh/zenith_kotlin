package com.saadm.zenith.ui.common

import android.content.Context
import android.content.Intent

/** Opens the system share sheet with [text]. Nothing is sent until the user picks an app and sends it. */
fun Context.shareText(text: String) {
    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
    startActivity(Intent.createChooser(send, null))
}
