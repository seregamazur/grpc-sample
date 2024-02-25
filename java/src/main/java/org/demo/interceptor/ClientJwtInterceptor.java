package org.demo.interceptor;

import java.nio.charset.StandardCharsets;
import java.security.Key;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * Intercept call before sending to server, append JWT and use compression
 */
public class ClientJwtInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION_HEADER
        = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(
            //add grpc-encoding=gzip header
            channel.newCall(methodDescriptor, callOptions.withCompression("gzip"))) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (!headers.containsKey(AUTHORIZATION_HEADER)) {
                    headers.put(AUTHORIZATION_HEADER, getJwtValue());
                }
                super.start(responseListener, headers);
            }
        };
    }

    private String getJwtValue() {
        Key secretKey = Keys.hmacShaKeyFor(System.getenv("JWT_SECRET").getBytes(StandardCharsets.UTF_8));
        return "Bearer " + Jwts.builder()
            .subject("java-client")
            .signWith(secretKey)
            .compact();
    }

}
