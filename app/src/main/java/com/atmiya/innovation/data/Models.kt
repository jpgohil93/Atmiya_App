package com.atmiya.innovation.data

import com.google.firebase.Timestamp

// --- User & Roles ---

// --- User & Roles ---

data class User(
    val uid: String = "",
    val phoneNumber: String = "",
    val role: String = "", // "startup", "investor", "mentor", "admin"
    val name: String = "",
    val email: String = "",
    val city: String = "",
    val region: String = "",
    val profilePhotoUrl: String? = null,
    val startupCategory: String = "", // For notification targeting
    val participantId: String? = null, // E.g., AIF-EDP-001
    @get:com.google.firebase.firestore.PropertyName("isOnboardingComplete")
    val isOnboardingComplete: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isBlocked")
    val isBlocked: Boolean = false,
    @get:com.google.firebase.firestore.PropertyName("isDeleted")
    val isDeleted: Boolean = false,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class Startup(
    val uid: String = "", // Same as User UID
    val startupType: String = "", // "edp", "accelerator" (Track)
    val track: String = "", // Explicit track field: "EDP" or "ACC"
    val startupName: String = "",
    val sector: String = "",
    val stage: String = "",
    val fundingAsk: String = "", // Numeric or range
    val teamSize: String = "", // "1-5", "6-20", etc.
    val dpiitNumber: String? = null, // Optional for Accelerator
    val pitchDeckUrl: String? = null,
    val logoUrl: String? = null,
    val website: String = "",
    val socialLinks: String = "",
    val demoVideoUrl: String? = null, // Added
    val revenue: String = "",
    val description: String = "",
    // Verification
    @get:com.google.firebase.firestore.PropertyName("isVerified")
    val isVerified: Boolean = false,
    val verifiedByAdminId: String? = null,
    val verifiedAt: Timestamp? = null
)

data class Investor(
    val uid: String = "", // Same as User UID
    val name: String = "",
    val firmName: String = "",
    val sectorsOfInterest: List<String> = emptyList(),
    val preferredStages: List<String> = emptyList(), // Added
    val ticketSizeMin: String = "",
    val ticketSizeMax: String = "",
    val investmentType: String = "",
    val city: String = "",
    val bio: String = "",
    val profilePhotoUrl: String? = null
)

data class Mentor(
    val uid: String = "", // Same as User UID
    val name: String = "",
    val title: String = "",
    val organization: String = "",
    val expertiseAreas: List<String> = emptyList(),
    val topicsToTeach: List<String> = emptyList(), // Added
    val experienceYears: String = "",
    val city: String = "",
    val bio: String = "",
    val profilePhotoUrl: String? = null
)

// --- Content ---

data class WallPost(
    val id: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val authorPhotoUrl: String? = null,
    val content: String = "", // Caption
    val mediaType: String = "none", // "image", "video", "none"
    val mediaUrl: String? = null,
    val thumbnailUrl: String? = null, // For video
    val postType: String = "generic", // "generic", "funding_call", "announcement", "poll"
    val fundingCallId: String? = null,
    val sector: String? = null,
    val pollQuestion: String? = null,
    val pollOptions: List<PollOption> = emptyList(),
    val likesCount: Int = 0,
    val commentsCount: Int = 0,
    @get:com.google.firebase.firestore.PropertyName("active")
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

data class PollOption(
    var id: String = "",
    var text: String = "",
    var voteCount: Int = 0
)

data class Comment(
    val id: String = "",
    val authorUserId: String = "",
    val authorName: String = "",
    val authorRole: String = "",
    val authorPhotoUrl: String? = null,
    val text: String = "",
    val createdAt: Timestamp? = null
)

data class Upvote(
    val userId: String = "",
    val createdAt: Timestamp? = null
)

data class FundingCall(
    val id: String = "",
    val investorId: String = "", // User UID of investor
    val investorName: String = "",
    val title: String = "",
    val description: String = "",
    val sectors: List<String> = emptyList(),
    val stages: List<String> = emptyList(),
    val minTicketAmount: String = "",
    val maxTicketAmount: String = "",
    val minEquity: String? = null,
    val maxEquity: String? = null,
    val locationPreference: String? = null,
    val applicationDeadline: Timestamp? = null,
    val attachments: List<Map<String, String>> = emptyList(), // [{type: "pdf", name: "deck.pdf", url: "..."}]
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null,
    val createdByUserId: String = ""
)

data class FundingApplication(
    val id: String = "",
    val callId: String = "",
    val startupId: String = "", // User UID of startup
    val startupName: String = "",
    val startupEmail: String = "",
    val startupPhone: String = "",
    val startupSector: String = "",
    val startupStage: String = "",
    val city: String = "",
    val state: String = "",
    val fundingAsk: String = "",
    val pitchDeckUrl: String? = null,
    val additionalNote: String = "",
    val status: String = "applied", // "applied", "shortlisted", "rejected"
    val appliedAt: Timestamp? = null
)

data class MentorVideo(
    val id: String = "",
    val mentorId: String = "",
    val mentorName: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val duration: String = "",
    val viewsCount: Int = 0,
    val createdAt: Timestamp? = null
)

data class FeaturedVideo(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val videoUrl: String = "",
    val thumbnailUrl: String? = null,
    val category: String = "", // "educational", "promotional", "tutorial"
    val duration: String = "",
    val order: Int = 0, // For sorting
    @get:com.google.firebase.firestore.PropertyName("isActive")
    val isActive: Boolean = true,
    val createdAt: Timestamp? = null
)

data class AIFEvent(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val startDate: Timestamp? = null,
    val endDate: Timestamp? = null,
    val venue: String = "",
    val city: String = "",
    val bannerUrl: String? = null,
    val status: String = "upcoming", // "upcoming", "ongoing", "completed"
    val agenda: String = "",
    val registrationUrl: String? = null,
    val createdAt: Timestamp? = null
)


data class ImportRecord(
    val id: String = "",
    val role: String = "", // "startup", "investor", "mentor"
    val filePath: String = "", // Storage path
    val status: String = "pending", // "pending", "processing", "completed", "completed_with_errors", "failed"
    val totalRows: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val createdByAdminId: String = "",
    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null
)

data class ImportError(
    val rowNumber: Int = 0,
    val errorMessage: String = "",
    val rawData: String = ""
)

// --- Chat ---

data class ChatChannel(
    val id: String = "",
    val participants: List<String> = emptyList(), // [userId1, userId2]
    val participantNames: Map<String, String> = emptyMap(), // {userId1: "Name1", userId2: "Name2"}
    val lastMessage: String = "",
    val lastMessageTimestamp: Timestamp? = null,
    val createdAt: Timestamp? = null
)

data class ChatMessage(
    val id: String = "",
    val senderId: String = "",
    val text: String = "",
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)
