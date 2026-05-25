package com.inkqilin.ledger.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AlbumPhotoDao {
    @Query("SELECT * FROM album_photos ORDER BY createdAt DESC")
    fun getAllPhotos(): Flow<List<AlbumPhoto>>

    @Query("SELECT * FROM album_photos WHERE id = :id")
    suspend fun getPhotoById(id: Long): AlbumPhoto?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: AlbumPhoto): Long

    @Update
    suspend fun updatePhoto(photo: AlbumPhoto)

    @Delete
    suspend fun deletePhoto(photo: AlbumPhoto)

    @Query("DELETE FROM album_photos WHERE id = :id")
    suspend fun deletePhotoById(id: Long)
}
