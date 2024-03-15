#!/usr/local/bin/bash

default() {
    docker build --no-cache -f k8s/java-k8s/client/Dockerfile -t grpc-java-client . \
    && docker build --no-cache -f k8s/java-k8s/server/Dockerfile -t grpc-java-server . \
    && kubectl apply -f k8s/jwt_secret.yaml \
    && kubectl apply -f k8s/java-k8s/server/deployment.yaml \
    && kubectl apply -f k8s/java-k8s/server/service.yaml \
    && kubectl apply -f k8s/java-k8s/client/deployment.yaml \
    && kubectl apply -f k8s/java-k8s/client/service.yaml
}

default
