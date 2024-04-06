# opsbox-ci

当使用此库时，看起来如下:

```groovy
#!groovy

library identifier: 'jenkins-library@main', retriever: modernSCM(
  [$class: 'GitSCMSource', remote: 'https://gitee.com/seanly/jenkins-library.git'])

node {

    properties([
            // Keep only the last 10 build to preserve space
            buildDiscarder(logRotator(numToKeepStr: '10')),
            // Don't run concurrent builds for a branch, because they use the same workspace directory
            disableConcurrentBuilds(abortPrevious: true)
    ])


    checkout scm

    pipelineFile('.opsbox/ci.yaml')
}

```

# 流水线语法

在 pipeline 这级包含 `timeout`、`image`、`variables`、`secrets` 和 `stages` 这个关键字

```yaml

# 构建超时时间. 默认值：10 (单位分钟). 
timeout: 10

# 运行任务时环境. 将steps任务执行命令放在 image 启动的容器中执行。这里是一个全局配置，step配置中可以覆盖。 
image: seanly/toolset:brew

# 全局环境变量
variables: 
  DOCKER_REGISTRY: harbor.opsbox.dev

# 敏感信息变量
secrets:
  DOCKER_AUTH: usernamePassword/acr-docker-auth

# 流水线执行阶段
stages:
  - name: build # 阶段显示名字
    steps: # step 任务
      - use: script # 任务类型，目前支持 script/groovy/ant/parallel, 其中 parallel 是内部的语法是stages的语法，用于并行执行多个stages。
        code: |
          echo -n "${DOCKER_AUTH_PSW}" | docker login -u ${DOCKER_AUTH_USR} --password-stdin ${DOCKER_REGISTRY}
      - image: rockylinux:8
        use: script
        code: |
          echo "building step 1"
          echo "--//INFO: ${DOCKER_AUTH_USR}:${DOCKER_AUTH_PSW}"

    after-steps: # step 任务，用于资源清理，比如登出。
      - use: script
        lock: node
        code: |
          docker logout ${DOCKER_REGISTRY}

```
