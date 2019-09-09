package com.lin.dhjar.plugin

import com.google.common.io.ByteStreams
import com.google.common.io.Files
import javassist.ClassPool
import javassist.CtClass
import javassist.CtMethod
import javassist.CtNewMethod
import javassist.Modifier
import org.apache.commons.io.FileUtils

import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream


class JavassistInject {
    private static Map<String, Class> map = new HashMap<>()

    private static Class getAnnotationClass(String className, ClassPool mClassPool) {
        if (!map.containsKey(className)) {
            CtClass mCtClass = mClassPool.getCtClass(className)
            if (mCtClass.isFrozen()) {
                mCtClass.defrost()
            }
            map.put(className, mCtClass.toClass())
            mCtClass.detach()
        }
        return map.get(className)
    }


    static void injectDir(String inputPath, String outPutPath, ClassPool mClassPool,LJarConfig lJarConfig) {
        File dir = new File(inputPath)
        if (dir.isDirectory()) {
            dir.eachFileRecurse { File file ->
                String filePath = file.absolutePath
                if (file.isFile()) {
                    File outPutFile = new File(outPutPath + filePath.substring(inputPath.length()))
                    Files.createParentDirs(outPutFile)
                    if (filePath.endsWith(".class")
                            && !filePath.contains('R$')
                            && !filePath.contains('R.class')
                            && !filePath.contains("BuildConfig.class")) {
                        FileInputStream inputStream = new FileInputStream(file)
                        FileOutputStream outputStream = new FileOutputStream(outPutFile)
                        System.out.println("output file==="+outPutFile.getAbsolutePath())
                        transform(inputStream, outputStream, mClassPool,lJarConfig)
                    } else {
                        FileUtils.copyFile(file, outPutFile)
                    }
                }
            }
        }
    }

    static void injectJar(File inputFile, File outFile, ClassPool mClassPool,LJarConfig lJarConfig) throws IOException {
        ArrayList entries = new ArrayList()
        Files.createParentDirs(outFile)
        FileInputStream fis = null
        ZipInputStream zis = null
        FileOutputStream fos = null
        ZipOutputStream zos = null
        try {
            fis = new FileInputStream(inputFile)
            zis = new ZipInputStream(fis)
            fos = new FileOutputStream(outFile)
            zos = new ZipOutputStream(fos)
            ZipEntry entry = zis.getNextEntry()
            while (entry != null) {
                String fileName = entry.getName()
                if (!entries.contains(fileName)) {
                    entries.add(fileName)
                    zos.putNextEntry(new ZipEntry(fileName))
                    if (!entry.isDirectory() && fileName.endsWith(".class")
                            && !fileName.contains('R$')
                            && !fileName.contains('R.class')
                            && !fileName.contains("BuildConfig.class"))
                        transform(zis, zos, mClassPool,lJarConfig)
                    else {
                        ByteStreams.copy(zis, zos)
                    }
                }
                entry = zis.getNextEntry()
            }
        } catch (Exception e) {
            e.printStackTrace()
        } finally {
            if (zos != null)
                zos.close()
            if (fos != null)
                fos.close()
            if (zis != null)
                zis.close()
            if (fis != null)
                fis.close()
        }
    }


    static void transform(InputStream input, OutputStream out, ClassPool mClassPool,LJarConfig lJarConfig) {
        try {
            CtClass c = mClassPool.makeClass(input)
            transformClass(c, mClassPool,lJarConfig)
            out.write(c.toBytecode())
            c.detach()
        } catch (Exception e) {
            e.printStackTrace()
            input.close()
            out.close()
            throw new RuntimeException(e.getMessage())
        }
    }

    private static void transformClass(CtClass c, ClassPool mClassPool,LJarConfig lJarConfig) {
        Set<String> keys = lJarConfig.cutList.keySet()
        for(String s: keys){
            if(c.getName().startsWith(s)){
                modify(c,mClassPool,lJarConfig.cutList.get(s))
                break;
            }
        }
    }


    private static void modify(CtClass c, ClassPool mClassPool,List<String> methods) {
        if (c.isFrozen()) {
            c.defrost()
        }
        System.out.println("find class==============="+c.getName())
        for(String method : methods){
            CtMethod ctMethod = c.getDeclaredMethod(method)
            String method2 = method+"DhCut"
            CtMethod ctMethod2 = CtNewMethod.copy(ctMethod,method2,c,null)
            c.addMethod(ctMethod2)
            int methodLen = ctMethod.getParameterTypes().length
            StringBuffer sb  = new StringBuffer()
            sb.append("{try{")
            sb.append(method2)
            sb.append("(")
            for(int i = 0; i<methodLen;i++){
                sb.append("\$"+(i+1))
                if(i!=methodLen-1){
                    sb.append(",")
                }
            }
            sb.append(");}catch(Exception ex){ System.out.println(ex.toString());ex.printStackTrace();}}")
            ctMethod.setBody(sb.toString())
        }
    }
    private static boolean checkMethod(int modifiers) {
        return !Modifier.isStatic(modifiers) && !Modifier.isNative(modifiers) && !Modifier.isAbstract(modifiers) && !Modifier.isEnum(modifiers) && !Modifier.isInterface(modifiers)
    }


}
