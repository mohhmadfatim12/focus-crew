package com.focuscrew.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.focuscrew.app.data.local.dao.BlockedAppDao
import com.focuscrew.app.data.local.dao.DistractionDao
import com.focuscrew.app.data.local.dao.SessionDao
import com.focuscrew.app.data.local.dao.TaskAlarmDao
import com.focuscrew.app.data.local.entity.BlockedAppEntity
import com.focuscrew.app.data.local.entity.DistractionEntryEntity
import com.focuscrew.app.data.local.entity.SessionEntity
import com.focuscrew.app.data.local.entity.TaskAlarmEntity

@Database(
    entities = [
        SessionEntity::class,
        DistractionEntryEntity::class,
        BlockedAppEntity::class,
        TaskAlarmEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class FocusDatabase : RoomDatabase() {

    abstract fun sessionDao(): SessionDao
    abstract fun distractionDao(): DistractionDao
    abstract fun blockedAppDao(): BlockedAppDao
    abstract fun taskAlarmDao(): TaskAlarmDao

    companion object {
        @Volatile
        private var INSTANCE: FocusDatabase? = null

        fun getInstance(context: Context): FocusDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    FocusDatabase::class.java,
                    "focuscrew.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
