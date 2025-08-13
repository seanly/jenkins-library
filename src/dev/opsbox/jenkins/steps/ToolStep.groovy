package dev.opsbox.jenkins.steps

import dev.opsbox.jenkins.Step

class ToolStep extends Step {
    def _tools = [:]
    def _env = [:]
    def _code

    def load(def yaml) {
        super.load(yaml)
        this._tools = yaml['tools']
        this._env = yaml['env'] ?: [:]
        this._code = yaml['code']

        return this
    }

    def run(def args) {
        /**
         * use: tool
         * tools:
         *   ant: ant-v1.10.14
         *   maven: maven-v3.8.6
         *   gradle: gradle-v7.6
         * env:
         *   stepName: sample
         *   buildType: release
         *   apiKey: ${API_KEY}
         *   password: ${DB_PASSWORD}
         * code: |
         *  ant -f $ANT_HOME/run.xml -DstepName=$stepName -logger org.apache.tools.ant.NoBannerLogger
         *  mvn -f $MAVEN_HOME/pom.xml clean install
         *  gradle -p $GRADLE_HOME build
         */
        
        // Load all tools and create environment variables
        def _toolPaths = [:]
        def _envVars = []
        
        this._tools.each { toolName, toolVersion ->
            def _toolPath = script.tool toolVersion
            _toolPaths[toolName] = _toolPath
            _envVars.add("${toolName.toUpperCase()}_HOME=$_toolPath")
        }
        
        // Add environment variables
        this._env.each {k, v ->
            _envVars.add("${k}=${v}")
        }
        script.withEnv(_envVars) {
            if (script.isUnix()) {
                script.sh "${this._code}"
            } else {
                script.bat "${this._code}"
            }
        }
    }
}
