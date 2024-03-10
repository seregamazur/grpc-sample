#!/bin/bash
usage(){
 cat << EOF

 Usage: $0 -f
 Description: To apply k8s manifests using the default \`kubectl apply -f\` command
[OR]
 Usage: $0 -k
 Description: To apply k8s manifests using the kustomize \`kubectl apply -k\` command
[OR]
 Usage: $0 -s
 Description: To apply k8s manifests using the skaffold binary \`skaffold run\` command

EOF
exit 0
}

logSummary() {
    echo ""
}

default() {
    docker build --no-cache -f k8s/python-k8s/client/Dockerfile -t grpc-python-client . \
    && docker build --no-cache -f k8s/python-k8s/server/Dockerfile -t grpc-python-server . \
    && kubectl apply -f k8s/python-k8s/server/deployment.yaml \
    && kubectl apply -f k8s/python-k8s/server/service.yaml \
    && kubectl apply -f k8s/python-k8s/client/deployment.yaml \
    && kubectl apply -f k8s/python-k8s/client/service.yaml \
    && kubectl apply -f k8s/namespace.yml \
    && kubectl apply -f k8s/jwt_secret.yaml
}

kustomize() {
    docker build --no-cache -f k8s/python-k8s/client/Dockerfile -t grpc-java-client . \
    && docker build --no-cache -f k8s/python-k8s/server/Dockerfile -t grpc-java-server . \
    && kubectl apply -k k8s/
}

#scaffold() {
#    // this will build the source and apply the manifests the K8s target. To turn the working directory
#    // into a CI/CD space, initilaize it with `skaffold dev`
#    skaffold run
#}

[[ "$@" =~ ^-[fks]{1}$ ]]  || usage;

while getopts ":fks" opt; do
    case ${opt} in
    f ) echo "Applying default \`kubectl apply -f\`"; default ;;
    k ) echo "Applying kustomize \`kubectl apply -k\`"; kustomize ;;
    s ) echo "Applying using skaffold \`skaffold run\`"; scaffold ;;
    \? | * ) usage ;;
    esac
done

logSummary
