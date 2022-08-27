package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class ScriptStep extends Step {

    def _code = ""

    def load(def yaml) {
        super.load(yaml)
        this._code = yaml["code"]

        return this
    }

    def run(def args) {
        if (image == null) {
            script.sh "${this._code}"
        } else {
            script.docker.image(image).inside(args) {
                script.sh "${this._code}"
            }
        }
    }
}
