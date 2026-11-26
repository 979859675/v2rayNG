package com.v2ray.ang.dto.entities

data class ServerAffiliationInfo(
    var testDelayMillis: Long = 0L,
    /** Raw `country`/`country_code` observed by the last real-ping egress-IP probe; null when unavailable. */
    var testCountry: String? = null
)
