package me.thekey.android.exception

import org.json.JSONObject

class RateLimitExceededApiError(responseCode: Int, json: JSONObject) : TheKeyApiError(responseCode, json) {
    companion object {
        const val ERROR_RATE_LIMIT_EXCEEDED = "rate_limit_exceeded"

        private const val PARAM_RETRY_AFTER = "retry_after"
    }

    val retryAfter: Int? = json.optString(PARAM_RETRY_AFTER)?.toIntOrNull()
}
