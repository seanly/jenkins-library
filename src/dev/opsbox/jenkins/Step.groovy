package dev.opsbox.jenkins

import com.google.inject.Stage

abstract class Step {

    def script
    def image

    // parallel steps
    def parallel = []

    boolean isParallelStep() {
        return parallel.size() > 0
    }

    /**
     * step identify
     * type: script, jenkins, oes
     *   script: execute script
     *   groovy: execute groovy script
     *   oes/xxx: oes-steps-plugin steps
     */
    def use

    protected def load(def yaml) {
        if (yaml.image != null) {
            this.image = yaml.image
        }
        this.use = yaml.use

        return this
    }

    abstract def run(def args)
}
