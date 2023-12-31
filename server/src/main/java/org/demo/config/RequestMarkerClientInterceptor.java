package org.demo.config;

import java.util.UUID;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.ForwardingClientCall;
import io.grpc.Metadata;
import io.grpc.MethodDescriptor;

public class RequestMarkerClientInterceptor implements ClientInterceptor {

    private static final Metadata.Key<String> REQUEST_MARKER_HEADER
        = Metadata.Key.of("X-grpc-Request-Marker", Metadata.ASCII_STRING_MARSHALLER);

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(MethodDescriptor<ReqT, RespT> methodDescriptor, CallOptions callOptions, Channel channel) {
        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
            channel.newCall(methodDescriptor, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                if (!headers.containsKey(REQUEST_MARKER_HEADER)) {
                    headers.put(REQUEST_MARKER_HEADER, UUID.randomUUID().toString());
                }
                super.start(responseListener, headers);
            }
        };
    }

}
