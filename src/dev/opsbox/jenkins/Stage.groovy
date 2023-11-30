package dev.opsbox.jenkins

import com.cloudbees.groovy.cps.NonCPS
import dev.opsbox.jenkins.steps.ArchiveArtifactsStep
import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import dev.opsbox.jenkins.steps.GroovyStep
import dev.opsbox.jenkins.steps.AntStep
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

    // variables set as env vars
    def variables = []
    // secrets set as env vars
    def secrets = [:]

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

        this.loadVariables(yaml.variables)
        this.loadSecrets(yaml.secrets)
        this.loadSteps(yaml)
        return this
    }

    def loadVariables(def yaml) {
        yaml.each { k, v ->
            this.variables.add("$k=$v")
        }
    }

    def loadSecrets(def yaml) {
        yaml.each { k, v ->
            this.secrets[k] = v
        }
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
            case "archiveArtifacts":
                return new ArchiveArtifactsStep(script: script).load(yaml)
            case "antStep":
                return new AntStep(script: script).load(yaml)
        }

        throw new Exception("yaml.use(${yaml.use}) is not support")
    }

    def run(def args) {
        script.stage(name) {
            script.withEnv(variables) {
                script.withCredentials(Util.getSecrets(script, secrets)) {
                    runStage(args)
                }
            }
        }
    }

    def runStage(def args) {

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

        if (step.lock == null) {
            // build
            step.run(args)
        } else {
            script.lock(script.env.NODE_NAME) {
                step.run(args)
            }
        }

        // build after
        /**
         * 注入环境变量
         * 分析 .opsbox/env 文件
         * 格式： <name>=<value>
         */
        if (script.fileExists(script.env.OPSBOX_ENV)) {
            def envstrs = extractLines(script.readFile(file: script.env.OPSBOX_ENV))
            envstrs.each { line ->
                if (line =~ /^#.*/) {
                    return
                }
                def envstr = line.split("=")
                script.env."${envstr[0]}" = envstr[1]
            }
        }
    }

    @NonCPS
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
            def matchItem = v.find { script.env."${k}" =~ /^${it}$/ }
            if ( matchItem == null) {
                ret = false
                return
            }
        }
        return ret
    }
}
