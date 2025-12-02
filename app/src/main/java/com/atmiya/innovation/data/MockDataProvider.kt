package com.atmiya.innovation.data

import com.atmiya.innovation.R

data class MockWallPost(
    val id: String,
    val userName: String,
    val userRole: String,
    val content: String,
    val timeAgo: String,
    val likes: Int,
    val comments: Int
)

data class MentorProfile(
    val id: String,
    val name: String,
    val title: String,
    val organization: String,
    val city: String,
    val expertise: List<String>,
    val bio: String,
    val experienceYears: Int,
    val videos: List<MockMentorVideo> = emptyList()
)

data class MockMentorVideo(
    val id: String,
    val title: String,
    val duration: String,
    val views: String,
    val thumbnailUrl: String // Placeholder URL or resource ID logic
)

data class InvestorProfile(
    val id: String,
    val name: String,
    val firmName: String,
    val city: String,
    val sectors: List<String>,
    val ticketSize: String,
    val bio: String
)

object MockDataProvider {

    val wallPosts = listOf(
        MockWallPost(
            "1", "Rajesh Kumar", "Startup Founder",
            "Excited to announce that we have reached our first 1000 users! Thanks to the Atmiya network for the support. #Milestone #Growth",
            "2h ago", 45, 12
        ),
        MockWallPost(
            "2", "Priya Desai", "Mentor",
            "Hosting a session on 'Pitching to Investors' this Saturday. Join me to learn the do's and don'ts of a perfect pitch deck.",
            "5h ago", 120, 34
        ),
        MockWallPost(
            "3", "GreenVentures Capital", "Investor",
            "Looking for innovative Agri-tech startups in Gujarat. DM for connection.",
            "1d ago", 89, 56
        ),
        MockWallPost(
            "4", "Amit Shah", "Startup (EDP)",
            "Just finished the prototype for our smart irrigation system. Looking for feedback from mentors.",
            "2d ago", 30, 8
        )
    )

    val mentors = listOf(
        MentorProfile(
            "m1", "Dr. Suresh Patel", "Senior Consultant", "IIM Ahmedabad", "Ahmedabad",
            listOf("Business Strategy", "Marketing", "Scaling"),
            "Over 20 years of experience in guiding startups to scale. Ex-CMO of a Fortune 500 company.",
            20,
            listOf(
                MockMentorVideo("v1", "How to Scale 10x", "15:30", "1.2k", ""),
                MockMentorVideo("v2", "Marketing 101", "10:00", "800", "")
            )
        ),
        MentorProfile(
            "m2", "Anjali Mehta", "Tech Lead", "Google", "Bangalore",
            listOf("Technology", "AI/ML", "Product Management"),
            "Passionate about building scalable tech products. Helping startups with their tech stack and product roadmap.",
            12,
            listOf(
                MockMentorVideo("v3", "AI for Startups", "20:00", "2.5k", "")
            )
        ),
        MentorProfile(
            "m3", "Vikram Singh", "Legal Advisor", "Singh & Associates", "Mumbai",
            listOf("Legal", "IPR", "Compliance"),
            "Expert in Intellectual Property Rights and startup compliance. ensuring your innovation is protected.",
            15
        )
    )

    val investors = listOf(
        InvestorProfile(
            "i1", "Venture Catalysts", "Venture Catalysts", "Mumbai",
            listOf("Fintech", "Edtech", "SaaS"),
            "₹50L - ₹2Cr",
            "We invest in early-stage startups with high growth potential. Focused on founders with a strong vision."
        ),
        InvestorProfile(
            "i2", "Gujarat Angels", "Gujarat Angel Network", "Ahmedabad",
            listOf("Agritech", "Manufacturing", "Clean Energy"),
            "₹25L - ₹1Cr",
            "Supporting local innovation in Gujarat. We look for sustainable business models."
        ),
        InvestorProfile(
            "i3", "Ravi Verma", "Angel Investor", "Surat",
            listOf("D2C", "Retail"),
            "₹10L - ₹50L",
            "Individual angel investor looking for promising D2C brands."
        )
    )
}
