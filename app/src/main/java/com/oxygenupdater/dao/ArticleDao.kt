package com.oxygenupdater.dao

import androidx.collection.ArraySet
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.oxygenupdater.models.Article

@Dao
interface ArticleDao {

    @Query("SELECT * FROM `news_item` ORDER BY `date_last_edited` DESC, `date_published` DESC, `id` DESC")
    fun getAll(): List<Article>

    @Query("SELECT * FROM `news_item` WHERE `id` = :id")
    fun getById(id: Long?): Article?

    /** Toggles [Article.read] status, unless overridden by [read] */
    @Query("UPDATE `news_item` SET `read` = :read WHERE `id` = :id")
    fun toggleRead(id: Long, read: Boolean)

    /** Toggles [Article.read] status, unless overridden by [read] */
    @Transaction
    fun toggleRead(
        article: Article,
        read: Boolean = !article.readState,
    ) = toggleRead(article.id!!, read)

    @Query("UPDATE `news_item` SET `read` = 1")
    @Transaction
    fun markAllRead()

    @Transaction
    fun insertOrUpdate(article: Article) = getById(article.id)?.let {
        // Make sure the `read` column isn't overwritten
        update(article.copy(read = it.readState))
    } ?: insert(article)

    @Transaction
    fun refreshArticles(articles: List<Article>) {
        val newIds = articles.mapTo(ArraySet(articles.size)) {
            insertOrUpdate(it)
            it.id
        }

        // Remove items not present in the server response. Calling deleteAll() before inserting fresh rows from the
        // server is slightly more efficient, but we'd lose read statuses in that case.
        val something = getAll().mapNotNull { item ->
            item.id?.let { if (!newIds.contains(it)) it else null }
        }.toLongArray()
        deleteByIds(*something)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(article: Article)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(article: Article)

    @Query("DELETE FROM `news_item` WHERE `id` IN (:ids)")
    fun deleteByIds(vararg ids: Long)
}
