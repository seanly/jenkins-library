package dev.opsbox.jenkins

class Pipeline {

    def timeout = 10

    // the container image the used to run the build
    def image
    // variables set as env vars
    def variables = []
    // secrets set as env vars
    def secrets = [:]
    def stages = []

    def script

    def load(def yaml) {
        if (yaml.timeout != null) {
            this.timeout = yaml.timeout
        }
        this.image = yaml.image
        this.loadVariables(yaml.variables)
        this.loadSecrets(yaml.secrets)
        this.loadStages(yaml.stages)

        return this
    }

    def loadStages(def yaml) {
        yaml.each { YamlStage ->
            this.stages.add(new Stage(script: script, image: image).load(YamlStage))
        }
    }

    def loadVariables(def yaml) {
        yaml.each { k, v ->
            this.variables.add("$k=$v")
        }
    }

    def loadSecrets(def yaml) {
        yaml.each { k, v ->
            this.secrets[k] = v
        }
    }

    def run(def args, def filter=[]) {
        script.timeout(time: this.timeout, unit: 'MINUTES') {
            script.withEnv(variables) {
                script.withCredentials(Util.getSecrets(script, secrets)) {
                    runStages(args, filter)
                }
            }
        }
    }

    def runStages(def args, def filter=[]) {
        this.stages.each { it ->
            if (filter.size() == 0) {
                it.run(args)
            } else {
                if (filter.contains(it.name)) {
                    it.run(args)
                }
            }
        }
    }
}
