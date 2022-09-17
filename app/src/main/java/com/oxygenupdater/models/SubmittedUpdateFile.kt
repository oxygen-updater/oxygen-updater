package com.oxygenupdater.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.oxygenupdater.utils.Utils
import java.time.LocalDateTime

/**
 * @author [Adhiraj Singh Chauhan](https://github.com/adhirajsinghchauhan)
 */
@Entity(tableName = "submitted_update_file")
data class SubmittedUpdateFile(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    @ColumnInfo(index = true)
    val name: String?,

    @ColumnInfo(
        name = "date_submitted",
        defaultValue = "CURRENT_TIMESTAMP"
    )
    val dateSubmitted: String = LocalDateTime.now(Utils.SERVER_TIME_ZONE).toString()
)
