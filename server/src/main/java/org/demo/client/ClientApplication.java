package org.demo.client;

import org.demo.config.RequestMarkerClientInterceptor;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

public class ClientApplication {
    public static void main(String[] args) throws InterruptedException {
        ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 9030)
            .usePlaintext()
            .intercept(new RequestMarkerClientInterceptor())
            .build();
        SocialMediaStreamClient client = new SocialMediaStreamClient(channel);
        client.downloadStream();
        Thread.sleep(5_000);
        client.watchStream();
        Thread.sleep(5_000);
        client.startStream();
        Thread.sleep(5_000);
        client.joinInteractStream();
        Thread.sleep(5_000);
        channel.shutdown();
    }
}
