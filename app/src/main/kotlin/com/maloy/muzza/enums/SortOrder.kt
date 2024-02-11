package com.maloy.muzza.enums

enum class SortOrder {
    Ascending,
    Descending;

    operator fun not() = when (this) {
        Ascending -> Descending
        Descending -> Ascending
    }
}
