package com.couchindex.app.state

import com.couchindex.core.ViewerProfile

fun profiledPreferencesName(baseName: String, profile: ViewerProfile): String =
    when (profile) {
        ViewerProfile.Adult -> baseName
        ViewerProfile.Kids -> "$baseName-kids"
    }
