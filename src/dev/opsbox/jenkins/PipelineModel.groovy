package dev.opsbox.jenkins

class PipelineModel {

    // the container image the used to run the build
    def image
    def steps = []
    def afterSteps = []
    // variables set as env vars
    def variables = []
    // secrets set as env vars
    def secrets = [:]

    def load(def yaml) {
        this.image = yaml.image
        this.loadSteps(yaml)
        this.loadVariables(yaml.variables)
        this.loadSecrets(yaml.secrets)

        return this
    }

    def loadSteps(def yaml) {
        yaml.steps.each { YamlStep ->
            this.steps.add(loadStep(YamlStep))
        }
        yaml."after-steps"?.each { YamlStep ->
            this.afterSteps.add(loadStep(YamlStep))
        }
    }

    static def loadStep(def yaml) {

        if (yaml.parallel != null) {
            def parallelSteps = []
            yaml.parallel.each { yamlParallelStep ->
                parallelSteps.add(new StepModel().load(yamlParallelStep))
            }

            return new StepModel(
                    name: yaml.name,
                    image: yaml.image,
                    parallel: parallelSteps
            )

        } else {
            return new StepModel().load(yaml)
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
}
