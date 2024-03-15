#!/usr/local/bin/bash

run_commands() {
  local -n cmds=$1
  local -n order=$2
  local tmpfile=$(mktemp)
  local spin='-\|/'

  for name in "${order[@]}"; do
    local line_length=$(tput cols)
    local padding=$((line_length - ${#name} - 5))
    printf "%s%${padding}s" "$name"
    eval "${cmds[$name]}" >"$tmpfile" 2>&1 &
    local pid=$!

    local i=0
    while kill -0 $pid 2>/dev/null; do
      printf "\r%s%${padding}s${spin:$i:1}" "$name"
      sleep .1
      i=$(((i + 1) % 4))
    done

    if [ $? -eq 0 ]; then
      printf "\r\e[32m\xE2\x9C\x94 %s%${padding}s\e[K\n\e[0m" "$name"
    else
      echo "Error: $name failed."
      cat "$tmpfile"
    fi
  done

  clear
  rm "$tmpfile"
}

printf "\e[36mBuilding Docker images...\n\e[0m"

declare -A docker_commands=(
  ["Create Java Client image"]="docker build -f k8s/java-k8s/client/Dockerfile -t grpc-java-client ."
  ["Create Java Server image"]="docker build -f k8s/java-k8s/server/Dockerfile -t grpc-java-server ."
  ["Create Kotlin Client image"]="docker build -f k8s/kotlin-k8s/client/Dockerfile -t grpc-kotlin-client ."
  ["Create Kotlin Server image"]="docker build -f k8s/kotlin-k8s/server/Dockerfile -t grpc-kotlin-server ."
  ["Create Python Client image"]="docker build -f k8s/python-k8s/client/Dockerfile -t grpc-python-client ."
  ["Create Python Server image"]="docker build -f k8s/python-k8s/server/Dockerfile -t grpc-python-server ."
)

declare -a docker_command_order=(
  "Create Java Client image"
  "Create Java Server image"
  "Create Kotlin Client image"
  "Create Kotlin Server image"
  "Create Python Client image"
  "Create Python Server image"
)

run_commands docker_commands docker_command_order

# kubectl commands
deploy_java_server_and_clients="kubectl apply -f k8s/java-k8s/server/deployment.yaml && kubectl apply -f k8s/java-k8s/server/service.yaml && kubectl apply -f k8s/java-k8s/client/deployment.yaml && kubectl apply -f k8s/java-k8s/client/service.yaml && kubectl apply -f k8s/kotlin-k8s/client/deployment.yaml && kubectl apply -f k8s/kotlin-k8s/client/service.yaml && kubectl apply -f k8s/python-k8s/client/deployment.yaml && kubectl apply -f k8s/python-k8s/client/service.yaml"
delete_java_server_and_clients="kubectl delete pod java-client && kubectl delete pod kotlin-client && kubectl delete pod python-client && kubectl delete service java-client && kubectl delete service kotlin-client && kubectl delete service python-client && kubectl delete pod java-server && kubectl delete service grpc-crashing-server"
deploy_kotlin_server_and_clients="kubectl apply -f k8s/kotlin-k8s/server/deployment.yaml ; kubectl apply -f k8s/kotlin-k8s/server/service.yaml ; kubectl apply -f k8s/java-k8s/client/deployment.yaml && kubectl apply -f k8s/java-k8s/client/service.yaml && kubectl apply -f k8s/kotlin-k8s/client/deployment.yaml && kubectl apply -f k8s/kotlin-k8s/client/service.yaml && kubectl apply -f k8s/python-k8s/client/deployment.yaml && kubectl apply -f k8s/python-k8s/client/service.yaml"
delete_kotlin_server_and_clients="kubectl delete pod java-client && kubectl delete pod kotlin-client && kubectl delete pod python-client && kubectl delete service java-client && kubectl delete service kotlin-client && kubectl delete service python-client && kubectl delete pod kotlin-server && kubectl delete service grpc-crashing-server"
deploy_python_server_and_clients="kubectl apply -f k8s/python-k8s/server/deployment.yaml && kubectl apply -f k8s/python-k8s/server/service.yaml && kubectl apply -f k8s/java-k8s/client/deployment.yaml && kubectl apply -f k8s/java-k8s/client/service.yaml && kubectl apply -f k8s/kotlin-k8s/client/deployment.yaml && kubectl apply -f k8s/kotlin-k8s/client/service.yaml && kubectl apply -f k8s/python-k8s/client/deployment.yaml && kubectl apply -f k8s/python-k8s/client/service.yaml"
delete_python_server_and_clients="kubectl delete pod java-client && kubectl delete pod kotlin-client && kubectl delete pod python-client && kubectl delete service java-client && kubectl delete service kotlin-client && kubectl delete service python-client && kubectl delete pod python-server && kubectl delete service grpc-crashing-server"
java_logs="kubectl logs --since=1h java-server && sleep 2"
kotlin_logs="kubectl logs --since=1h kotlin-server && sleep 2"
python_logs="kubectl logs --since=1h python-server && sleep 2"

wait_for_run="sleep 10"

printf "\e[36mRequesting clients against Python server...\n\e[0m"
declare -A python_commands=(
  ["Deploy Python server and clients"]="$deploy_python_server_and_clients"
  ["Wait for run"]="$wait_for_run"
  ["Get logs"]="$python_logs"
  ["Delete Python server and clients"]="$delete_python_server_and_clients"
)

declare -a python_command_order=(
  "Deploy Python server and clients"
  "Wait for run"
  "Get logs"
  "Delete Python server and clients"
)

run_commands python_commands python_command_order

printf "\e[36mRequesting clients against Java server...\n\e[0m"
declare -A java_commands=(
  ["Deploy Java server and clients"]="$deploy_java_server_and_clients"
  ["Wait for run"]="$wait_for_run"
  ["Get logs"]="$java_logs"
  ["Delete Java server and clients"]="$delete_java_server_and_clients"
)

declare -a java_command_order=(
  "Deploy Java server and clients"
  "Wait for run"
  "Get logs"
  "Delete Java server and clients"
)

run_commands java_commands java_command_order

printf "\e[36mRequesting clients against Kotlin server...\n\e[0m"
declare -A kotlin_commands=(
  ["Deploy Kotlin server and clients"]="$deploy_kotlin_server_and_clients"
  ["Wait for run"]="$wait_for_run"
  ["Get logs"]="$kotlin_logs"
  ["Delete Kotlin server and clients"]="$delete_kotlin_server_and_clients"
)

declare -a kotlin_command_order=(
  "Deploy Kotlin server and clients"
  "Wait for run"
  "Get logs"
  "Delete Kotlin server and clients"
)

run_commands kotlin_commands kotlin_command_order

printf "\e[32mFinish\e\n[0m"
