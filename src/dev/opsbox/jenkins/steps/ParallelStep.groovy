package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Stage
import dev.opsbox.jenkins.Step

class ParallelStep extends Step{

    def load(def yaml) {
        super.load(yaml)

        def parallelStages = []
        yaml["parallel"].each { yamlParallelStep ->
            parallelStages.add(new Stage(script: script).load(yamlParallelStep))
        }
        this.parallel = parallelStages

        return this
    }

    @Override
    def run(def args) {
        def stages = [:]
        parallel.each { pStage ->
            stages[pStage.name] = {
                script.stage(pStage.name) {
                    pStage.run(args)
                }
            }
        }
        script.parallel(stages)
    }
}
