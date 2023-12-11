package com.oxygenupdater.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.DeleteColumn
import androidx.room.RenameColumn
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.RoomDatabase
import androidx.room.migration.AutoMigrationSpec
import com.oxygenupdater.dao.ArticleDao
import com.oxygenupdater.dao.SubmittedUpdateFileDao
import com.oxygenupdater.dao.UpdateDataDao
import com.oxygenupdater.models.Article
import com.oxygenupdater.models.SubmittedUpdateFile
import com.oxygenupdater.models.UpdateData

@RewriteQueriesToDropUnusedColumns
@Database(
    version = 2,
    entities = [
        Article::class,
        SubmittedUpdateFile::class,
        UpdateData::class,
    ],
    autoMigrations = [
        AutoMigration(1, 2, LocalAppDb.NewsItemRemoveLanguageSpecificity::class),
    ],
)
abstract class LocalAppDb : RoomDatabase() {

    abstract fun articleDao(): ArticleDao
    abstract fun submittedUpdateFileDao(): SubmittedUpdateFileDao
    abstract fun updateDataDao(): UpdateDataDao

    @RenameColumn(NewsItem, "english_title", "title")
    @RenameColumn(NewsItem, "english_subtitle", "subtitle")
    @RenameColumn(NewsItem, "english_text", "text")
    @DeleteColumn(NewsItem, "dutch_title")
    @DeleteColumn(NewsItem, "dutch_subtitle")
    @DeleteColumn(NewsItem, "dutch_text")
    class NewsItemRemoveLanguageSpecificity : AutoMigrationSpec

    companion object {
        private const val NewsItem = "news_item"
    }
}
