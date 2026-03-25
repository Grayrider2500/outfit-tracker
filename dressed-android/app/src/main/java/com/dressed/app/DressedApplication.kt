package com.dressed.app

import android.app.Application
import androidx.room.Room
import com.dressed.app.data.WardrobeRepository
import com.dressed.app.data.local.DressedDatabase

class DressedApplication : Application() {

    lateinit var database: DressedDatabase
        private set

    lateinit var wardrobeRepository: WardrobeRepository
        private set

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            DressedDatabase::class.java,
            "dressed.db",
        )
            .addMigrations(DressedDatabase.MIGRATION_1_2)
            .build()
        wardrobeRepository = WardrobeRepository(database, database.wardrobeDao())
    }
}
