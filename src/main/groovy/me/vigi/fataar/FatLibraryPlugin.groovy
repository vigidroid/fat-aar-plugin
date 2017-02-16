package me.vigi.fataar

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyResolutionListener
import org.gradle.api.artifacts.ResolvableDependencies
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * TODO
 * 1.into classes, "Note: duplicate definition of library class", when proguard
 * 2.minSdkVersion check(done by processDebugAndroidTestManifest??)
 * 3.manifest merge
 * 4.res merge
 * 5.R.txt merge
 * 7.so merge
 * 8.proguard.txt merge
 * 9.lint.jar merge
 * 11.configuration with extension
 * 12.aidl merge?
 * 13.other gradle version and android plugin version support
 * 15.duplicate class check
 * 17.design of configuration. refer to, into libs or classes, make embed more flexible to each variant like
 *    that android gradle plugin does, and combination of the prior
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
                    // use provided instead of compile to prune node dependency in
                    // pom file when upload aar library archives.
                    // but I have no idea whether have any side effect
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
        if (variant.buildType.isMinifyEnabled()) {
            Task javacTask = variant.getJavaCompile()
            if (javacTask == null) {
                return
            }
            javacTask.doLast {
                def dustDir = project.file(project.buildDir.path + '/intermediates/classes/' + variant.name)
                ExplodedHelper.processIntoClasses(project, artifacts, dustDir)
            }
        } else {
            Task prepareTask = project.tasks.findByPath('prepare' + variant.name.capitalize() + 'Dependencies')
            if (prepareTask == null) {
                return
            }
            prepareTask.doLast {
                def dustDir = project.file(project.buildDir.path + '/intermediates/bundles/' + variant.name + '/libs')
                ExplodedHelper.processIntoJars(project, artifacts, dustDir)
            }
        }
        // merge assets
        // AaptOptions.setIgnoreAssets and AaptOptions.setIgnoreAssetsPattern will work as normal
        Task assetsTask = variant.getMergeAssets()
        assetsTask.doFirst {
            for (artifact in artifacts) {
                if (!'aar'.equals(artifact.type)) {
                    continue
                }
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
                project.android.sourceSets.main.assets.srcDir(archiveLibrary.assetsFolder)
            }
        }
    }
}
