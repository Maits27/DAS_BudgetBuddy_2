package com.example.budgetbuddy.utils

import android.util.Log
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.example.budgetbuddy.Local.Data.AppLanguage
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


/*************************************************
 **            App's Language Manager           **
 *************************************************/

/**
 * Clase de modificación del idioma actual
 *
 * Se le ha establecido como @Singleton de Hilt para crear una única instancia en toda la APP
 */
@Singleton
class LanguageManager @Inject constructor() {

    // Idioma actual de la APP
    var currentLang: AppLanguage = AppLanguage.getFromCode(Locale.getDefault().language.lowercase())

    // Método que cambia el idioma de la aplicación de forma local e instantánea
    fun changeLang(lang: AppLanguage) {
        currentLang = lang
        val localeList = LocaleListCompat.forLanguageTags(lang.code)
        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
