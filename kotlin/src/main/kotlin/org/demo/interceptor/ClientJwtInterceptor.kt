package org.demo.org.demo.interceptor

import io.grpc.CallOptions
import io.grpc.Channel
import io.grpc.ClientCall
import io.grpc.ClientInterceptor
import io.grpc.ForwardingClientCall
import io.grpc.MethodDescriptor
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import javax.crypto.spec.SecretKeySpec

class ClientJwtInterceptor : ClientInterceptor {

    val authHeader: io.grpc.Metadata.Key<String> =
        io.grpc.Metadata.Key.of("Authorization", io.grpc.Metadata.ASCII_STRING_MARSHALLER)

    override fun <ReqT : Any?, RespT : Any?> interceptCall(
        methodDescriptor: MethodDescriptor<ReqT, RespT>?,
        callOptions: CallOptions?,
        channel: Channel?
    ): ClientCall<ReqT, RespT> {
        return object : ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            channel?.newCall(methodDescriptor, callOptions)
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
        val secretKey = SecretKeySpec(
            System.getenv("JWT_SECRET").toByteArray(),
            SignatureAlgorithm.HS256.jcaName
        )
        return "Bearer " + Jwts.builder()
            .setSubject("kotlin-client")
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact()
    }

}
