#!/usr/local/bin/bash

default() {
    docker build --no-cache -f k8s/kotlin-k8s/client/Dockerfile -t grpc-kotlin-client . \
    && docker build --no-cache -f k8s/kotlin-k8s/server/Dockerfile -t grpc-kotlin-server . \
    && kubectl apply -f k8s/jwt_secret.yaml \
    && kubectl apply -f k8s/kotlin-k8s/server/deployment.yaml \
    && kubectl apply -f k8s/kotlin-k8s/server/service.yaml \
    && kubectl apply -f k8s/kotlin-k8s/client/deployment.yaml \
    && kubectl apply -f k8s/kotlin-k8s/client/service.yaml
}

default
