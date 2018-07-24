import org.gradle.api.Plugin
import org.gradle.api.Project

class OSSPlugin implements Plugin<Project> {
    @Override
    void apply(Project project) {
        def customOSSTask = project.task("customOSSTask") {
            doFirst {
                project.logger.quiet("OSSPlugin is applied")
            }
        }
        project.tasks.build.dependsOn customOSSTask
    }
}
