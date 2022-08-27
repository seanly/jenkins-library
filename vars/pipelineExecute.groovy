import dev.opsbox.jenkins.Pipeline

import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field

@Field String opsboxDir = ".opsbox"
@Field String opsboxEnv = ".${opsboxDir}/env"

def call(String yamlName) {
    script = this
    def args = " -v /var/run/docker.sock:/var/run/docker.sock "

    env.OPSBOX_ENV = opsboxEnv
    env.OPSBOX_DIR = opsboxDir

    try {
        def yaml = readYaml file: yamlName
        def model = new Pipeline(script: script).load(yaml)

        timeout(time: 10, unit: 'MINUTES') {
            withEnv(model.variables) {
                withCredentials(getSecrets(model)) {
                    runStages(model, args)
                }
            }
        }
    } catch (err) {
        throw err
    }
}

def runStages(def model, def args) {
    model.stages.each { it ->
        stage(it.name) {
            it.run(args)
        }
    }
}

def getSecrets(def model) {
    def secrets = []
    if (model.secrets instanceof Map) {
        model.secrets?.each {
            def secretKey = it.key.replace("-", "_").toUpperCase()
            def secretEnvPrefix = "${secretKey}_"

            // usernamePassword/xxx-id
            def values = it.value.split("/")
            def type = values[0]
            def credId = values[1]

            if (type == "usernamePassword") {
                secrets << usernamePassword(credentialsId: credId,
                        usernameVariable: "${secretEnvPrefix}USR",
                        passwordVariable: "${secretEnvPrefix}PSW")
            } else if (type == "sshUserPrivateKey") {
                secrets << sshUserPrivateKey(credentialsId: credId,
                        keyFileVariable: "${secretEnvPrefix}KEYFILE",
                        passphraseVariable: "${secretEnvPrefix}KEYPASSWORD",
                        usernameVariable: "${secretEnvPrefix}USER")
            } else if (type == "FileBinding") {
                secrets << [
                        $class: 'FileBinding',
                        credentialsId: credId,
                        variable: "${envPrefix}${stepKey}_${secretKey}"
                ]
            } else {
                error "${type} not supported"
            }
        }
    }
    return secrets
}