package org.demo.interceptor

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.MethodDescriptor
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import java.nio.charset.StandardCharsets

class ClientJwtInterceptor : ClientInterceptor {

    val authHeader: io.grpc.Metadata.Key<String> =
        io.grpc.Metadata.Key.of("Authorization", io.grpc.Metadata.ASCII_STRING_MARSHALLER)

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        methodDescriptor: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        channel: Channel?
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            //add grpc-encoding=gzip header
            channel?.newCall(methodDescriptor, callOptions?.withCompression("gzip"))
        ) {
            override fun start(responseListener: Listener<RespT>, headers: io.grpc.Metadata) {
                if (!headers.containsKey(authHeader)) {
                    headers.put(authHeader, getJwtValue())
                }
                super.start(responseListener, headers)
            }
        }
    }

    fun getJwtValue(): String {
        val secretKey = Keys.hmacShaKeyFor(System.getenv("JWT_SECRET").toByteArray(StandardCharsets.UTF_8))
        return "Bearer " + Jwts.builder()
            .subject("kotlin-client")
            .signWith(secretKey)
            .compact()
    }

}
