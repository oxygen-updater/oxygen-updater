package com.oxygenupdater.dao

import androidx.room.Dao
import androidx.room.Delete
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

    @Query("SELECT * FROM news_item ORDER BY id DESC")
    fun getAll(): List<NewsItem>

    @Query("SELECT * FROM news_item WHERE id = :id")
    fun getById(id: Long?): NewsItem?

    /**
     * Toggles [NewsItem.read] status, unless overridden by [newReadStatus]
     */
    @Query("UPDATE news_item SET read = :newReadStatus WHERE id = :id")
    fun toggleReadStatus(
        id: Long,
        newReadStatus: Boolean
    )

    /**
     * Toggles [NewsItem.read] status, unless overridden by [newReadStatus]
     */
    @Transaction
    fun toggleReadStatus(
        newsItem: NewsItem,
        newReadStatus: Boolean = !newsItem.read
    ) = toggleReadStatus(newsItem.id!!, newReadStatus)

    @Transaction
    fun insertOrUpdate(newsItem: NewsItem) = getById(newsItem.id)?.let {
        // Make sure the `read` column isn't overwritten
        update(newsItem.copy(read = it.read))
    } ?: insert(newsItem)

    @Transaction
    fun refreshNewsItems(newsItems: List<NewsItem>) {
        val newIds = newsItems.map {
            insertOrUpdate(it)
            it.id
        }

        // All IDs that are in the database but are not in the result should be deleted from the database.
        getAll().map { it.id }
            .filter { it != null && !newIds.contains(it) }
            .forEach { deleteByIds(it!!) }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(newsItem: NewsItem)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(newsItem: NewsItem)

    @Query("DELETE FROM news_item WHERE id IN (:ids)")
    fun deleteByIds(vararg ids: Long)

    @Delete
    fun delete(newsItems: Set<NewsItem>)

    @Query("DELETE FROM news_item")
    fun deleteAll()
}
