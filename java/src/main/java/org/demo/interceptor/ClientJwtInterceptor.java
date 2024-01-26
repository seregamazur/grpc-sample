package org.demo.interceptor;

import java.security.Key;

import javax.crypto.spec.SecretKeySpec;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;


public class ClientJwtInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> AUTHORIZATION_HEADER
        = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            channel.newCall(methodDescriptor, callOptions)) {
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
        Key secretKey = new SecretKeySpec(System.getenv("JWT_SECRET").getBytes(),
            SignatureAlgorithm.HS256.getJcaName());
        return "Bearer " + Jwts.builder()
            .setSubject("java-client")
            .signWith(SignatureAlgorithm.HS256, secretKey)
            .compact();
    }

}