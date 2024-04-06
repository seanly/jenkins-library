package dev.opsbox.jenkins

import com.google.inject.Stage

abstract class Step {

    def script
    def image
    def lock

    // parallel steps
    def parallel = []

    boolean isParallelStep() {
        return parallel.size() > 0
    }

    /**
     * step identify
     * type: script, groovy, ant
     */
    def use

    protected def load(def yaml) {
        if (yaml.image != null) {
            this.image = yaml.image
        }
        if (yaml.lock != null) {
            this.lock = yaml.lock
        }
        this.use = yaml.use

        return this
    }

    abstract def run(def args)
}
