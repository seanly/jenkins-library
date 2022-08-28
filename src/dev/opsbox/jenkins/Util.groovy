package dev.opsbox.jenkins

class Util {

    static def getSecrets(def script, def secrets) {
        def ret = []
        if (secrets instanceof Map) {
            secrets?.each {
                def secretKey = it.key.replace("-", "_").toUpperCase()
                def secretEnvPrefix = "${secretKey}_"

                // usernamePassword/xxx-id
                def values = it.value.split("/")
                def type = values[0]
                def credId = values[1]

                if (type == "usernamePassword") {
                    ret << script.usernamePassword(credentialsId: credId,
                            usernameVariable: "${secretEnvPrefix}USR",
                            passwordVariable: "${secretEnvPrefix}PSW")
                } else if (type == "sshUserPrivateKey") {
                    ret << script.sshUserPrivateKey(credentialsId: credId,
                            keyFileVariable: "${secretEnvPrefix}KEYFILE",
                            passphraseVariable: "${secretEnvPrefix}KEYPASSWORD",
                            usernameVariable: "${secretEnvPrefix}USER")
                } else if (type == "file") {
                    ret << [
                            $class: 'FileBinding',
                            credentialsId: credId,
                            variable: "${secretKey}"
                    ]
                } else {
                    script.error "${type} not supported"
                }
            }
        }
        return ret
    }
}
