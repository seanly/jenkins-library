package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class GroovyStep extends Step {
    def _code

    def load(def yaml) {
        super.load(yaml)

        this._code = yaml["code"]
        return this
    }

    def run(def args) {

        /**
         * config format:
         * steps:
         * - use: groovy
         *   code: |
         *     def test() {
         *         echo "test"
         *     }
         *     test()
         *
         * do actions:
         * 1. evaluate groovy code directly
         */
        this.script.evaluate(this._code)
    }
}
