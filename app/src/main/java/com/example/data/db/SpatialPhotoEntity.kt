package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.SpatialCaptureType
import com.example.data.model.SpatialPhoto

@Entity(tableName = "spatial_photos")
data class SpatialPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val sourceUri: String,
    val depthMapUri: String,
    val captureType: String,
    val depthIntensity: Float,
    val focalPlane: Float,
    val blurAmount: Float,
    val layerSeparation: Float,
    val aiAnalysis: String,
    val timestamp: Long,
    val isFavorite: Boolean,
    val width: Int,
    val height: Int
)

fun SpatialPhotoEntity.toDomainModel(): SpatialPhoto {
    return SpatialPhoto(
        id = id,
        title = title,
        sourceUri = sourceUri,
        depthMapUri = depthMapUri,
        captureType = try {
            SpatialCaptureType.valueOf(captureType)
        } catch (e: Exception) {
            SpatialCaptureType.AI_CONVERTED_2D
        },
        depthIntensity = depthIntensity,
        focalPlane = focalPlane,
        blurAmount = blurAmount,
        layerSeparation = layerSeparation,
        aiAnalysis = aiAnalysis,
        timestamp = timestamp,
        isFavorite = isFavorite,
        width = width,
        height = height
    )
}

fun SpatialPhoto.toEntity(): SpatialPhotoEntity {
    return SpatialPhotoEntity(
        id = id,
        title = title,
        sourceUri = sourceUri,
        depthMapUri = depthMapUri,
        captureType = captureType.name,
        depthIntensity = depthIntensity,
        focalPlane = focalPlane,
        blurAmount = blurAmount,
        layerSeparation = layerSeparation,
        aiAnalysis = aiAnalysis,
        timestamp = timestamp,
        isFavorite = isFavorite,
        width = width,
        height = height
    )
}
