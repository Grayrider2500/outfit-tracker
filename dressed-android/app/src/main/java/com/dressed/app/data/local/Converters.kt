package com.dressed.app.data.local

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromSeasons(list: List<String>): String =
        list.joinToString(",")

    @TypeConverter
    fun toSeasons(value: String): List<String> =
        if (value.isBlank()) emptyList() else value.split(",").map { it.trim() }.filter { it.isNotEmpty() }
}
