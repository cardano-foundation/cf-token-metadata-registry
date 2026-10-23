VERSION 0.8

# Docker images for the token metadata registry.
#
# Tag names and registry selection are not decided here: cf-gha-baseline
# (cardano-foundation/cf-gha-workflows) provides
#   DOCKER_REGISTRIES_FILTERED      - public registries only for release tags,
#                                     private registries for every build
#   EARTHLY_DOCKER_IMAGES_EXTRA_TAGS - PR / branch / tag specific image tags
# and the shared functions below turn those into pushed images:
#   functions+DOCKER_TAG_N_PUSH     - tag and push registry/<name>:<tag>
#
# CI builds one target per invocation, named "<app>-<variant>", e.g.
#   earthly +api-jvm --PUSH=true --DOCKER_REGISTRIES="$DOCKER_REGISTRIES_FILTERED" \
#     --DOCKER_IMAGES_EXTRA_TAGS="$EARTHLY_DOCKER_IMAGES_EXTRA_TAGS"

IMPORT --allow-privileged github.com/cardano-foundation/cf-gha-workflows/./earthfiles/functions:main AS functions

ARG --global DOCKER_IMAGE_PREFIX="cf-token-metadata-registry"
ARG --global DOCKER_IMAGES_TARGETS="api-jvm api-native job-jvm"
ARG --global PUSH=false
ARG --global DOCKER_REGISTRIES=""
ARG --global DOCKER_IMAGES_EXTRA_TAGS=""

all:
  FOR image_target IN $DOCKER_IMAGES_TARGETS
    BUILD +$image_target --PUSH=$PUSH
  END

docker-publish:
  BUILD +all --PUSH=$PUSH

api-jvm:
  LET DOCKER_IMAGE_NAME=${DOCKER_IMAGE_PREFIX}-api
  WAIT
    FROM DOCKERFILE -f api/Dockerfile api
  END
  WAIT
    SAVE IMAGE ${DOCKER_IMAGE_NAME}:latest
  END
  DO functions+DOCKER_TAG_N_PUSH \
    --PUSH=$PUSH \
    --DOCKER_IMAGE_NAME=${DOCKER_IMAGE_NAME} \
    --DOCKER_IMAGES_EXTRA_TAGS="${DOCKER_IMAGES_EXTRA_TAGS}" \
    --DOCKER_REGISTRIES="${DOCKER_REGISTRIES}"

api-native:
  LET DOCKER_IMAGE_NAME=${DOCKER_IMAGE_PREFIX}-api-native
  WAIT
    FROM DOCKERFILE -f api/Dockerfile.native .
  END
  WAIT
    SAVE IMAGE ${DOCKER_IMAGE_NAME}:latest
  END
  DO functions+DOCKER_TAG_N_PUSH \
    --PUSH=$PUSH \
    --DOCKER_IMAGE_NAME=${DOCKER_IMAGE_NAME} \
    --DOCKER_IMAGES_EXTRA_TAGS="${DOCKER_IMAGES_EXTRA_TAGS}" \
    --DOCKER_REGISTRIES="${DOCKER_REGISTRIES}"

job-jvm:
  LET DOCKER_IMAGE_NAME=${DOCKER_IMAGE_PREFIX}-job
  WAIT
    FROM DOCKERFILE -f job/Dockerfile job
  END
  WAIT
    SAVE IMAGE ${DOCKER_IMAGE_NAME}:latest
  END
  DO functions+DOCKER_TAG_N_PUSH \
    --PUSH=$PUSH \
    --DOCKER_IMAGE_NAME=${DOCKER_IMAGE_NAME} \
    --DOCKER_IMAGES_EXTRA_TAGS="${DOCKER_IMAGES_EXTRA_TAGS}" \
    --DOCKER_REGISTRIES="${DOCKER_REGISTRIES}"
