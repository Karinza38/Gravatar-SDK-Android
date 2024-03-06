/**
 *
 * Please note:
 * This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * Do not edit this file manually.
 *
 */

@file:Suppress(
    "ArrayInDataClass",
    "EnumEntryName",
    "RemoveRedundantQualifierName",
    "UnusedImport",
)

package com.gravatar.api.models

import com.google.gson.annotations.SerializedName

/**
 *
 *
 * @param hash The email's hash of the profile.
 * @param requestHash
 * @param profileUrl
 * @param preferredUsername
 * @param thumbnailUrl
 * @param lastProfileEdit
 * @param profileBackground
 * @param name
 * @param displayName
 * @param pronouns
 * @param aboutMe
 * @param currentLocation
 * @param shareFlags
 * @param emails
 */

data class UserProfile(
    // The email's hash of the profile.
    @SerializedName("hash")
    val hash: kotlin.String,
    @SerializedName("requestHash")
    val requestHash: kotlin.String? = null,
    @SerializedName("profileUrl")
    val profileUrl: kotlin.String? = null,
    @SerializedName("preferredUsername")
    val preferredUsername: kotlin.String? = null,
    @SerializedName("thumbnailUrl")
    val thumbnailUrl: kotlin.String? = null,
    @SerializedName("last_profile_edit")
    val lastProfileEdit: kotlin.String? = null,
    @SerializedName("profileBackground")
    val profileBackground: ProfileBackground? = null,
    @SerializedName("name")
    val name: Name? = null,
    @SerializedName("displayName")
    val displayName: kotlin.String? = null,
    @SerializedName("pronouns")
    val pronouns: kotlin.String? = null,
    @SerializedName("aboutMe")
    val aboutMe: kotlin.String? = null,
    @SerializedName("currentLocation")
    val currentLocation: kotlin.String? = null,
    @SerializedName("share_flags")
    val shareFlags: ShareFlags? = null,
    @SerializedName("emails")
    val emails: kotlin.collections.List<Email>? = null,
)
