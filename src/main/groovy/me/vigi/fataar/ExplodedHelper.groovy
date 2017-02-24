package me.vigi.fataar

import org.gradle.api.Project

/**
 * Created by Vigi on 2017/1/20.
 */
class ExplodedHelper {

    public static void processIntoJars(Project project,
                                       Collection<AndroidArchiveLibrary> androidLibraries, Collection<File> jarFiles,
                                       File folderOut) {
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                println 'fat-aar-->[warning]' + androidLibrary.rootFolder + ' not found!'
                continue
            }
//          println 'fat-aar-->copy aar from: ' + androidLibrary.rootFolder
            def prefix = androidLibrary.name + '-' + androidLibrary.version
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
        for (jarFile in jarFiles) {
            if (!jarFile.exists()) {
                println 'fat-aar-->[warning]' + jarFile + ' not found!'
                continue
            }
//          println 'fat-aar-->copy jar from: ' + jarFile
            project.copy {
                from(jarFile)
                into folderOut
            }
        }
    }

    public static void processIntoClasses(Project project,
                                          Collection<AndroidArchiveLibrary> androidLibraries, Collection<File> jarFiles,
                                          File folderOut) {
        Collection<File> allJarFiles = new ArrayList<>()
        for (androidLibrary in androidLibraries) {
            if (!androidLibrary.rootFolder.exists()) {
                println 'fat-aar-->[warning]' + androidLibrary.rootFolder + ' not found!'
                continue
            }
            allJarFiles.add(androidLibrary.classesJarFile)
            allJarFiles.addAll(androidLibrary.localJars)
        }
        for (jarFile in jarFiles) {
            if (!jarFile.exists()) {
                println 'fat-aar-->[warning]' + jarFile + ' not found!'
                continue
            }
            allJarFiles.add(jarFile)
        }
        for (jarFile in allJarFiles) {
//          println 'fat-aar-->copy classes from: ' + jarFile
            project.copy {
                from project.zipTree(jarFile)
                into folderOut
                include '**/*.class'
                exclude 'META-INF/'
            }
        }
    }
}
