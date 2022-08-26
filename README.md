# oes-jenkins-library

## Jenkinsfile
```groovy

library identifier: 'objl@master', retriever: modernSCM(
  [$class: 'GitSCMSource',
   remote: 'https://github.com/opsbox-dev/oes-jenkins-library.git'])

node {
  checkout scm
  execPipeline('.opsbox/pipeline.yml')
}

```

## `.opsbox/pipeline.yml`:

```yaml
image: rockylinux:8

variables:
  TEST_URL: http://example.com
  
secrets:
  DOCKER_AUTH: usernamePassword/acr-docker-auth

steps:

  - name: build
    image: rockylinux:8
    use: script
    run: |
      echo "building step 1"
      echo "--//INFO: ${DOCKER_AUTH_USR}:${DOCKER_AUTH_PSW}"

  - name: test
    parallel:
      - name: unit test
        except:
          BRANCH_NAME:
            - master
        use: script
        run: |
          echo "unit testing"

      - name: integration test
        image: alpine
        only: 
          BRANCH_NAME: [develop, release/*]
        use: script
        run: |
          apk add docker

  - name: test oesStep
    use: oes/sample
    run: 
      arg1: hi, opsbox!

  - name: deploy
    trigger: manual
    only:
      - release/*
    use: script
    run: |
      echo "do deploy"
      docker version

```

## Inspiration
This project is inspired by [wolox-ci](https://github.com/Wolox/wolox-ci)

