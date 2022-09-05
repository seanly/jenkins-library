import dev.opsbox.jenkins.Pipeline

import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field

@Field String opsboxDir = ".opsbox"
@Field String opsboxEnv = "${opsboxDir}/env"

def call(String yamlText, Map variables = [:], Map secrets = [:]) {
    script = this
    def args = " -v /var/run/docker.sock:/var/run/docker.sock "

    env.OPSBOX_ENV = opsboxEnv
    env.OPSBOX_DIR = opsboxDir

    sh "mkdir -p ${env.OPSBOX_DIR}"

    try {
        def yaml = readYaml text: yamlText
        def model = new Pipeline(script: script).load(yaml)

        variables.each {k, v ->
            model.variables.add("$k=$v")
        }

        secrets.each {k, v ->
            model.secrets[k] = v
        }

        model.run(args)
    } catch (err) {
        throw err
    }
}
