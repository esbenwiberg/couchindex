package com.couchindex.core

interface CatalogueRepository {
    suspend fun discoverSubscriptionTitles(
        region: String,
        providerIds: Set<String>,
    ): List<Title>
}
