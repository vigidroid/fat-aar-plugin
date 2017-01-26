package me.vigi.fataar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * Created by Vigi on 2017/1/14.
 */
class FatLibraryPlugin implements Plugin<Project> {

    private Project project
    private Configuration embedConf

    private Set<ResolvedArtifact> artifacts

    @Override
    void apply(Project project) {
        this.project = project
        createConfiguration()
        project.afterEvaluate {
            resolveArtifacts()
            project.android.libraryVariants.all { variant ->
                processVariant(variant)
            }
        }
    }

    private void createConfiguration() {
        embedConf = project.configurations.create('embed')
        embedConf.visible = false
        project.gradle.addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {
                embedConf.dependencies.each { dependency ->
                    project.dependencies.add('compile', dependency)
                }
                project.gradle.removeListener(this)
            }

            @Override
            void afterResolve(ResolvableDependencies resolvableDependencies) {}
        })
    }

    private void resolveArtifacts() {
        def set = new HashSet<>()
        embedConf.resolvedConfiguration.resolvedArtifacts.each { artifact ->
            // jar file wouldn't be here
            if ('aar'.equals(artifact.type) || 'jar'.equals(artifact.type)) {
                println 'vigi-->artifact=[' + artifact.type + ']' + artifact.moduleVersion.id + ', file=' + artifact.file
            } else {
                throw new ProjectConfigurationException('Only support embed .aar and .jar dependencies!', null)
            }
            set.add(artifact)
        }
        artifacts = Collections.unmodifiableSet(set)
    }

    private void processVariant(variant) {
        if ('debug'.equals(variant.buildType.name)) {
            def prepareTask = project.tasks.findByPath('prepare' + variant.name.capitalize() + 'Dependencies')
            if (prepareTask == null) {
                return
            }
            prepareTask.doLast {
                def dustDir = project.file(project.buildDir.path + '/intermediates/bundles/' + variant.name + '/libs')
                ExplodedHelper.processIntoJars(project, artifacts, dustDir)
            }
        }
        if ('release'.equals(variant.buildType.name)) {
            def javacTask = project.tasks.findByPath('compile' + variant.name.capitalize() + 'JavaWithJavac')
            if (javacTask == null) {
                return
            }
            javacTask.doLast {
                def dustDir = project.file(project.buildDir.path + '/intermediates/classes/' + variant.name)
                ExplodedHelper.processIntoClasses(project, artifacts, dustDir)
            }
        }
    }

    /**
     * TODO
     * 1.into classes, "Note: duplicate definition of library class", when proguard
     * 2.minSdkVersion check(done by processDebugAndroidTestManifest)
     * 3.manifest merge
     * 4.res merge
     * 5.R.txt merge
     * 6.assets merge
     * 7.so merge
     * 8.proguard.txt merge
     * 9.lint.jar merge
     * 10.support packaging bundle, like guava
     * 11.configuration with extension
     * 12.aidl merge?
     * 13.other gradle version and android plugin version support
     * 14.support compile project(aar, jar)
     * 15.duplicate class check
     */
}
