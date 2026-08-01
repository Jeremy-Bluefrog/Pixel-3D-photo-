package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SpatialPhotoDao {
    @Query("SELECT * FROM spatial_photos ORDER BY timestamp DESC")
    fun getAllSpatialPhotos(): Flow<List<SpatialPhotoEntity>>

    @Query("SELECT * FROM spatial_photos WHERE id = :id")
    suspend fun getSpatialPhotoById(id: Long): SpatialPhotoEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSpatialPhoto(photo: SpatialPhotoEntity): Long

    @Update
    suspend fun updateSpatialPhoto(photo: SpatialPhotoEntity)

    @Query("DELETE FROM spatial_photos WHERE id = :id")
    suspend fun deleteSpatialPhotoById(id: Long)
}
