package com.atmiya.innovation.repository

import com.atmiya.innovation.data.GovernmentScheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class GovernanceRepository {

    // Simulating an API call to https://apisetu.gov.in/swarrnim/v1/schemes
    fun getSchemes(): Flow<List<GovernmentScheme>> = flow {
        // Simulate network latency
        delay(1500)
        
        val schemes = listOf(
            GovernmentScheme(
                id = "SISFS",
                name = "Startup India Seed Fund Scheme",
                ministry = "DPIIT",
                description = "Financial assistance to startups for proof of concept, prototype development, product trials, market entry and commercialization.",
                benefits = "Up to INR 20 Lakhs as grant for validation and up to INR 50 Lakhs for commercialization.",
                eligibility = "Startup recognized by DPIIT, incorporated not more than 2 years ago.",
                applyUrl = "https://seedfund.startupindia.gov.in/",
                category = "Funding"
            ),
            GovernmentScheme(
                id = "FFS",
                name = "Fund of Funds for Startups (FFS)",
                ministry = "SIDBI",
                description = "Provides capital to SEBI-registered Alternative Investment Funds (AIFs) who in turn invest in startups.",
                benefits = "Investment through Daughter Funds. Corpus of INR 10,000 Crore.",
                eligibility = "Startups seeking equity funding.",
                applyUrl = "https://www.startupindia.gov.in/content/sih/en/government-schemes/fund-of-funds-for-startups.html",
                category = "Funding"
            ),
            GovernmentScheme(
                id = "80IAC",
                name = "Income Tax Exemption (80-IAC)",
                ministry = "Income Tax Dept",
                description = "Post certification, Startups can apply for Tax exemption for 3 consecutive years out of first 10 years.",
                benefits = "100% Tax deduction on profits for 3 years.",
                eligibility = "Private Limited/LLP incorporated after 1st April 2016.",
                applyUrl = "https://www.startupindia.gov.in/content/sih/en/government-schemes/Section80IAC.html",
                category = "Tax"
            ),
            GovernmentScheme(
                id = "IPR",
                name = "Startup Intellectual Property Protection (SIPP)",
                ministry = "DPIIT",
                description = "Facilitators for filing patents, designs and trademarks.",
                benefits = "80% rebate in filing patents and 50% rebate in filing trademarks.",
                eligibility = "Any DPIIT Recognized Startup.",
                applyUrl = "https://ipindia.gov.in/",
                category = "General"
            ),
             GovernmentScheme(
                id = "AIM",
                name = "Atal Innovation Mission",
                ministry = "NITI Aayog",
                description = "To promote a culture of innovation and entrepreneurship. Grants for establishing Atal Incubation Centres.",
                benefits = "Grant-in-aid of INR 10 Crore for establishing AICs.",
                eligibility = "Higher Edu Institutions, R&D Institutes, Corporate/Individuals.",
                applyUrl = "https://aim.gov.in/",
                category = "Incubation"
            ),
            GovernmentScheme(
                id = "PMMY",
                name = "Pradhan Mantri Mudra Yojana",
                ministry = "Ministry of Finance",
                description = "Loans up to 10 Lakhs to non-corporate, non-farm small/micro enterprises.",
                benefits = "Shishu: up to 50k, Kishore: 50k-5L, Tarun: 5L-10L.",
                eligibility = "Any Indian Citizen with a business plan for non-farm income generating activity.",
                applyUrl = "https://www.mudra.org.in/",
                category = "Funding"
            ),
            GovernmentScheme(
                id = "CGSS",
                name = "Credit Guarantee Scheme for Startups",
                ministry = "DPIIT",
                description = "Credit guarantee to Member Institutions (MIs) for loans extended to startups.",
                benefits = "Guarantee cover up to INR 10 Crore per borrower.",
                eligibility = "DPIIT Recognized Startups.",
                applyUrl = "https://www.startupindia.gov.in/content/sih/en/government-schemes/cgss.html",
                category = "Funding"
            )
        )
        
        emit(schemes)
    }
}
