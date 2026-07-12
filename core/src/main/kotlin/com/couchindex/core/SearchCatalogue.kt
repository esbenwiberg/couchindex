package com.couchindex.core

import java.util.Locale

class SearchCatalogue {
    fun invoke(catalogue: List<Title>, query: String): List<Title> {
        val terms = query.trim()
            .lowercase(Locale.ROOT)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }
        if (terms.isEmpty()) return emptyList()

        return catalogue
            .distinctBy { it.id }
            .filter { title ->
                val searchableText = "${title.name} ${title.synopsis}".lowercase(Locale.ROOT)
                terms.all(searchableText::contains)
            }
            .sortedWith(
                compareByDescending<Title> { title ->
                    title.name.lowercase(Locale.ROOT).startsWith(terms.joinToString(" "))
                }.thenBy { it.name.lowercase(Locale.ROOT) },
            )
    }
}
