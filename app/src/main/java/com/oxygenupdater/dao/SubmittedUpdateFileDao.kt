package com.oxygenupdater.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.oxygenupdater.models.SubmittedUpdateFile

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Dao
interface SubmittedUpdateFileDao {

    @Query("SELECT COUNT(name) > 0 FROM submitted_update_file WHERE name = :filename")
    fun isFileAlreadySubmitted(filename: String?): Boolean

    @Insert
    fun insert(submittedUpdateFile: SubmittedUpdateFile)
}
