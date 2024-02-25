# grpc-sample

This has been created to illustrate the gRPC technology features alongside Protobuf in practise, including all 4 types of API:

- [Unary request](https://github.com/seregamazur/grpc-sample/blob/master/proto/social-media-stream.proto#L41)
- [Server streaming](https://github.com/seregamazur/grpc-sample/blob/master/proto/social-media-stream.proto#L43)
- [Client streaming](https://github.com/seregamazur/grpc-sample/blob/master/proto/social-media-stream.proto#L45)
- [Bidirectional streaming](https://github.com/seregamazur/grpc-sample/blob/master/proto/social-media-stream.proto#L47)

Each module contains server and client. To test it just run ```mvn clean install -Prun-server-and-client```, it will:

- generate classes and files using Protobuf
- run server and client, make demonstrative calls (check logs)

We use blocking (non-async) stubs to make unary and server streaming requests. Async stub used for client streaming and bidireactional
streaming.
Used features:

- [SSL/TLS secured channel](https://grpc.io/docs/guides/auth/#supported-auth-mechanisms)
- [GZIP Compression](https://grpc.io/docs/guides/compression/)
- [Call deadline](https://grpc.io/docs/guides/deadlines/#deadlines-on-the-client)
- [JWT Metadata](https://grpc.io/docs/guides/metadata/)
- [Cancel call](https://grpc.io/docs/guides/cancellation/)
- Retry config

| Feature                 | Java Client                                                                                                                          | Java Server                                                                                                                          | Kotlin Client                                                                                                                          | Kotlin Server                                                                                                                          | Python Client                                                                                                                              | Python Server                                                                                                         |
| ----------------------- |--------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------|
| SSL/TLS secured channel | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/client/GrpcResilientClient.java#L194)      | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/server/GrpcCrashingServer.java#L151)       | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/client/GrpcResilientClient.kt#L172)      | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/server/GrpcCrashingServer.kt#L146)       | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/client/grpc_resilient_client.py#L62)                                  | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/server/grpc_crashing_server.py#L79)              |
| GZIP Compression        | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/interceptor/ClientJwtInterceptor.java#L28) | No need to define manually, included by default                                                                                      | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/interceptor/ClientJwtInterceptor.kt#L25) | No need to define manually, included by default                                                                                        | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/client/grpc_resilient_client.py#L71)                                  | No need to define manually, included by default                                                                       |
| Call deadline           | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/client/GrpcResilientClient.java#L66)       | -                                                                                                                                    | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/client/GrpcResilientClient.kt#L47)       | -                                                                                                                                      | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/client/grpc_resilient_client.py#L52)                                  | -                                                                                                                     |
| JWT Metadata            | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/interceptor/ClientJwtInterceptor.java#L31) | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/interceptor/ServerJwtInterceptor.java#L35) | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/interceptor/ClientJwtInterceptor.kt#L28) | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/interceptor/ServerJwtInterceptor.kt#L35) | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/interceptor/grpc_client_auth_interceptor.py#L7)                       | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/interceptor/grpc_server_auth_interceptor.py#L18) |
| Cancel call             | -                                                                                                                                    | [code](https://github.com/seregamazur/grpc-sample/blob/master/java/src/main/java/org/demo/server/GrpcCrashingServer.java#L34)        | -                                                                                                                                      | [code](https://github.com/seregamazur/grpc-sample/blob/master/kotlin/src/main/kotlin/org/demo/server/GrpcCrashingServer.kt#L25)        | -                                                                                                                                          | [code](https://github.com/seregamazur/grpc-sample/blob/master/python/server/grpc_crashing_server.py#L17)              |
| Retry config            | [json](https://github.com/seregamazur/grpc-sample/blob/master/retrying_config.json)                                                  | -                                                                                                                                    | [json](https://github.com/seregamazur/grpc-sample/blob/master/retrying_config.json)                                                    | -                                                                                                                                      | [Manual circuit breaker + retries](https://github.com/seregamazur/grpc-sample/blob/master/python/interceptor/grpc_client_retry_handler.py) | -                                                                                                                     |
