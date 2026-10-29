package com.als.presence.core.network

import com.als.presence.core.common.Resource
import com.als.presence.data.model.ServerErrorResponse
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import retrofit2.Response

object NetworkErrorParser {
    val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
        explicitNulls = false
        isLenient = true
    }

    fun parseErrorBody(errorBodyString: String?, defaultMessage: String = "An error occurred"): Pair<String, String?> {
        if (errorBodyString.isNullOrBlank()) {
            return Pair(defaultMessage, null)
        }
        return try {
            val errorResponse = json.decodeFromString<ServerErrorResponse>(errorBodyString)
            val msg = errorResponse.message?.takeIf { it.isNotBlank() }
                ?: errorResponse.error?.takeIf { it.isNotBlank() }
                ?: defaultMessage
            val code = errorResponse.code?.takeIf { it.isNotBlank() }
                ?: errorResponse.statusCode?.toString()
            Pair(msg, code)
        } catch (_: Exception) {
            val trimmed = errorBodyString.trim()
            if (!trimmed.startsWith("<") && trimmed.length in 1..250) {
                Pair(trimmed, null)
            } else {
                Pair(defaultMessage, null)
            }
        }
    }
}

/**
 * Checks if the throwable represents a server connection failure, offline network, socket timeout,
 * or host resolution failure.
 */
fun isServerNotConnectedError(e: Throwable): Boolean {
    var current: Throwable? = e
    while (current != null) {
        if (current is java.net.ConnectException ||
            current is java.net.UnknownHostException ||
            current is java.net.SocketTimeoutException ||
            current is javax.net.ssl.SSLException ||
            current is kotlinx.coroutines.TimeoutCancellationException
        ) {
            return true
        }
        val msg = (current.message ?: current.localizedMessage ?: "").lowercase()
        if (msg.contains("failed to connect") ||
            msg.contains("connection refused") ||
            msg.contains("unable to resolve host") ||
            msg.contains("no address associated with hostname") ||
            msg.contains("network is unreachable") ||
            msg.contains("sockettimeout") ||
            msg.contains("timeout") ||
            msg.contains("econnrefused") ||
            msg.contains("enotfound") ||
            msg.contains("ehostunreach")
        ) {
            return true
        }
        current = current.cause
    }
    return false
}

const val SERVER_NOT_CONNECTED_MESSAGE = "Server is not connected. Please check your internet connection or verify that the server is running."

/**
 * Converts a Retrofit [Response] into a [Resource].
 * Parses the server's JSON error response body if [Response.isSuccessful] is false.
 */
inline fun <reified T> Response<T>.toResource(defaultErrorMessage: String = "Operation failed"): Resource<T> {
    return if (isSuccessful) {
        val body = body()
        if (body != null) {
            Resource.Success(body)
        } else {
            if (T::class == Unit::class) {
                @Suppress("UNCHECKED_CAST")
                Resource.Success(Unit as T)
            } else {
                try {
                    @Suppress("UNCHECKED_CAST")
                    Resource.Success(null as T)
                } catch (_: Exception) {
                    Resource.Error(
                        exception = IllegalStateException("Response body was null"),
                        message = defaultErrorMessage
                    )
                }
            }
        }
    } else {
        val statusCode = code()
        if (statusCode in listOf(502, 503, 504)) {
            return Resource.Error(
                exception = HttpException(this),
                message = "Server is temporarily unavailable (HTTP $statusCode). Please try again later.",
                code = statusCode.toString()
            )
        }
        val errorString = try {
            errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val (message, code) = NetworkErrorParser.parseErrorBody(errorString, defaultErrorMessage)
        Resource.Error(
            exception = HttpException(this),
            message = message,
            code = code
        )
    }
}

/**
 * Parses [HttpException] error body or extracts exception localized message.
 * Handles server offline and network connection failures with clear user-friendly error messages.
 */
fun <T> handleNetworkException(e: Throwable, fallbackMessage: String = "Network error"): Resource<T> {
    if (isServerNotConnectedError(e)) {
        return Resource.Error(
            exception = e,
            message = SERVER_NOT_CONNECTED_MESSAGE
        )
    }

    return if (e is HttpException) {
        val statusCode = e.code()
        if (statusCode in listOf(502, 503, 504)) {
            return Resource.Error(
                exception = e,
                message = "Server is temporarily unavailable (HTTP $statusCode). Please try again later.",
                code = statusCode.toString()
            )
        }
        val errorString = try {
            e.response()?.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        val (message, code) = NetworkErrorParser.parseErrorBody(errorString, fallbackMessage)
        Resource.Error(exception = e, message = message, code = code)
    } else {
        val msg = e.localizedMessage?.takeIf { it.isNotBlank() && !it.contains("Exception") }
            ?: fallbackMessage
        Resource.Error(exception = e, message = msg)
    }
}

/**
 * Safely executes a suspend network block returning [Response<T>], catching exceptions
 * and extracting server JSON error messages automatically.
 */
suspend inline fun <reified T> safeApiCall(
    defaultErrorMessage: String = "Operation failed",
    crossinline block: suspend () -> Response<T>
): Resource<T> {
    return try {
        block().toResource(defaultErrorMessage)
    } catch (e: Throwable) {
        handleNetworkException(e, defaultErrorMessage)
    }
}
