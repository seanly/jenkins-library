package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class GroovyStep extends Step {
    def _id
    def _code

    def load(def yaml) {
        super.load(yaml)

        this._id = yaml["id"]
        this._code = yaml["code"]
        return this
    }

    def run(def args) {

        /**
         * config format:
         * steps:
         * - use: groovy
         *   id: test-xxx
         *   code: |
         *     def test() {
         *         echo "test"
         *     }
         *     test()
         *
         * do actions:
         * 1. write groovy
         * 2. load groovy
         * 3. run groovy func(run)
         */
        def groovyPath = "${this.script.env.OPSBOX_DIR}/${this._id}.groovy"
        this.script.writeFile(file: groovyPath, text: this._code)
        this.script.load(groovyPath)
    }
}
