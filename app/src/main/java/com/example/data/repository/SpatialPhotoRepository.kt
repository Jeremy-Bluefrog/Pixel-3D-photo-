package com.example.data.repository

import com.example.data.db.SpatialPhotoDao
import com.example.data.db.toDomainModel
import com.example.data.db.toEntity
import com.example.data.model.SpatialPhoto
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SpatialPhotoRepository(private val dao: SpatialPhotoDao) {

    val allPhotos: Flow<List<SpatialPhoto>> = dao.getAllSpatialPhotos().map { entities ->
        entities.map { it.toDomainModel() }
    }

    suspend fun getPhotoById(id: Long): SpatialPhoto? {
        return dao.getSpatialPhotoById(id)?.toDomainModel()
    }

    suspend fun savePhoto(photo: SpatialPhoto): Long {
        return dao.insertSpatialPhoto(photo.toEntity())
    }

    suspend fun updatePhoto(photo: SpatialPhoto) {
        dao.updateSpatialPhoto(photo.toEntity())
    }

    suspend fun deletePhoto(id: Long) {
        dao.deleteSpatialPhotoById(id)
    }
}
