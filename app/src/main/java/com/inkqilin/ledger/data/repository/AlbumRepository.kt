package com.inkqilin.ledger.data.repository

import com.inkqilin.ledger.data.AlbumPhoto
import com.inkqilin.ledger.data.AlbumPhotoDao
import kotlinx.coroutines.flow.Flow

class AlbumRepository(private val dao: AlbumPhotoDao) {
    fun getAllPhotos(): Flow<List<AlbumPhoto>> = dao.getAllPhotos()
    suspend fun getAllPhotosOnce(): List<AlbumPhoto> = dao.getAllPhotosOnce()
    suspend fun getPhotoById(id: Long): AlbumPhoto? = dao.getPhotoById(id)
    suspend fun insertPhoto(photo: AlbumPhoto): Long = dao.insertPhoto(photo)
    suspend fun updatePhoto(photo: AlbumPhoto) = dao.updatePhoto(photo)
    suspend fun deletePhoto(photo: AlbumPhoto) = dao.deletePhoto(photo)
    suspend fun deletePhotoById(id: Long) = dao.deletePhotoById(id)
    suspend fun deleteAllPhotos() = dao.deleteAllPhotos()
}
