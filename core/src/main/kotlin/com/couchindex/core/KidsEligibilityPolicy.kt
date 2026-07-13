package com.couchindex.core

class KidsEligibilityPolicy {
    fun filter(
        titles: List<Title>,
        maximumAge: Int,
        overrides: List<KidsCatalogueOverride> = emptyList(),
    ): List<Title> {
        require(maximumAge >= 0) { "maximumAge must not be negative" }
        return titles.filter { title -> isAllowed(title, maximumAge, overrides) }
    }

    fun isAllowed(
        title: Title,
        maximumAge: Int,
        overrides: List<KidsCatalogueOverride> = emptyList(),
    ): Boolean {
        require(maximumAge >= 0) { "maximumAge must not be negative" }
        val titleOverrides = overrides.filter { it.titleId == title.id }
        if (titleOverrides.any { it.decision == KidsOverrideDecision.Blocked }) return false

        val allowOverride = titleOverrides
            .filter { it.decision == KidsOverrideDecision.Allowed }
            .maxByOrNull { it.changedAtEpochMillis }
        val certification = title.certification ?: return allowOverride != null
        return certification.minimumAge <= maximumAge || allowOverride?.allowsOverAge == true
    }
}
