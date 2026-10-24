package com.lucasmarujo.gitwallpaper.data.auth

interface SecretCipher {
    fun encrypt(plainText: String): String

    fun decrypt(cipherText: String): String
}
