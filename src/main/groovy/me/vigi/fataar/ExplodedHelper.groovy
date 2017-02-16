package me.vigi.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree

/**
 * Created by Vigi on 2017/1/20.
 */
class ExplodedHelper {

    /**
     * iterate over all AndroidManifest.xml to resolve aar dependencies
     */
    static FileTree resolveAllManifests(Project project) {
        def explodedRoot = project.file(project.buildDir.path + '/intermediates' + '/exploded-aar')
        def manifests = project.fileTree(explodedRoot) {
            include '**/AndroidManifest.xml'
            exclude '**/aapt/AndroidManifest.xml'
        }
        return manifests
    }

    static void processIntoJars(Project project, Collection<ResolvedArtifact> artifacts, File folderOut) {
        for (artifact in artifacts) {
            if ('aar'.equals(artifact.type)) {
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
                if (!archiveLibrary.rootFolder.exists()) {
                    println 'fat-aar-->[warning]' + archiveLibrary.rootFolder + ' not found!'
                    continue
                }
                println 'fat-aar-->copy aar from: ' + archiveLibrary.rootFolder
                def prefix = archiveLibrary.name + '-' + archiveLibrary.version
                project.copy {
                    from(archiveLibrary.classesJarFile)
                    into folderOut
                    rename { prefix + '.jar' }
                }
                project.copy {
                    from(archiveLibrary.localJars)
                    into folderOut
                    rename { prefix + '-' + it }
                }
            }
            if ('jar'.equals(artifact.type)) {
                if (!artifact.file.exists()) {
                    println 'fat-aar-->[warning]' + artifact.file.path + ' not found!'
                    continue
                }
                println 'fat-aar-->copy jar from: ' + artifact.file
                project.copy {
                    from(artifact.file)
                    into folderOut
                }
            }
        }
    }

    static void processIntoClasses(Project project, Collection<ResolvedArtifact> artifacts, File folderOut) {
        for (artifact in artifacts) {
            FileCollection jars = null
            if ('aar'.equals(artifact.type)) {
                AndroidArchiveLibrary archiveLibrary = new AndroidArchiveLibrary(project, artifact)
                if (!archiveLibrary.rootFolder.exists()) {
                    println 'fat-aar-->[warning]' + archiveLibrary.rootFolder + ' not found!'
                    continue
                }
                jars = project.files(archiveLibrary.classesJarFile, archiveLibrary.localJars)
            }
            if ('jar'.equals(artifact.type)) {
                if (!artifact.file.exists()) {
                    println 'fat-aar-->[warning]' + artifact.file.path + ' not found!'
                    continue
                }
                jars = project.files(artifact.file)
            }
            for (jar in jars) {
                println 'fat-aar-->copy classes from: ' + jar
                project.copy {
                    from project.zipTree(jar)
                    into folderOut
                    include '**/*.class'
                    exclude 'META-INF/'
                }
            }
        }
    }
}
