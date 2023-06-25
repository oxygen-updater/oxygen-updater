package com.oxygenupdater.database

import androidx.room.Database
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomDatabase
import com.oxygenupdater.dao.NewsItemDao
import com.oxygenupdater.dao.SubmittedUpdateFileDao
import com.oxygenupdater.models.NewsItem
import com.oxygenupdater.models.SubmittedUpdateFile

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@RewriteQueriesToDropUnusedColumns
@Database(
    entities = [
        NewsItem::class,
        SubmittedUpdateFile::class,
    ],
    version = 1
)
abstract class LocalAppDb : RoomDatabase() {
    abstract fun newsItemDao(): NewsItemDao
    abstract fun submittedUpdateFileDao(): SubmittedUpdateFileDao
}
