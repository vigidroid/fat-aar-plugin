package me.vigi.fataar;

import org.gradle.api.Project;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolvedArtifact;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Created by Vigi on 2017/2/16.
 *
 * @see com.android.builder.dependency.LibraryDependency
 */
public class AndroidArchiveLibrary {

    private final Project mProject;

    private final ResolvedArtifact mArtifact;

    public AndroidArchiveLibrary(Project project, ResolvedArtifact artifact) {
        if (!"aar".equals(artifact.getType())) {
            throw new IllegalArgumentException("artifact must be aar type!");
        }
        mProject = project;
        mArtifact = artifact;
    }

    public String getGroup() {
        return mArtifact.getModuleVersion().getId().getGroup();
    }

    public String getName() {
        return mArtifact.getModuleVersion().getId().getName();
    }

    public String getVersion() {
        return mArtifact.getModuleVersion().getId().getVersion();
    }

    public File getRootFolder() {
        File explodedRootDir = mProject.file(mProject.getBuildDir() + "/intermediates" + "/exploded-aar/");
        ModuleVersionIdentifier id = mArtifact.getModuleVersion().getId();
        return mProject.file(explodedRootDir + "/" + id.getGroup() + "/" + id.getName() + "/" + id.getVersion());
    }

    private File getJarsRootFolder() {
        return new File(getRootFolder(), "jars");
    }

    public File getAidlFolder() {
        return new File(getRootFolder(), "aidl");
    }

    public File getAssetsFolder() {
        return new File(getRootFolder(), "assets");
    }

    public File getClassesJarFile() {
        return new File(getJarsRootFolder(), "classes.jar");
    }

    public Collection<File> getLocalJars() {
        List<File> localJars = new ArrayList<>();
        File[] jarList = new File(getJarsRootFolder(), "libs").listFiles();
        if (jarList != null) {
            for (File jars : jarList) {
                if (jars.isFile() && jars.getName().endsWith(".jar")) {
                    localJars.add(jars);
                }
            }
        }

        return localJars;
    }

    public File getJniFolder() {
        return new File(getRootFolder(), "jni");
    }

    public File getResFolder() {
        return new File(getRootFolder(), "res");
    }

    public File getManifest() {
        return new File(getRootFolder(), "AndroidManifest.xml");
    }

    public File getLintJar() {
        return new File(getJarsRootFolder(), "lint.jar");
    }

    public File getProguardRules() {
        return new File(getRootFolder(), "proguard.txt");
    }

    public File getSymbolFile() {
        return new File(getRootFolder(), "R.txt");
    }
}
