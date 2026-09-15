package com.ghartak.app.domain.model

enum class ParcelType(val displayName: String) {
    DOCUMENTS("Documents"),
    SMALL_PACKAGE("Small package"),
    MEDIUM_PACKAGE("Medium package"),
    LARGE_PACKAGE("Large package"),
    FOOD("Food / package"),
    ELECTRONICS("Electronics"),
    OTHER("Other")
}
