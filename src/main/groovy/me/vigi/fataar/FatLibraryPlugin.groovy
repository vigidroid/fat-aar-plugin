package me.vigi.fataar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact

/**
 *
 * Created by Vigi on 2017/1/14.
 */
class FatLibraryPlugin implements Plugin<Project> {

    private Project project
    private Configuration embedConf

    private Set<ResolvedArtifact> artifacts

    @Override
    void apply(Project project) {
        this.project = project
        checkAndroidPlugin()
        createConfiguration()
        project.afterEvaluate {
            resolveArtifacts()
            project.android.libraryVariants.all { variant ->
                processVariant(variant)
            }
        }
    }

    private void checkAndroidPlugin() {
        if (!project.plugins.hasPlugin('com.android.library')) {
            throw new ProjectConfigurationException('fat-aar-plugin must be applied in project that' +
                    ' has android library plugin!', null)
        }
    }

    private void createConfiguration() {
        embedConf = project.configurations.create('embed')
        embedConf.visible = false
        project.gradle.addListener(new DependencyResolutionListener() {
            @Override
            void beforeResolve(ResolvableDependencies resolvableDependencies) {
                embedConf.dependencies.each { dependency ->
                    /**
                     * use provided instead of compile.
                     * advantage:
                     *   1. prune dependency node in generated pom file when upload aar library archives.
                     *   2. make invisible to the android application module, thus to avoid some duplicated processes.
                     * side effect:
                     *   1. incorrect R.txt in bundle. I fixed it by another way.
                     *   2. loss R.class in intermediates\classes\**\
                     *   3. any other...
                     */
                    project.dependencies.add('provided', dependency)
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
                println 'fat-aar-->[embed detected][' + artifact.type + ']' + artifact.moduleVersion.id
            } else {
                throw new ProjectConfigurationException('Only support embed aar and jar dependencies!', null)
            }
            set.add(artifact)
        }
        artifacts = Collections.unmodifiableSet(set)
    }

    private void processVariant(variant) {
        def processor = new VariantProcessor(project, variant)
        for (artifact in artifacts) {
            if ('aar'.equals(artifact.type)) {
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
                processor.addAndroidArchiveLibrary(archiveLibrary)
            }
            if ('jar'.equals(artifact.type)) {
                processor.addJarFile(artifact.file)
            }
        }
        processor.processVariant()
    }
}
