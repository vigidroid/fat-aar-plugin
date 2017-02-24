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
        Task prepareTask = project.tasks.findByPath('prepare' + variant.name.capitalize() + 'Dependencies')
        if (variant.buildType.isMinifyEnabled()) {
            Task javacTask = variant.getJavaCompile()
            if (javacTask) {
                javacTask.doLast {
                    def dustDir = project.file(project.buildDir.path + '/intermediates/classes/' + variant.dirName)
                    ExplodedHelper.processIntoClasses(project, artifacts, dustDir)
                }
            }
        } else {
            if (prepareTask) {
                prepareTask.doLast {
                    def dustDir = project.file(project.buildDir.path + '/intermediates/bundles/' + variant.dirName + '/libs')
                    ExplodedHelper.processIntoJars(project, artifacts, dustDir)
                }
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
                // the source set here should be main or variant?
                project.android.sourceSets."main".assets.srcDir(archiveLibrary.assetsFolder)
            }
        }
        // merge jniLibs
        Task mergeJniLibsTask = project.tasks.findByPath('merge' + variant.name.capitalize() + 'JniLibFolders')
        if (mergeJniLibsTask) {
            mergeJniLibsTask.doFirst {
                for (artifact in artifacts) {
                    if (!'aar'.equals(artifact.type)) {
                        continue
                    }
                    AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
                    // the source set here should be main or variant?
                    project.android.sourceSets."main".jniLibs.srcDir(archiveLibrary.jniFolder)
                }
            }
        }
        /**
         * merge R.txt(actually is to fix issue caused by provided configuration) and res
         *
         * Here I have to inject res into "main" instead of "variant.name".
         * To avoid the res from embed dependencies being used, once they have the same res Id with main res.
         *
         * Now the same res Id will cause a build exception: Duplicate resources, to encourage you to change res Id.
         * Adding "android.disableResourceValidation=true" to "gradle.properties" can do a trick to skip the exception, but is not recommended.
         */
        Task resourceGenTask = project.tasks.findByPath('generate' + variant.name.capitalize() + 'Resources')
        if (resourceGenTask) {
            resourceGenTask.doFirst {
                for (artifact in artifacts) {
                    if (!'aar'.equals(artifact.type)) {
                        continue
                    }
                    AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
                    project.android.sourceSets."main".res.srcDir(archiveLibrary.resFolder)
                }
            }
        }
        /**
         * merge R.txt and res.
         *
         * Deprecated.
         * This will cause a warning "Source folders generated at incorrect location" when Gradle Sync
         */
//        if (prepareTask) {
//            for (artifact in artifacts) {
//                if (!'aar'.equals(artifact.type)) {
//                    continue
//                }
//                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
//                variant.registerResGeneratingTask(prepareTask, archiveLibrary.resFolder)
//            }
//        }
        /**
         * merge manifest
         *
         * TODO process each variant.getOutputs()
         * TODO "InvokeManifestMerger" deserve more android plugin version check
         * TODO add setMergeReportFile
         * TODO a better temp manifest file location
         */
        Class invokeManifestTaskClazz
        try {
            invokeManifestTaskClazz = Class.forName('com.android.build.gradle.tasks.InvokeManifestMerger')
        } catch (ClassNotFoundException e) {
            println 'fat-aar-->' + e.getMessage()
            return
        }
        Task processManifestTask = variant.getOutputs().get(0).getProcessManifest()
        def manifestOutput = project.file(project.buildDir.path + '/intermediates/fat-aar/' + variant.dirName + '/AndroidManifest.xml')
        File manifestOutputBackup = processManifestTask.getManifestOutputFile()
        processManifestTask.setManifestOutputFile(manifestOutput)
        Task manifestsMergeTask = project.tasks.create('merge' + variant.name.capitalize() + 'Manifest', invokeManifestTaskClazz)
        manifestsMergeTask.setVariantName(variant.name)
        manifestsMergeTask.setMainManifestFile(manifestOutput)
        List<File> list = new ArrayList<>()
        for (artifact in artifacts) {
            if (!'aar'.equals(artifact.type)) {
                continue
            }
            AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
            list.add(archiveLibrary.getManifest())
        }
        manifestsMergeTask.setSecondaryManifestFiles(list)
        manifestsMergeTask.setOutputFile(manifestOutputBackup)
        manifestsMergeTask.dependsOn processManifestTask
        Task processResourcesTask = variant.getOutputs().get(0).getProcessResources()
        processResourcesTask.dependsOn manifestsMergeTask
    }
}
