package org.demo.interceptor

import io.grpc.Context
import io.grpc.Contexts
import io.grpc.Metadata
import io.grpc.Metadata.ASCII_STRING_MARSHALLER
import io.grpc.ServerCall
import io.grpc.ServerCallHandler
import io.grpc.ServerInterceptor
import io.grpc.Status
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys

class ServerJwtInterceptor : ServerInterceptor {

    companion object {
        private const val BEARER_TYPE = "Bearer"

        private val AUTHORIZATION_METADATA_KEY = Metadata.Key.of("Authorization", ASCII_STRING_MARSHALLER)
        private val CLIENT_ID_CONTEXT_KEY: Context.Key<String> = Context.key("clientId")
        private val parser = Jwts.parser().verifyWith(Keys.hmacShaKeyFor(System.getenv("JWT_SECRET").toByteArray())).build()
        private val clients: List<String> = listOf("kotlin-client", "python-client", "java-client")
    }

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        call: ServerCall<ReqT, RespT>?,
        headers: Metadata?,
        next: ServerCallHandler<ReqT, RespT>?
    ): ServerCall.Listener<ReqT> {
        val value = headers?.get(AUTHORIZATION_METADATA_KEY)

        val status: Status
        try {
            val token = value?.substring(BEARER_TYPE.length)?.trim()
            val claims = parser.parseSignedClaims(token)
            assert(!clients.none { c -> c.contentEquals(claims.payload.subject) })
            val ctx = Context.current().withValue(CLIENT_ID_CONTEXT_KEY, claims.payload.subject)
            return Contexts.interceptCall(ctx, call, headers, next)
        } catch (e: Exception) {
            when (e) {
                is JwtException, is IllegalArgumentException -> {
                    status = Status.UNAUTHENTICATED.withDescription(e.message).withCause(e)
                    call?.close(status, headers)
                }
            }
        }
        return object : ServerCall.Listener<ReqT>() {
        }
    }
}