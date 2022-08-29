package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class ArchiveArtifactsStep extends Step{

    def _with = [:]

    def load(def yaml) {
        super.load(yaml)
        this._with = yaml['with']

        return this
    }

    @Override
    def run(Object args) {
        script.archiveArtifacts _with
    }
}
