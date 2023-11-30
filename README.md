# oes-jenkins-library

## File Usage
```groovy

library identifier: 'objl@master', retriever: modernSCM(
  [$class: 'GitSCMSource',
   remote: 'https://github.com/opsbox-dev/oes-jenkins-library.git'])

properties([
        disableConcurrentBuilds(abortPrevious: true),
        buildDiscarder(logRotator(numToKeepStr: '5')),
])

node {
    checkout scm
    pipelineFile('.opsbox/pipeline.yml')
}

```

## Pipeline Syntax

```yaml
image: rockylinux:8

variables:
  DOCKER_REGISTRY: harbor.opsbox.dev
  
secrets:
  DOCKER_AUTH: usernamePassword/acr-docker-auth

stages:
  - name: build
    steps:
      - use: script
        code: |
          echo -n "${DOCKER_AUTH_PSW}" | docker login -u ${DOCKER_AUTH_USR} --password-stdin ${DOCKER_REGISTRY}
      - image: rockylinux:8
        use: script
        code: |
          echo "building step 1"
          echo "--//INFO: ${DOCKER_AUTH_USR}:${DOCKER_AUTH_PSW}"

    after-steps:
      - use: script
        lock: ${env.NODE_NAME}
        code: |
          docker logout ${DOCKER_REGISTRY}
          
  - name: test
    steps:
      - parallel:
          - name: test1
            except:
              BRANCH_NAME:
                - master
            steps:
              - use: script
                code: |
                  echo "unit testing"
    
          - name: test2
            only: 
              BRANCH_NAME: [develop, release/*]
            steps:
              - image: alpine
                use: script
                code: |
                  apk add docker

  - name: test antStep
    steps:
      - use: antStep
        tools:
          antHome: apache ant
          aslHome: ant steps library
        id: sample
        props: 
          arg1: hi, opsbox!

  - name: deploy
    trigger: manual
    only:
      BRANCH_NAME:
      - release/*
    steps:
      - use: script
        code: |
          echo "do deploy"
          docker version
    
```

## Inspiration

This project is inspired by [wolox-ci](https://github.com/Wolox/wolox-ci)

