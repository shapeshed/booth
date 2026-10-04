package com.shapeshed.booth.data

import org.json.JSONObject

internal fun JSONObject.optionalString(key: String): String? = if (isNull(key)) {
    null
} else {
    optString(key).takeIf {
        it.isNotBlank()
    }
}

internal fun JSONObject.optionalDouble(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)

internal fun JSONObject.optionalBoolean(key: String): Boolean? = if (isNull(key) || !has(key)) null else optBoolean(key)

internal fun JSONObject.optLongOrNull(key: String): Long? = if (isNull(key) || !has(key)) null else optLong(key)

internal fun JSONObject.optDoubleOrNull(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key)

internal fun JSONObject.optBooleanOrNull(key: String): Boolean? =
    if (isNull(key) || !has(key)) null else optBoolean(key)

internal inline fun <reified T : Enum<T>> String.toEnum(): T? = runCatching { enumValueOf<T>(this) }.getOrNull()
