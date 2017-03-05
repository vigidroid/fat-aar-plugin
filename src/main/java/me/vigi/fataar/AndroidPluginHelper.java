package me.vigi.fataar;

import com.google.common.base.Strings;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.util.VersionNumber;
import org.joor.Reflect;

import java.io.File;

/**
 * Created by Vigi on 2017/3/5.
 */
public class AndroidPluginHelper {

    /**
     * Resolve from com.android.builder.Version#ANDROID_GRADLE_PLUGIN_VERSION
     *
     * Throw exception if can not found
     */
    public static String getAndroidPluginVersion() {
        return Reflect.on("com.android.builder.Version").get("ANDROID_GRADLE_PLUGIN_VERSION");
    }

    /**
     * return bundle dir of specific variant
     */
    public static File resolveBundleDir(Project project, Object variant) {
        if (VersionNumber.parse(getAndroidPluginVersion()).compareTo(VersionNumber.parse("2.3.0")) < 0) {
            String dirName = Reflect.on(variant).call("getDirName").get();
            if (Strings.isNullOrEmpty(dirName)) {
                return null;
            }
            return project.file(project.getBuildDir() + "/intermediates/bundles/" + dirName);
        } else {
            // do the trick getting assets task output
            Task mergeAssetsTask = Reflect.on(variant).call("getMergeAssets").get();
            File assetsDir = Reflect.on(mergeAssetsTask).call("getOutputDir").get();
            return assetsDir.getParentFile();
        }
    }
}
