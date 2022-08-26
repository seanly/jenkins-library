import org.jenkinsci.plugins.pipeline.modeldefinition.Utils
import com.cloudbees.groovy.cps.NonCPS
import groovy.transform.Field

@Field String opsboxDir = ".opsbox"
@Field String opsboxEnv = ".${opsboxDir}/env"

def call(def model) {
    def args = " -v /var/run/docker.sock:/var/run/docker.sock "

    env.OPSBOX_ENV = opsboxEnv
    env.OPSBOX_DIR = opsboxDir

    try {
        buildSteps(model, model.steps, args)
    } catch (err) {
        throw err
    } finally {
        buildSteps(model, model.afterSteps, args)
    }
}

def buildSteps(def model, def steps, def args) {
    steps.each { step ->
        if (step.image == null) {
            step.image = model.image
        }
        stage(step.name) {
            if (step.isParallelStep()) {
                def stages = [:]
                step.parallel.each { pStep ->
                    stages[pStep.name] = {
                        stage(pStep.name) {
                            if (pStep.image == null) {
                                pStep.image = step.image
                            }
                            buildStep(pStep, args)
                        }
                    }
                }
                parallel(stages)
            } else {
                buildStep(step, args)
            }
        }
    }
}

def buildStep(def step, def args) {
    if (step.except.size() > 0 && globContains(step.except)) {
        Utils.markStageSkippedForConditional(env.STAGE_NAME)
        return
    }

    if (step.only.size() > 0 && !globContains(step.only)) {
        Utils.markStageSkippedForConditional(env.STAGE_NAME)
        return
    }

    if (step.waitOnInput()) {
        try {
            timeout(time: 5, unit: 'MINUTES') {
                input()
            }
        } catch (err) {
            def user = err.getCauses()[0].getUser()
            if ('SYSTEM' == user.toString()) { // SYSTEM means timeout.
                println("build timed out")
            } else {
                userInput = false
                println("Aborted by: [${user}]")
            }
            return
        }
    }

    if (step.image != null) {
        docker.image(step.image).inside(args) {
            runStep(step)
        }
    } else {
        runStep(step)
    }
}

def runStep(def step) {
    // build before
    sh "mkdir -p ${opsboxDir}"

    // build
    if (step.use == "script") {
        sh step.run
    } else if(step.use == "groovy") {

        /**
         * config format:
         * steps:
         * - name: build
         *   use: groovy
         *   id: test-xxx
         *   run: |
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
        def scriptFile = step.name.replaceAll(" ", "-")
        def groovyPath = "${opsboxDir}/${scriptFile}.groovy"
        writeFile(file: groovyPath, text: step.run)
        load(groovyPath)
    } else if(step.use =~ "oes/*") {
        def oesStepId = step.use.substring(4)

        def stepArgs = []
        step.run.each {k, v ->
            stepArgs.add(stepProp(key: k, value: v))
        }
        oesStep stepId: oesStepId, stepProps: stepArgs
    } else {
        error "--//ERR: step configure error."
    }

    // build after
    /**
     * 注入环境变量
     * 分析 .opsbox/env 文件
     * 格式： <name>=<value>
     */
    if (fileExists(opsboxEnv)) {
        def envstrs = extractLines(readFile(file:opsboxEnv))
        envstrs.each { line ->
            def envstr = line.split("=")
            env."${envstr[0]}" = envstr[1]
        }
    }
}

@NonCPS
List extractLines(final String content) {
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
    when.each {k, v ->
        ret = v.find { env."${k}" =~ it }
        if (ret != true) {
            return false
        }
    }
    return ret
}
