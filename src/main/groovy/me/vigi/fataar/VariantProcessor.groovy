package me.vigi.fataar

import org.gradle.api.Project
import org.gradle.api.Task

/**
 * Created by Vigi on 2017/2/24.
 */
class VariantProcessor {

    private final Project mProject

    private final mVariant

    private Collection<AndroidArchiveLibrary> mAndroidArchiveLibraries = new ArrayList<>()

    private Collection<File> mJarFiles = new ArrayList<>()

    public VariantProcessor(Project project, variant) {
        mProject = project
        mVariant = variant
    }

    public void addAndroidArchiveLibrary(AndroidArchiveLibrary library) {
        mAndroidArchiveLibraries.add(library)
    }

    public void addJarFile(File jar) {
        mJarFiles.add(jar)
    }

    public void processVariant() {
        processManifest()
        processClassesAndJars()
        processResourcesAndR()
        processAssets()
        processJniLibs()
    }

    /**
     * merge manifest
     *
     * TODO process each variant.getOutputs()
     * TODO "InvokeManifestMerger" deserve more android plugin version check
     * TODO add setMergeReportFile
     * TODO a better temp manifest file location
     */
    private void processManifest() {
        Class invokeManifestTaskClazz = null
        try {
            invokeManifestTaskClazz = Class.forName('com.android.build.gradle.tasks.InvokeManifestMerger')
        } catch (ClassNotFoundException e) {
            println 'fat-aar-->' + e.getMessage()
        }
        if (invokeManifestTaskClazz == null) {
            return
        }
        Task processManifestTask = mVariant.getOutputs().get(0).getProcessManifest()
        def manifestOutput = mProject.file(mProject.buildDir.path + '/intermediates/fat-aar/' + mVariant.dirName + '/AndroidManifest.xml')
        File manifestOutputBackup = processManifestTask.getManifestOutputFile()
        processManifestTask.setManifestOutputFile(manifestOutput)

        Task manifestsMergeTask = mProject.tasks.create('merge' + mVariant.name.capitalize() + 'Manifest', invokeManifestTaskClazz)
        manifestsMergeTask.setVariantName(mVariant.name)
        manifestsMergeTask.setMainManifestFile(manifestOutput)
        List<File> list = new ArrayList<>()
        for (archiveLibrary in mAndroidArchiveLibraries) {
            list.add(archiveLibrary.getManifest())
        }
        manifestsMergeTask.setSecondaryManifestFiles(list)
        manifestsMergeTask.setOutputFile(manifestOutputBackup)
        manifestsMergeTask.dependsOn processManifestTask
        processManifestTask.finalizedBy manifestsMergeTask
    }

    private void processClassesAndJars() {
        if (mVariant.buildType.isMinifyEnabled()) {
            Task javacTask = mVariant.getJavaCompile()
            if (javacTask == null) {
                return
            }
            javacTask.doLast {
                def dustDir = mProject.file(mProject.buildDir.path + '/intermediates/classes/' + mVariant.dirName)
                ExplodedHelper.processIntoClasses(mProject, mAndroidArchiveLibraries, mJarFiles, dustDir)
            }
        } else {
            Task prepareTask = mProject.tasks.findByPath('prepare' + mVariant.name.capitalize() + 'Dependencies')
            if (prepareTask == null) {
                return
            }
            prepareTask.doLast {
                def dustDir = mProject.file(mProject.buildDir.path + '/intermediates/bundles/' + mVariant.dirName + '/libs')
                ExplodedHelper.processIntoJars(mProject, mAndroidArchiveLibraries, mJarFiles, dustDir)
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
    private void processResourcesAndR() {
        Task resourceGenTask = mProject.tasks.findByPath('generate' + mVariant.name.capitalize() + 'Resources')
        if (resourceGenTask == null) {
            return
        }
        resourceGenTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                mProject.android.sourceSets."main".res.srcDir(archiveLibrary.resFolder)
            }
        }
    }

    /**
     * merge assets
     *
     * AaptOptions.setIgnoreAssets and AaptOptions.setIgnoreAssetsPattern will work as normal
     */
    private void processAssets() {
        Task assetsTask = mVariant.getMergeAssets()
        if (assetsTask == null) {
            return
        }
        assetsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                // the source set here should be main or variant?
                mProject.android.sourceSets."main".assets.srcDir(archiveLibrary.assetsFolder)
            }
        }
    }

    /**
     * merge jniLibs
     */
    private void processJniLibs() {
        Task mergeJniLibsTask = mProject.tasks.findByPath('merge' + mVariant.name.capitalize() + 'JniLibFolders')
        if (mergeJniLibsTask == null) {
            return
        }
        mergeJniLibsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                // the source set here should be main or variant?
                mProject.android.sourceSets."main".jniLibs.srcDir(archiveLibrary.jniFolder)
            }
        }
    }
}
