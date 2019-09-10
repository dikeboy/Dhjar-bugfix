package com.lin.dhjar.plugin

import com.android.build.api.transform.*
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.utils.FileUtils
import javassist.ClassPool
import javassist.JarClassPath
import org.apache.commons.codec.digest.DigestUtils
import org.gradle.api.Project


public class JavassistTransform extends Transform {
    private Project project;
    private LJarConfig lJarConfig
    public JavassistTransform(Project project) {
        this.project = project;
        this.lJarConfig = project.dhjar
        if(lJarConfig==null){
            lJarConfig = new LJarConfig()
        }
    }

    @Override
    public String getName() {
        return "dhJarPlugin"
    }

    @Override
    public Set<QualifiedContent.ContentType> getInputTypes() {
        return TransformManager.CONTENT_CLASS;
    }

    @Override
    public Set<? super QualifiedContent.Scope> getScopes() {
        Set<QualifiedContent.Scope> sets = new HashSet<QualifiedContent.Scope>()
        sets.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        return sets;
    }

    @Override
    Set<? super QualifiedContent.Scope> getReferencedScopes() {
        Set<QualifiedContent.Scope> sets = new HashSet<QualifiedContent.Scope>()
        sets.add(QualifiedContent.Scope.EXTERNAL_LIBRARIES)
        sets.add(QualifiedContent.Scope.PROVIDED_ONLY)
        return sets
    }

    @Override
    public boolean isIncremental() {
        return false;
    }

    @Override
    public void transform(TransformInvocation transformInvocation) throws IOException {
        project.logger.error("=================DhJarPluginTransform start====================");

        try {
            Collection<TransformInput> inputs = transformInvocation.getInputs();
            TransformOutputProvider outputProvider = transformInvocation.getOutputProvider();
            outputProvider.deleteAll();

            ClassPool mClassPool = new ClassPool(ClassPool.getDefault());
            // 添加android.jar目录
            mClassPool.appendClassPath(project.android.bootClasspath[0].toString());
            mClassPool.importPackage("android.os.Bundle");
            Set<DirectoryInput> classSet = new HashSet<DirectoryInput>()
            Set<JarInput> jarSet =new  HashSet<JarInput>()
            for (TransformInput input : inputs) {
                for (DirectoryInput directoryInput : input.getDirectoryInputs()) {
                    classSet.add(directoryInput)
                    mClassPool.appendClassPath(directoryInput.getFile().getAbsolutePath());
                }
                for (JarInput jarInput : input.getJarInputs()) {
                    jarSet.add(jarInput)
                    mClassPool.appendClassPath(new JarClassPath(jarInput.getFile().getAbsolutePath()));
                }
            }
            System.out.println("class size==="+classSet.size());
            for (DirectoryInput directoryInput : classSet){
                File dest = outputProvider.getContentLocation(directoryInput.getName(),
                        directoryInput.getContentTypes(), directoryInput.getScopes(),
                        Format.DIRECTORY);
                FileUtils.copyDirectory(directoryInput.getFile(),dest)
            }
            for(JarInput jarInput : jarSet){
                String fileName = jarInput.getFile().getAbsolutePath()
                System.out.println("filename==="+fileName);
                boolean isCut = false
                 for(String jarFile: lJarConfig.jarPath){
                     if(fileName.contains(jarFile)&&fileName.endsWith("jar")) {
                         isCut = true
                         break

                     }
                 }
                String jarName = jarInput.getName();
                String md5Name = DigestUtils.md5Hex(jarInput.getFile().getAbsolutePath());
                if (jarName.endsWith(".jar")) {
                    jarName = jarName.substring(0, jarName.length() - 4);
                }
                File dest = outputProvider.getContentLocation(jarName + md5Name,
                        jarInput.getContentTypes(), jarInput.getScopes(), Format.JAR);
                if(isCut){
                    project.logger.error("============replace jar==="+fileName);
                    project.logger.error("===========replace dest==="+dest);
                    JavassistInject.injectJar(jarInput.getFile(),dest, mClassPool,lJarConfig);
                }else{
                    FileUtils.copyFile(jarInput.getFile(),dest)
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        project.logger.error("=================DhJarPluginTransform finish====================");
    }

}