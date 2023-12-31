package org.demo.config;

import java.util.UUID;

import io.grpc.Metadata;
import io.grpc.ServerCall;
import io.grpc.ServerCallHandler;
import io.grpc.ServerInterceptor;

public class RequestMarkerServerInterceptor implements ServerInterceptor {

    private static final Metadata.Key<String> REQUEST_MARKER_HEADER
        = Metadata.Key.of("X-grpc-Request-Marker", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(ServerCall<ReqT, RespT> call, Metadata headers, ServerCallHandler<ReqT, RespT> next) {
        if (!headers.containsKey(REQUEST_MARKER_HEADER)) {
            headers.put(REQUEST_MARKER_HEADER, UUID.randomUUID().toString());
        }
        return next.startCall(call, headers);
    }
}
