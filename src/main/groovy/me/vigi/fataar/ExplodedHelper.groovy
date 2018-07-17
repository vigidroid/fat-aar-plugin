package me.vigi.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact

/**
 * Created by Vigi on 2017/1/20.
 */
class ExplodedHelper {

    public static void processIntoJars(Project project,
                                       Collection<AndroidArchiveLibrary> androidLibraries, Collection<ResolvedArtifact> jarFiles,
                                       File folderOut) {
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                println 'fat-aar-->[warning]' + androidLibrary.rootFolder + ' not found!'
                continue
            }
//          println 'fat-aar-->copy aar from: ' + androidLibrary.rootFolder
            def prefix = androidLibrary.group + '-' + androidLibrary.name + '-' + androidLibrary.version
            project.copy {
                from(androidLibrary.classesJarFile)
                into folderOut
                rename { prefix + '.jar' }
            }
            project.copy {
                from(androidLibrary.localJars)
                into folderOut
                rename { prefix + '-' + it }
            }
        }
        for (ResolvedArtifact jarFile in jarFiles) {
            if (!jarFile.file.exists()) {
                println 'fat-aar-->[warning] ' + jarFile.file + ' not found!'
                continue
            }
            def id = jarFile.getModuleVersion().getId()
            def prefix = id.group + '-' + id.name + '-' + id.version
//          println 'fat-aar-->copy jar from: ' + jarFile
            project.copy {
                from(jarFile.file)
                into folderOut
                rename { prefix + '-' + it }
            }
        }
    }

    public static void processIntoClasses(Project project,
                                          Collection<AndroidArchiveLibrary> androidLibraries, Collection<ResolvedArtifact> jarFiles,
                                          File folderOut) {
        Collection<ResolvedArtifact> allJarFiles = new ArrayList<>()
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                println 'fat-aar-->[warning]' + androidLibrary.rootFolder + ' not found!'
                continue
            }
            allJarFiles.add(androidLibrary.classesJarFile)
            allJarFiles.addAll(androidLibrary.localJars)
        }
        for (ResolvedArtifact jarFile in jarFiles) {
            if (!jarFile.file.exists()) {
                println 'fat-aar-->[warning] ' + jarFile.file + ' not found!'
                continue
            }
            allJarFiles.add(jarFile)
        }
        for (ResolvedArtifact jarFile in allJarFiles) {
//          println 'fat-aar-->copy classes from: ' + jarFile.file
            project.copy {
                from project.zipTree(jarFile.file)
                into folderOut
                include '**/*.class'
                exclude 'META-INF/'
            }
        }
    }
}
