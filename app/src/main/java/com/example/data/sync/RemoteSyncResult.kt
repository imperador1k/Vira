package com.example.data.sync

sealed class RemoteSyncResult {
    data class Success(val serverUpdatedAt: Long, val remoteVersion: Long) : RemoteSyncResult()
    data class Conflict(val serverVersion: Long, val serverUpdatedAt: Long, val message: String) : RemoteSyncResult()
    data class NetworkError(val message: String, val canRetry: Boolean = true) : RemoteSyncResult()
}
