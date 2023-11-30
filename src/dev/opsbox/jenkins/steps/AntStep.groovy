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
        this._name = yaml['name']
        this._props = yaml['props']

        return this
    }

    def run(def args) {
        /**
         * use: ant
         * tools: 
            ant: ant-v1.10.14
            steps: steps-v1.0.0
         * id: sample-test
         * name: sample
         * props:
         *  arg1: xxx
         */
        def _stepProps = "basedir=${script.env.WORKSPACE}\n"
        this._props.each {k, v ->
            _stepProps += "${k}=${v}\n"
        }

        def _run_properties = ".oes/run/${_name}-${_id}.properties"
        def _run_step = _name

        script.writeFile encoding: 'UTF-8', file: _run_properties, text: _stepProps

        def _ant = script.tool this._tools['ant']
        def _steps = script.tool this._tools['steps']

        script.withEnv(["ANT_HOME=$_ant", 
                        "STEPS_ROOT=$_steps", 
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
