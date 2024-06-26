#!/usr/bin/env bash

set +x

echo "Checking argocd namespace existence"
kubectl get ns argocd > /dev/null 2>&1

if [ $? != 0 ]; then
  echo "argocd namespace does not exist, creating..."
  kubectl create ns argocd > /dev/null 2>&1
fi

echo "Checking cf-token-metadata-registry namespace existence"
kubectl get ns cf-token-metadata-registry > /dev/null 2>&1

if [ $? != 0 ]; then
  echo "cf-token-metadata-registry namespace does not exist, creating..."
  kubectl create ns cf-token-metadata-registry > /dev/null 2>&1
fi

## DockerHub secret
kubectl create secret -n cf-token-metadata-registry generic regcred \
  --from-file=.dockerconfigjson=../../.keys/docker-cred.json \
  --type=kubernetes.io/dockerconfigjson \
  --save-config \
  --dry-run=client \
  -o yaml \
  | kubectl apply -f -

## Git Hub deploy key
kubectl create secret generic github-deploy-key \
  --save-config \
  --dry-run=client \
  -o yaml \
  -n argocd \
  --from-file=../../.keys/cf-token-metadata-registry \
  | kubectl apply -f -

exit 0

#echo "Fetching helm dependencies for main app"
helm dependency build

echo "Updating helm dependencies for main app"
helm dependency update

helm upgrade --install argocd -n argocd . \
  --set mainAppPath=deploy/main-app \
  --set git.targetRevision=infra-develop \
  --set valueFile=values-dev-mainnet.yaml \
  -f values-secrets.yaml \
  -f values-dev-mainnet.yaml



    git.targetRevision: infra-develop
    valueFile: values-dev-mainnet.yaml
