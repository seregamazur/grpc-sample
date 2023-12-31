# grpc-sample

This has been created to illustrate the gRPC technology alongside Protobuf features in practise, including all 4 types of API:
- [Unary request](https://github.com/seregamazur/grpc-sample/blob/master/server/src/main/proto/social-media-stream.proto#L41)
- [Server streaming](https://github.com/seregamazur/grpc-sample/blob/master/server/src/main/proto/social-media-stream.proto#L43)
- [Client streaming](https://github.com/seregamazur/grpc-sample/blob/master/server/src/main/proto/social-media-stream.proto#L45)
- [Bidirectional streaming](https://github.com/seregamazur/grpc-sample/blob/master/server/src/main/proto/social-media-stream.proto#L47)

Client in Python, server in Java. Additionally, Java server module contains client in Java, so there are 2 clients in this repo.
To test it just run ```mvn clean install -Prun-server-and-client```, it will:
- generate classes and files using Protobuf
- run server and client, make demonstrative calls (check logs)