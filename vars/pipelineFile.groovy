import dev.opsbox.jenkins.Pipeline

def call(String yamlName, Map variables = [:], Map secrets = [:]) {
    try {
        def yamlText = readFile file: yamlName
        pipelineExecute(yamlText, variables, secrets)
    } catch (err) {
        throw err
    }
}
