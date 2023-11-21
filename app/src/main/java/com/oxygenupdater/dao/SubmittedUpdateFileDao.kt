package com.oxygenupdater.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.oxygenupdater.models.SubmittedUpdateFile

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Dao
interface SubmittedUpdateFileDao {

    @Query("SELECT EXISTS(SELECT 1 FROM `submitted_update_file` WHERE `name` = :url LIMIT 1)")
    fun isUrlAlreadySubmitted(url: String?): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg submittedUpdateFile: SubmittedUpdateFile)
}
