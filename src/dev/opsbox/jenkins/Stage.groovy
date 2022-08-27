package dev.opsbox.jenkins
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import dev.opsbox.jenkins.steps.GroovyStep
import dev.opsbox.jenkins.steps.OesStep
import dev.opsbox.jenkins.steps.ParallelStep
import dev.opsbox.jenkins.steps.ScriptStep

class Stage {
    def script
    def name
    def image
    // env what only to run the step in
    def only = [:]
    // env that don't run the step in
    def except = [:]

    // trigger: manual|automatic, default=automatic
    def trigger = "automatic"

    def steps = []
    def afterSteps = []

    boolean waitOnInput() {
        return "manual".equalsIgnoreCase(trigger)
    }

    def load(def yaml) {
        yaml.only.each { k, v ->
            this.only[k] = v
        }
        yaml.except.each { k, v ->
            this.except[k] = v
        }

        this.name = yaml.name
        this.trigger = yaml.trigger

        this.loadSteps(yaml)
        return this
    }

    def loadSteps(def yaml) {
        yaml.steps.each { YamlStep ->
            this.steps.add(this.loadStep(YamlStep))
        }
        yaml."after-steps"?.each { YamlStep ->
            this.afterSteps.add(this.loadStep(YamlStep))
        }
    }

    def loadStep(def yaml) {

        if (yaml.parallel != null) {
            return new ParallelStep(script: script, image: image).load(yaml)
        }

        switch (yaml.use) {
            case "script":
                return new ScriptStep(script: script, image: image).load(yaml)
            case "groovy":
                return new GroovyStep(script: script, image: image).load(yaml)
        }

        if (yaml.use =~ "oes/*") {
            return new OesStep(script: script).load(yaml)
        }

        throw new Exception("yaml.use(${yaml.use}) is not support")
    }

    def run(def args) {

        if (except.size() > 0 && globContains(except)) {
            Utils.markStageSkippedForConditional(script.env.STAGE_NAME)
            return
        }

        if (only.size() > 0 && !globContains(only)) {
            Utils.markStageSkippedForConditional(script.env.STAGE_NAME)
            return
        }

        if (waitOnInput()) {
            try {
                script.timeout(time: 5, unit: 'MINUTES') {
                    script.input()
                }
            } catch (err) {
                def user = err.getCauses()[0].getUser()
                if ('SYSTEM' == user.toString()) { // SYSTEM means timeout.
                    script.error("build timed out")
                } else {
                    script.error("Aborted by: [${user}]")
                }
                return
            }
        }

        try {
            runSteps(steps, args)
        } catch (err) {
            throw err
        } finally {
            runSteps(afterSteps, args)
        }
    }

    def runSteps(def steps, def args) {
        steps.each { step ->
            if (step.isParallelStep()) {
                step.run(args)
            } else {
                runStep(step, args)
            }
        }
    }

    def runStep(def step, def args) {
        // build before
        script.sh "mkdir -p ${script.env.OPSBOX_DIR}"

        // build
        step.run(args)
        // build after

        /**
         * 注入环境变量
         * 分析 .opsbox/env 文件
         * 格式： <name>=<value>
         */
        if (script.fileExists(script.env.OPSBOX_ENV)) {
            def envstrs = extractLines(script.readFile(file: script.env.OPSBOX_ENV))
            envstrs.each { line ->
                def envstr = line.split("=")
                script.env."${envstr[0]}" = envstr[1]
            }
        }
    }

    static def extractLines(def content) {
        List myKeys = []
        content.eachLine { line ->
            myKeys << line
        }
        return myKeys
    }

    boolean globContains(def when) {

        /**
         *
         * only:
         *   BRANCH:
         *   - master
         */
        def ret = true
        // k = env[k]
        // v = match list, ex: master release/**
        when.each { k, v ->
            ret = v.find { env."${k}" =~ it }
            if (ret != true) {
                return false
            }
        }
        return ret
    }
}