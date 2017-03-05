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
        String taskPath = 'prepare' + mVariant.name.capitalize() + 'Dependencies'
        Task prepareTask = mProject.tasks.findByPath(taskPath)
        if (prepareTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }

        processClassesAndJars(prepareTask)

        if (mAndroidArchiveLibraries.isEmpty()) {
            return
        }
        processManifest()
        processResourcesAndR()
        processRSources()
        processAssets()
        processJniLibs()
        processProguardTxt(prepareTask)
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
        String className = 'com.android.build.gradle.tasks.InvokeManifestMerger'
        try {
            invokeManifestTaskClazz = Class.forName(className)
        } catch (ClassNotFoundException ignored) {}
        if (invokeManifestTaskClazz == null) {
            throw new RuntimeException("Can not find class ${className}!")
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

    private void processClassesAndJars(Task prepareTask) {
        if (mVariant.getBuildType().isMinifyEnabled()) {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                File thirdProguard = archiveLibrary.proguardRules
                if (!thirdProguard.exists()) {
                    continue
                }
                mProject.android.getDefaultConfig().proguardFile(thirdProguard)
            }
            Task javacTask = mVariant.getJavaCompile()
            if (javacTask == null) {
                // warn: can not find javaCompile task, jack compile might be on.
                return
            }
            javacTask.doLast {
                def dustDir = mProject.file(mProject.buildDir.path + '/intermediates/classes/' + mVariant.dirName)
                ExplodedHelper.processIntoClasses(mProject, mAndroidArchiveLibraries, mJarFiles, dustDir)
            }
        } else {
            prepareTask.doLast {
                def dustDir = mProject.file(AndroidPluginHelper.resolveBundleDir(mProject, mVariant).path + '/libs')
//                FileUtils.cleanDirectory(dustDir)
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
        String taskPath = 'generate' + mVariant.name.capitalize() + 'Resources'
        Task resourceGenTask = mProject.tasks.findByPath(taskPath)
        if (resourceGenTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }
        resourceGenTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                mProject.android.sourceSets."main".res.srcDir(archiveLibrary.resFolder)
            }
        }
    }

    /**
     * generate R.java
     */
    private void processRSources() {
        Task processResourcesTask = mVariant.getOutputs().get(0).getProcessResources()
        processResourcesTask.doLast {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                RSourceGenerator.generate(processResourcesTask.getSourceOutputDir(), archiveLibrary)
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
            throw new RuntimeException("Can not find task in variant.getMergeAssets()!")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            assetsTask.getInputs().dir(archiveLibrary.assetsFolder)
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
        String taskPath = 'merge' + mVariant.name.capitalize() + 'JniLibFolders'
        Task mergeJniLibsTask = mProject.tasks.findByPath(taskPath)
        if (mergeJniLibsTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            mergeJniLibsTask.getInputs().dir(archiveLibrary.jniFolder)
        }
        mergeJniLibsTask.doFirst {
            for (archiveLibrary in mAndroidArchiveLibraries) {
                // the source set here should be main or variant?
                mProject.android.sourceSets."main".jniLibs.srcDir(archiveLibrary.jniFolder)
            }
        }
    }

    /**
     * merge proguard.txt
     */
    private void processProguardTxt(Task prepareTask) {
        String taskPath = 'merge' + mVariant.name.capitalize() + 'ProguardFiles'
        Task mergeFileTask = mProject.tasks.findByPath(taskPath)
        if (mergeFileTask == null) {
            throw new RuntimeException("Can not find task ${taskPath}!")
        }
        for (archiveLibrary in mAndroidArchiveLibraries) {
            File thirdProguard = archiveLibrary.proguardRules
            if (!thirdProguard.exists()) {
                continue
            }
            mergeFileTask.getInputs().file(thirdProguard)
        }
        mergeFileTask.doFirst {
            Collection proguardFiles = mergeFileTask.getInputFiles()
            for (archiveLibrary in mAndroidArchiveLibraries) {
                File thirdProguard = archiveLibrary.proguardRules
                if (!thirdProguard.exists()) {
                    continue
                }
                proguardFiles.add(thirdProguard)
            }
        }
        mergeFileTask.dependsOn prepareTask
    }
}
