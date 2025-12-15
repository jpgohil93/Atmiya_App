package com.atmiya.innovation.utils

object StorageUtils {
    fun profilePhoto(userId: String): String = "profile_photos/$userId/profile.jpg"

    fun startupPitchDeckPdf(startupId: String, filename: String): String = "pitchDecks/$startupId/pdf/$filename"

    fun startupPitchDeckPpt(startupId: String, filename: String): String = "pitchDecks/$startupId/ppt/$filename"

    fun mentorVideoThumbnail(mentorId: String, filename: String): String = "mentor_thumbnails/$mentorId/$filename"

    fun getFileNameFromUrl(url: String): String {
        try {
            val decodedUrl = java.net.URLDecoder.decode(url, "UTF-8")
            val path = decodedUrl.substringBefore("?")
            val filename = path.substringAfterLast("/")
            
            // Cleanup: Sometimes Firebase returns full path in name
            // If path contains "pitchDecks", we might want just everything after the last slash
            return if (filename.isNotBlank()) filename else "Document"
        } catch (e: Exception) {
            return "Document"
        }
    }
}
