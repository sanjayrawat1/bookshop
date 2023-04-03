#!/bin/sh

echo "\n🏴️ Destroying Kubernetes cluster...\n"

minikube stop --profile bookshop

minikube delete --profile bookshop

echo "\n🏴️ Cluster destroyed\n"
