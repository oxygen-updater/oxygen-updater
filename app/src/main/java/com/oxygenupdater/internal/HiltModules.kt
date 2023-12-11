package com.oxygenupdater.internal

import android.content.Context
import android.content.SharedPreferences
import androidx.core.app.NotificationManagerCompat
import androidx.preference.PreferenceManager
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.work.WorkManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.oxygenupdater.apis.DownloadApi
import com.oxygenupdater.apis.ServerApi
import com.oxygenupdater.database.LocalAppDb
import com.oxygenupdater.database.SqliteMigrations
import com.oxygenupdater.extensions.get
import com.oxygenupdater.extensions.set
import com.oxygenupdater.internal.settings.KeySqlToRoomMigrationDone
import com.oxygenupdater.utils.createDownloadApi
import com.oxygenupdater.utils.createServerApi
import com.oxygenupdater.utils.logDebug
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Singleton
    @Provides
    fun serverApi(@ApplicationContext context: Context): ServerApi = createServerApi(context)

    @Singleton
    @Provides
    fun downloadApi(): DownloadApi = createDownloadApi()
}

@Module
@InstallIn(SingletonComponent::class)
object PrefModule {

    @Singleton
    @Provides
    fun sharedPreferences(
        @ApplicationContext context: Context,
    ): SharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
}

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    const val AppDb = "oxygen_updater"

    @Singleton
    @Provides
    fun buildLocalAppDb(
        @ApplicationContext context: Context,
        sharedPreferences: SharedPreferences,
    ) = Room.databaseBuilder(context, LocalAppDb::class.java, AppDb).apply {
        // Ensure migration is done only once (skip `onOpen` callback)
        if (sharedPreferences[KeySqlToRoomMigrationDone, false]) {
            logDebug("DatabaseModule", "SQLite to Room migration has already been done")
        } else addCallback(object : RoomDatabase.Callback() {
            /**
             * While overriding [RoomDatabase.Callback.onCreate] would be ideal, attaching
             * other databases doesn't work (due to some WAL/transaction restrictions).
             */
            override fun onOpen(db: SupportSQLiteDatabase) = super.onOpen(db).also {
                SqliteMigrations.prepopulateFromSqlite(context, db)
                sharedPreferences[KeySqlToRoomMigrationDone] = true
            }
        })
    }.fallbackToDestructiveMigrationOnDowngrade().build()

    @Singleton
    @Provides
    fun articleDao(localAppDb: LocalAppDb) = localAppDb.articleDao()

    @Singleton
    @Provides
    fun submittedUpdateFileDao(localAppDb: LocalAppDb) = localAppDb.submittedUpdateFileDao()

    @Singleton
    @Provides
    fun updateDataDao(localAppDb: LocalAppDb) = localAppDb.updateDataDao()
}

@Module
@InstallIn(SingletonComponent::class)
object NotificationModule {

    @Singleton
    @Provides
    fun notificationManagerCompat(@ApplicationContext context: Context) = NotificationManagerCompat.from(context)
}

@Module
@InstallIn(SingletonComponent::class)
object MiscellaneousModule {

    @Singleton
    @Provides
    fun appUpdateManager(@ApplicationContext context: Context) = AppUpdateManagerFactory.create(context)

    @Singleton
    @Provides
    fun firebaseAnalytics(@ApplicationContext context: Context) = FirebaseAnalytics.getInstance(context)

    @Singleton
    @Provides
    fun firebaseCrashlytics() = FirebaseCrashlytics.getInstance()

    @Singleton
    @Provides
    fun firebaseMessaging() = FirebaseMessaging.getInstance()

    @Singleton
    @Provides
    fun workManager(@ApplicationContext context: Context) = WorkManager.getInstance(context)
}
