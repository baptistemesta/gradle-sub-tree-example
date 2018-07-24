import org.gradle.api.Plugin
import org.gradle.api.Project

class ComPlugin implements Plugin<Project> {

    @Override
    void apply(Project project) {
        def customComTask = project.task("customComTask") {
            doFirst {
                project.logger.quiet("ComPlugin is applied")
            }
        }
        project.tasks.build.dependsOn customComTask
    }
}
