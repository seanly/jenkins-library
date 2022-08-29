package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class OesStep extends Step {
    def _props = [:]
    def _id

    def load(def yaml) {
        super.load(yaml)
        this._id = yaml['id']
        this._props = yaml['props']

        return this
    }

    def run(def args) {
        /**
         * use: oesStep
         * id: sample
         * props:
         *  arg1: xxx
         */
        def stepArgs = []
        this._props.each {k, v ->
            stepArgs.add(this.script.stepProp(key: k, value: v))
        }
        script.oesStep stepId: _id, stepProps: stepArgs
    }
}
