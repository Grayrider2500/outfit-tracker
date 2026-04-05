package com.dressed.app.data.dev

import com.dressed.app.data.local.DressedDatabase

/**
 * Release stub: seed datasets are not shipped. [run] is never invoked from the app when
 * [com.dressed.app.BuildConfig.DEBUG] is false; this exists so shared call sites compile.
 */
object TestDataSeeder {

    data class SeedSummary(
        val itemsAdded: Int,
        val itemsSkipped: Int,
        val outfitsAdded: Int,
        val outfitsSkipped: Int,
    )

    @Suppress("UNUSED_PARAMETER")
    suspend fun run(database: DressedDatabase, targetItemCount: Int = 0): SeedSummary {
        throw UnsupportedOperationException("Test data seeding is not available in release builds.")
    }
}
