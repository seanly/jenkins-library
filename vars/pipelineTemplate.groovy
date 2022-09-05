import dev.opsbox.jenkins.Pipeline

def call(String templateName, Map variables = [:], Map secrets = [:]) {
    try {
        def templateText = libraryResource("templates/${templateName}.yaml")
        pipelineExecute(templateText, variables, secrets)
    } catch (err) {
        throw err
    }
}
