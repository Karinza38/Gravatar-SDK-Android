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
 * @param givenName
 * @param familyName
 * @param formatted
 */

data class Name(
    @SerializedName("givenName")
    val givenName: kotlin.String,
    @SerializedName("familyName")
    val familyName: kotlin.String,
    @SerializedName("formatted")
    val formatted: kotlin.String,
)
