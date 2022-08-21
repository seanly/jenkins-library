package dev.opsbox.jenkins

class StepModel {
    // step name
    def name
    def image
    // env what only to run the step in
    def only = [:]
    // env that don't run the step in
    def except = [:]
    // parallel steps
    def parallel = []

    def use = "script"
    def run

    // trigger: manual|automatic, default=automatic

    def trigger

    boolean isParallelStep() {
        return parallel.size() > 0
    }

    boolean waitOnInput() {
        return "manual".equalsIgnoreCase(trigger)
    }

    def load(def yaml) {
        yaml.only.each { k, v ->
            this.only[k] = v
        }
        yaml.except.each {k, v ->
            this.except[k] = v
        }

        this.name = yaml.name
        this.image = yaml.image
        this.trigger = yaml.trigger

        this.use = yaml.use
        this.run = yaml.run

        return this
    }
}
