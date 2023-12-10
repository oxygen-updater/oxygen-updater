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
    fun refresh(data: UpdateData?) = if (data == null) deleteAll() else if (data.id == getId()) update(data) else {
        deleteAll() // ensure there's only one row
        insert(data)
    }

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(data: UpdateData)

    @Update(onConflict = OnConflictStrategy.REPLACE)
    fun update(data: UpdateData)

    @Query("DELETE FROM `update_data`")
    fun deleteAll()
}
