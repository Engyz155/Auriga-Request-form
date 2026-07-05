package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "client_requests")
data class ClientRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val clientName: String,
    val contactInfo: String,
    val sector: String,
    val challenge: String,
    val description: String,
    val proposalDraft: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
