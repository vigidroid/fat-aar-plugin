package me.vigi.fataar;

import com.android.build.api.transform.*;
import com.android.build.gradle.internal.pipeline.TransformManager;
import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.Collection;
import java.util.Set;

/**
 * Created by Vigi on 2017/3/17.
 */
public class TestTransform extends Transform {

    @Override
    public String getName() {
        return "Test";
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_JARS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        return ImmutableSet.of();
    }

    @Override
    public Set<? super QualifiedContent.Scope> getReferencedScopes() {
        return ImmutableSet.of(
//                QualifiedContent.Scope.PROJECT,               // current project classes dir
//                QualifiedContent.Scope.PROJECT_LOCAL_DEPS,    // local jars
                QualifiedContent.Scope.SUB_PROJECTS,         // sub-project dependency (jar of java project, classes.jar of aar project)
                QualifiedContent.Scope.SUB_PROJECTS_LOCAL_DEPS,   // local jars of sub-project dependency
//                QualifiedContent.Scope.TESTED_CODE,
                QualifiedContent.Scope.PROVIDED_ONLY,       // "provided" dependency
                QualifiedContent.Scope.EXTERNAL_LIBRARIES   // maven repositories dependency(jar of java repo, classes.jar of aar repo)
        );
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws TransformException, InterruptedException, IOException {
        System.out.println("-----start transform log-----");
        Context context = transformInvocation.getContext();
        System.out.println("context.getTemporaryDir=" + context.getTemporaryDir());
        System.out.println("context.getPath=" + context.getPath());
        System.out.println();
        Collection<TransformInput> inputs = transformInvocation.getInputs();
        for (TransformInput input : inputs) {
            System.out.println("getInputs: " + input);
//            for (JarInput jarInput : input.getJarInputs()) {
//                System.out.println("    jarInput: " + jarInput);
//            }
//            for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
//                System.out.println("    directoryInput: " + directoryInput);
//            }
        }
        System.out.println();
        Collection<TransformInput> referencedInputs = transformInvocation.getReferencedInputs();
        for (TransformInput referencedInput : referencedInputs) {
            System.out.println("getReferencedInputs: " + referencedInput);
//            for (JarInput jarInput : referencedInput.getJarInputs()) {
//                System.out.println("    jarInput: " + jarInput);
//            }
//            for (DirectoryInput directoryInput : referencedInput.getDirectoryInputs()) {
//                System.out.println("    directoryInput: " + directoryInput);
//            }
        }
        System.out.println();
        Collection<SecondaryInput> secondaryInputs = transformInvocation.getSecondaryInputs();
        for (SecondaryInput secondaryInput : secondaryInputs) {
            System.out.println("getSecondaryInputs: " + secondaryInput.getSecondaryInput().getFile() + ", " + secondaryInput.getStatus().name());
        }
        System.out.println("-----end transform log-----");
    }
}
