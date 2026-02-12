package com.oxygenupdater.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.oxygenupdater.models.UpdateData
import kotlinx.coroutines.flow.Flow

@Dao
interface UpdateDataDao {

    @Query("SELECT * FROM `update_data` ORDER BY `id` DESC LIMIT 1")
    fun getFlow(): Flow<UpdateData?>

    @Query("SELECT `id` FROM `update_data` ORDER BY `id` DESC LIMIT 1")
    fun getId(): Long?

    @Transaction
    fun refresh(data: UpdateData?) = if (data == null) deleteAll() else when (val id = data.id) {
        getId() -> if (id != null) update(data) else {
            deleteAll() // ensure there's only one row
            insert(data.copy(id = 0)) // use 0 instead of null
        }

        else -> {
            deleteAll() // ensure there's only one row
            insert(data)
        }
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: UpdateData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: UpdateData)

    @Query("UPDATE `update_data` SET `download_url` = :downloadUrl WHERE `id` = :id")
    fun updateDownloadUrl(id: Long, downloadUrl: String)

    @Query("DELETE FROM `update_data`")
    fun deleteAll()
}
