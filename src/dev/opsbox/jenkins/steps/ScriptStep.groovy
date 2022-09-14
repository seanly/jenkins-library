package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class ScriptStep extends Step {

    def _code = ""
    def _type = "sh"

    def load(def yaml) {
        super.load(yaml)
        this._code = yaml["code"]
        this._type = yaml["type"]

        if (this._type == null) {
            this._type = "sh"
        }

        return this
    }

    def run(def args) {
        if (this._type == "sh") {
            runShScript(args)
        }

        switch (this._type) {
            case "bat":
                script.bat "${this._code}"
                break
            case "powershell":
                script.powershell "${this._code}"
                break
            case "pwsh":
                script.pwsh "${this._code}"
                break
            default:
                runShScript(args)
        }
    }

    def runShScript(def args) {
        if (image == null) {
            script.sh "${this._code}"
        } else {
            script.docker.image(image).inside(args) {
                script.sh "${this._code}"
            }
        }
    }
}
