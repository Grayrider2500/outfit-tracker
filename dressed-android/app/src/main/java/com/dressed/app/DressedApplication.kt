package com.dressed.app

import android.app.Application
import androidx.room.Room
import com.dressed.app.data.OutfitRepository
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.local.DressedDatabase

class DressedApplication : Application() {

    lateinit var database: DressedDatabase
        private set

    lateinit var wardrobeRepository: WardrobeRepository
        private set

    lateinit var outfitRepository: OutfitRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            DressedDatabase::class.java,
            "dressed.db",
        )
            .addMigrations(DressedDatabase.MIGRATION_1_2, DressedDatabase.MIGRATION_2_3)
            .build()
        wardrobeRepository = WardrobeRepository(database, database.wardrobeDao())
        outfitRepository = OutfitRepository(database.outfitDao())
    }
}
