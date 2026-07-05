package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ClientRequestDao {
    @Query("SELECT * FROM client_requests ORDER BY timestamp DESC")
    fun getAllRequests(): Flow<List<ClientRequest>>

    @Query("SELECT * FROM client_requests WHERE id = :id")
    suspend fun getRequestById(id: Long): ClientRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRequest(request: ClientRequest): Long

    @Update
    suspend fun updateRequest(request: ClientRequest)

    @Delete
    suspend fun deleteRequest(request: ClientRequest)
}
