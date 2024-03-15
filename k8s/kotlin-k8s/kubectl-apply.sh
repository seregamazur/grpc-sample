#!/bin/bash
usage(){
 cat << EOF

 Usage: $0 -f
 Description: To apply k8s manifests using the default \`kubectl apply -f\` command
EOF
exit 0
}

logSummary() {
    echo ""
}

default() {
    docker build --no-cache -f k8s/kotlin-k8s/client/Dockerfile -t grpc-kotlin-client . \
    && docker build --no-cache -f k8s/kotlin-k8s/server/Dockerfile -t grpc-kotlin-server . \
    && kubectl apply -f k8s/kotlin-k8s/server/deployment.yaml \
    && kubectl apply -f k8s/kotlin-k8s/server/service.yaml \
    && kubectl apply -f k8s/kotlin-k8s/client/deployment.yaml \
    && kubectl apply -f k8s/kotlin-k8s/client/service.yaml \
    && kubectl apply -f k8s/jwt_secret.yaml
}

[[ "$@" =~ ^-[fks]{1}$ ]]  || usage;

while getopts ":fks" opt; do
    case ${opt} in
    f ) echo "Applying default \`kubectl apply -f\`"; default ;;
    \? | * ) usage ;;
    esac
done

logSummary
