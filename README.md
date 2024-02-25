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
