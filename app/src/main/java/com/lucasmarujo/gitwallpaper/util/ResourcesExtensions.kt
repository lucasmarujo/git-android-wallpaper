package com.lucasmarujo.gitwallpaper.util

import android.content.res.Resources
import android.icu.text.MessageFormat
import androidx.annotation.StringRes
import java.util.Locale

val Resources.primaryLocale: Locale
    get() = configuration.locales[0]

fun Resources.formatMessage(@StringRes patternRes: Int, vararg arguments: Pair<String, Any>): String =
    MessageFormat(getString(patternRes), primaryLocale).format(arguments.toMap())
