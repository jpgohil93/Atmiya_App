package com.atmiya.innovation.utils

object StorageUtils {
    fun profilePhoto(userId: String): String = "profile_photos/$userId/profile.jpg"

    fun startupPitchDeckPdf(startupId: String, filename: String): String = "pitchDecks/$startupId/pdf/$filename"

    fun startupPitchDeckPpt(startupId: String, filename: String): String = "pitchDecks/$startupId/ppt/$filename"

    fun mentorVideoThumbnail(mentorId: String, filename: String): String = "mentor_thumbnails/$mentorId/$filename"
}
