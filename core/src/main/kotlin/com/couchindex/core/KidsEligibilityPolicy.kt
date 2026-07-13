package com.couchindex.core

class KidsEligibilityPolicy {
    fun filter(titles: List<Title>, maximumAge: Int): List<Title> {
        require(maximumAge >= 0) { "maximumAge must not be negative" }
        return titles.filter { title -> isAllowed(title, maximumAge) }
    }

    fun isAllowed(title: Title, maximumAge: Int): Boolean {
        require(maximumAge >= 0) { "maximumAge must not be negative" }
        val certification = title.certification ?: return false
        return certification.minimumAge <= maximumAge
    }
}
