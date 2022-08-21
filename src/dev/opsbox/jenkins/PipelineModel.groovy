package dev.opsbox.jenkins

class PipelineModel {

    // the container image the used to run the build
    def image
    def steps = []
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

            if (YamlStep.parallel != null) {

                def parallelSteps = []
                YamlStep.parallel.each { yamlParallelStep ->
                    parallelSteps.add(new StepModel().load(yamlParallelStep))
                }

                this.steps.add(new StepModel(
                        name: YamlStep.name,
                        image: YamlStep.image,
                        parallel: parallelSteps
                ))

            } else {
                def step = new StepModel().load(YamlStep)
                this.steps.add(step)
            }
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
