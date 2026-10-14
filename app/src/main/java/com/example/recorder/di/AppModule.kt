package com.example.recorder.di

import android.content.Context
import androidx.room.Room
import com.example.recorder.data.AppDb
import com.example.recorder.data.RecordingDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module @InstallIn(SingletonComponent::class)
object AppModule {
    @Provides @Singleton fun db(@ApplicationContext c: Context): AppDb =
        Room.databaseBuilder(c, AppDb::class.java, "recordings.db").build()
    @Provides fun dao(db: AppDb): RecordingDao = db.dao()
}
