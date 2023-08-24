package com.oxygenupdater.dao

import androidx.collection.ArraySet
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.oxygenupdater.models.NewsItem

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Dao
interface NewsItemDao {

    @Query("SELECT * FROM `news_item` ORDER BY `date_last_edited` DESC, `date_published` DESC, `id` DESC")
    fun getAll(): List<NewsItem>

    @Query("SELECT * FROM `news_item` WHERE `id` = :id")
    fun getById(id: Long?): NewsItem?

    /** Toggles [NewsItem.read] status, unless overridden by [read] */
    @Query("UPDATE `news_item` SET `read` = :read WHERE `id` = :id")
    fun toggleRead(id: Long, read: Boolean)

    /** Toggles [NewsItem.read] status, unless overridden by [read] */
    @Transaction
    fun toggleRead(
        newsItem: NewsItem,
        read: Boolean = !newsItem.readState.value,
    ) = toggleRead(newsItem.id!!, read)

    @Query("UPDATE `news_item` SET `read` = 1")
    @Transaction
    fun markAllRead()

    @Transaction
    fun insertOrUpdate(newsItem: NewsItem) = getById(newsItem.id)?.let {
        // Make sure the `read` column isn't overwritten
        update(newsItem.copy(read = it.readState.value))
    } ?: insert(newsItem)

    @Transaction
    fun refreshNewsItems(newsItems: List<NewsItem>) {
        val newIds = newsItems.mapTo(ArraySet(newsItems.size)) {
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
    fun insert(newsItem: NewsItem)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(newsItem: NewsItem)

    @Query("DELETE FROM `news_item` WHERE `id` IN (:ids)")
    fun deleteByIds(vararg ids: Long)
}
