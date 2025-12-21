package com.smartprivacy.scanner.utils

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

object HashUtils {
    fun getSha256(filePath: String): String? {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            val file = File(filePath)
            val inputStream = FileInputStream(file)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
            inputStream.close()
            val hashBytes = digest.digest()
            val sb = StringBuilder()
            for (b in hashBytes) {
                sb.append(String.format("%02x", b))
            }
            sb.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
