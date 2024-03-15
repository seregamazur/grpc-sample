#!/usr/local/bin/bash

default() {
    docker build --no-cache -f k8s/python-k8s/client/Dockerfile -t grpc-python-client . \
    && docker build --no-cache -f k8s/python-k8s/server/Dockerfile -t grpc-python-server . \
    && kubectl apply -f k8s/jwt_secret.yaml \
    && kubectl apply -f k8s/python-k8s/server/deployment.yaml \
    && kubectl apply -f k8s/python-k8s/server/service.yaml \
    && kubectl apply -f k8s/java-k8s/client/deployment.yaml \
    && kubectl apply -f k8s/java-k8s/client/service.yaml
}

default
