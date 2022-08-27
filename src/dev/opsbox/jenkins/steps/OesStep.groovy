package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class OesStep extends Step {
    def _with = [:]

    def load(def yaml) {
        super.load(yaml)
        this._with = yaml['with']

        return this
    }

    def run(def args) {
        /**
         * use: oes/xxx
         */
        def oesStepId = this.use.substring(4)
        def stepArgs = []
        this._with.each {k, v ->
            stepArgs.add(this.script.stepProp(key: k, value: v))
        }
        script.oesStep stepId: oesStepId, stepProps: stepArgs
    }
}
