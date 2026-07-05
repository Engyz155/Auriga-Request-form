package com.example.data

import kotlinx.coroutines.flow.Flow

class ClientRequestRepository(private val clientRequestDao: ClientRequestDao) {
    val allRequests: Flow<List<ClientRequest>> = clientRequestDao.getAllRequests()

    suspend fun getRequestById(id: Long): ClientRequest? {
        return clientRequestDao.getRequestById(id)
    }

    suspend fun insert(request: ClientRequest): Long {
        return clientRequestDao.insertRequest(request)
    }

    suspend fun update(request: ClientRequest) {
        clientRequestDao.updateRequest(request)
    }

    suspend fun delete(request: ClientRequest) {
        clientRequestDao.deleteRequest(request)
    }
}
