import dev.opsbox.jenkins.Pipeline

def call(String yamlName, Map variables = [:], Map secrets = [:], List filter = []) {
    try {
        def yamlText = readFile file: yamlName
        pipelineExecute(yamlText, variables, secrets, filter)
    } catch (err) {
        throw err
    }
}
