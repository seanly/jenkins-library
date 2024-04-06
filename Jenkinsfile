#!groovy

library identifier: 'jenkins-library@main', retriever: legacySCM(scm)

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