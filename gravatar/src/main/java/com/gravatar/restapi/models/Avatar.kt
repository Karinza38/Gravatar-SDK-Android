/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */
package com.gravatar.restapi.models

import com.google.gson.annotations.SerializedName
import java.util.Objects

/**
 * An avatar that the user has already uploaded to their Gravatar account.
 *
 * @param imageId Unique identifier for the image.
 * @param imageUrl Image URL
 * @param isCropped Indicates whether the image has been cropped.
 * @param format Format of the image (e.g., JPEG, PNG).
 * @param rating Rating associated with the image.
 * @param updatedDate Date and time when the image was last updated.
 * @param altText Alternative text description of the image.
 */

public class Avatar private constructor(
    // Unique identifier for the image.
    @SerializedName("image_id")
    public val imageId: kotlin.String,
    // Image URL
    @SerializedName("image_url")
    public val imageUrl: kotlin.String,
    // Indicates whether the image has been cropped.
    @SerializedName("is_cropped")
    public val isCropped: kotlin.Boolean,
    // Format of the image (e.g., JPEG, PNG).
    @SerializedName("format")
    public val format: kotlin.Int,
    // Rating associated with the image.
    @SerializedName("rating")
    public val rating: kotlin.String,
    // Date and time when the image was last updated.
    @SerializedName("updated_date")
    public val updatedDate: java.time.Instant,
    // Alternative text description of the image.
    @SerializedName("altText")
    public val altText: kotlin.String,
) {
    override fun toString(): String = "Avatar(imageId=$imageId, imageUrl=$imageUrl, isCropped=$isCropped, format=$format, rating=$rating, updatedDate=$updatedDate, altText=$altText)"

    override fun equals(other: Any?): Boolean = other is Avatar &&
        imageId == other.imageId &&
        imageUrl == other.imageUrl &&
        isCropped == other.isCropped &&
        format == other.format &&
        rating == other.rating &&
        updatedDate == other.updatedDate &&
        altText == other.altText

    override fun hashCode(): Int = Objects.hash(imageId, imageUrl, isCropped, format, rating, updatedDate, altText)

    public class Builder {
        // Unique identifier for the image.
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var imageId: kotlin.String? = null

        // Image URL
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var imageUrl: kotlin.String? = null

        // Indicates whether the image has been cropped.
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var isCropped: kotlin.Boolean? = null

        // Format of the image (e.g., JPEG, PNG).
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var format: kotlin.Int? = null

        // Rating associated with the image.
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var rating: kotlin.String? = null

        // Date and time when the image was last updated.
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var updatedDate: java.time.Instant? = null

        // Alternative text description of the image.
        @set:JvmSynthetic // Hide 'void' setter from Java
        public var altText: kotlin.String? = null

        public fun setImageId(imageId: kotlin.String?): Builder = apply { this.imageId = imageId }

        public fun setImageUrl(imageUrl: kotlin.String?): Builder = apply { this.imageUrl = imageUrl }

        public fun setIsCropped(isCropped: kotlin.Boolean?): Builder = apply { this.isCropped = isCropped }

        public fun setFormat(format: kotlin.Int?): Builder = apply { this.format = format }

        public fun setRating(rating: kotlin.String?): Builder = apply { this.rating = rating }

        public fun setUpdatedDate(updatedDate: java.time.Instant?): Builder = apply { this.updatedDate = updatedDate }

        public fun setAltText(altText: kotlin.String?): Builder = apply { this.altText = altText }

        public fun build(): Avatar = Avatar(imageId!!, imageUrl!!, isCropped!!, format!!, rating!!, updatedDate!!, altText!!)
    }
}

@JvmSynthetic // Hide from Java callers who should use Builder.
public fun Avatar(initializer: Avatar.Builder.() -> Unit): Avatar {
    return Avatar.Builder().apply(initializer).build()
}
