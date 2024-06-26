package com.example.budgetbuddy.utils

import android.util.Log

/**
 * Verificaciones para el registro de usuarios
 */
fun correctEmail(email: String): Boolean {
    val regex = Regex("""\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Z|a-z]{2,}\b""")
    return regex.matches(email)
}
fun correctName(name: String): Boolean{
    return name.isNotBlank() && name.any { it != ' ' }
}

fun correctPasswd(password: String, password2: String): Boolean{
    return password!="" && password == password2
}
