package org.demo.server;

import java.io.IOException;

import org.demo.config.JwtInterceptor;
import org.demo.config.RequestMarkerServerInterceptor;

import io.grpc.Server;
import io.grpc.ServerBuilder;

public class ServerApplication {
    public static void main(String[] args) throws InterruptedException, IOException {
        Server server = ServerBuilder.forPort(9030)
            .addService(new SocialMediaStreamServer())
            .intercept(new JwtInterceptor())
            .intercept(new RequestMarkerServerInterceptor())
            .build();
        server.start();
        Thread.sleep(10_000);
        server.shutdownNow();
    }
}
