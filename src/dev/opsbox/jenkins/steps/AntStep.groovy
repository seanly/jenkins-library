package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class AntStep extends Step {
    def _tools = [:]
    def _props = [:]
    def _id

    def load(def yaml) {
        super.load(yaml)
        this._tools = yaml['tools']
        this._id = yaml['id']
        this._props = yaml['props']

        return this
    }

    def run(def args) {
        /**
         * use: antStep
         * tools: 
            antHome: apache ant
            aslHome: ant steps library
         * id: sample
         * props:
         *  arg1: xxx
         */
        def _stepProps = "basedir=${env.WORKSPACE}\n"
        this._props.each {k, v ->
            _stepProps += "${k}=${v}\n"
        }

        def _run_properties = ".oes/run/${_id}.properties"
        def _run_step = _id

        writeFile encoding: 'UTF-8', file: _run_properties, text: _stepProps

        antHome = tool this._tools['antHome']
        aslHome = tool this._tools['aslHome']

        script.withEnv(["ANT_HOME=$antHome", 
                        "STEPS_ROOT=$aslHome", 
                        "STEP_NAME=$_run_step",
                        "STEP_PROPS=$_run_properties"]) {
            if (script.isUnix()) {
                script.sh '"$ANT_HOME/bin/ant" -f $STEPS_ROOT/${STEP_NAME}/run.xml -propertyfile ${STEP_PROPS} -logger org.apache.tools.ant.NoBannerLogger'
            } else {
                script.bat(/%ANT_HOME\bin\ant -f %STEPS_ROOT%\%STEP_NAME%\run.xml -propertyfile %STEP_PROPS% -logger org.apache.tools.ant.NoBannerLogger/)
            }
        }
    }
}
