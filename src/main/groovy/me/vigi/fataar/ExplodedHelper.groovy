package me.vigi.fataar

import org.gradle.api.Project
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.FileTree

/**
 * Created by Vigi on 2017/1/20.
 */
class ExplodedHelper {

    static FileTree resolveAllManifests(Project project) {
        def explodedRoot = project.file(project.buildDir.path + '/intermediates' + '/exploded-aar')
        def manifests = project.fileTree(explodedRoot) {
            include '**/AndroidManifest.xml'
            exclude '**/aapt/AndroidManifest.xml'
        }
        return manifests
    }

    static void processIntoJars(Project project, Collection<ResolvedArtifact> artifacts, File folderOut) {
        resolveAllManifests(project).each { file ->
            println 'vigi-->file=' + file
            def aarRoot = file.parentFile
            def prefix = aarRoot.parentFile.name + '-' + aarRoot.name

            project.copy {
                from("$aarRoot.path/jars/classes.jar")
                into folderOut
                rename { prefix + '.jar' }
            }
            project.copy {
                from("$aarRoot.path/jars") {
                    include '*.jar'
                    exclude 'classes.jar'
                }
                from("$aarRoot.path/jars/libs") {
                    include '*.jar'
                }
                from("$aarRoot.path/libs") {
                    include '*.jar'
                }
                into folderOut
                rename { prefix + '-' + it }
            }
        }
    }

    static void processIntoClasses(Project project, File folderOut) {
        resolveAllManifests(project).each { file ->
            println 'vigi-->file=' + file
            def aarRoot = file.parentFile
            def jarsTree = project.fileTree(aarRoot) {
                include 'jars/*.jar'
                include 'jars/libs/*.jar'
                include 'libs/*.jar'
            }
            jarsTree.each { jar ->
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
