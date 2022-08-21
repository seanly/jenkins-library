import dev.opsbox.jenkins.PipelineModel

def call(String yamlName) {

    def yaml = readYaml file: yamlName
    def model = new PipelineModel().load(yaml)

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

    timeout(time: 10, unit: 'MINUTES') {
        withEnv(model.variables) {
            withCredentials(secrets) {
                execSteps(model)
            }
        }
    }
}
