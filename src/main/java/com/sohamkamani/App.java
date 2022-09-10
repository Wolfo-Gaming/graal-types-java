package com.sohamkamani;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.json.JSONArray;
import org.json.JSONObject;
import org.reflections.ReflectionUtils;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.scanners.SubTypesScanner;
import org.reflections.scanners.TypeElementsScanner;
import org.reflections.util.ConfigurationBuilder;
import org.apache.commons.lang3.ClassUtils;
import java.util.List;

/**
 * Hello world!
 *
 */
public class App {
    public static ArrayList<String> getDeps(ArrayList<String> artifacts, String outPath) {
        ArrayList<String> jarfiles = new ArrayList<String>();
        ArrayList<String> repo = new ArrayList();
        repo.add("https://repo1.maven.org/maven2");
        repo.add("https://papermc.io/repo/repository/maven-public");
        Maven maven = new Maven(Path.of(outPath), repo);
        for (String artifact : artifacts) {
            try {
                Maven.ArtifactResults results = maven.downloadArtifacts(artifact, false);
                List<Path> deDupStringList3 = results.symbols.stream().distinct().collect(Collectors.toList());

                for (Path string : deDupStringList3) {
                    //System.out.println("Need to process " + string.toString());
                    jarfiles.add(string.toString());
                }
                // System.out.println(results.sourceJar.toString());
            } catch (InterruptedException | IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }

        return jarfiles;
    }

    public static List<String> getClasses(String jarFile) throws IOException {
        List<String> classNames = new ArrayList<String>();
        ZipInputStream zip = new ZipInputStream(new FileInputStream(jarFile));
        for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
            if (!entry.isDirectory() && (entry.getName().endsWith(".class") || entry.getName().endsWith(".java"))) {
                if (entry.getName().endsWith(".java")) {
                    String className = entry.getName().replace('/', '.'); // including ".class"
                    classNames.add(className.substring(0, className.length() - ".java".length()));
                } else {
                    String className = entry.getName().replace('/', '.'); // including ".class"
                    classNames.add(className.substring(0, className.length() - ".class".length()));
                }
                // This ZipEntry represents a class. Now, what class does it represent?

            }
        }
        return classNames;
    }

    public static void loadClasses(String path) {
        try {
            JSONObject obj = new JSONObject(Files.readString(Path.of("./pack.json")));
            JSONArray artifs = obj.getJSONArray("artifacts");
            String[] stringArray = new String[artifs.length()];
            for (int i = 0; i < artifs.length(); i++) {
                stringArray[i] = (String) artifs.getString(i);
            }
            List<String> jars = getDeps(new ArrayList<String>(Arrays.asList(stringArray)), obj.getString("out"));

            ArrayList<URL> arrayList = new ArrayList<URL>();
            File folder = new File(obj.getString("out"));
            File[] listOfFiles = folder.listFiles();
            JSONArray res = new JSONArray();
            ArrayList<String> classes = new ArrayList<String>();
            for (File file : listOfFiles) {
                if (file.isFile()) {
                    for (String file2 : getClasses(file.getPath())) {
                        //System.out.println("foundclass " + file2);
                        classes.add(file2);
                    }
                    
                    //System.out.println(file.getName());
                    arrayList.add(new URL("jar:file:" + obj.getString("out") + "/" + file.getName() + "!/"));
                }
            }

            URLClassLoader cl = URLClassLoader.newInstance((URL[]) arrayList.toArray(new URL[0]));
           
            

           
            String[] additional = { "java.base", "java.util", "java.io", "java.nio" };
            getClasses("./src.zip").forEach((action) -> {
                if (action.startsWith("java.base") && !action.contains("OSGI-OPT")) {
                    //System.out.println(action.replace("java.base.", ""));
                    classes.add(action.replace("java.base.", ""));
                }
            });
            for (String className : additional) {
                Reflections reflections = new Reflections(className, new SubTypesScanner(false));
                Reflections ref = new Reflections(new ConfigurationBuilder().setScanners(new SubTypesScanner(false), new ResourcesScanner(), new TypeElementsScanner()));
                
               
                Set<Class<? extends Object>> allClasses = 
    ref.getSubTypesOf(Object.class);
                   // System.out.println("AAAAAAA" + allClasses.size());
                allClasses.forEach((cal) -> {
                    classes.add(cal.getCanonicalName());
                   // System.out.println("EE" + cal.getCanonicalName());
                });
                
                //Class<?> c = cl.loadClass(className);
                //res.put(parseClass(c));
            }
            for (String className : classes) {
                if (className.contains("org.slf4j.impl.StaticLoggerBinder") || className.contains("sun.") || className.contains("package-info") ||className.contains("OSGI-OPT") || className.contains("org.junit.jupiter.api") || className.contains("META-INF") || className.contains("module-info") || className.equals("com.google.common.base.MoreObjects$ToStringHelper$UnconditionalValueHolder") || className.equals("com.google.common.collect.CollectSpliterators$FlatMapSpliteratorOfDouble") || className.equals("com.google.common.collect.CollectSpliterators$FlatMapSpliteratorOfInt") || className.contains("com.google.common.collect.CollectSpliterators") || className.contains("com.google.common") || className.contains("org.bukkit.plugin.java.LibraryLoader") || className.contains("org.junit.runner.manipulation.Alphanumeric")) {
                    //System.out.println("continue!");
                    continue;
                };
                //System.out.println(className);
                Class<?> c = cl.loadClass(className);
                res.put(parseClass(c));
            }
            System.out.println(res.toString());
        } catch (IOException | ClassNotFoundException e1) {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        return;
    }

    public static void main(String[] args) {
        loadClasses(args[0]);
    }

    public static JSONObject parseClass(Class<?> loadedClass) throws ClassNotFoundException {
        JSONArray methods = new JSONArray();
        JSONArray fields = new JSONArray();
        JSONArray constructors = new JSONArray();
        for (Constructor constructor : loadedClass.getConstructors()) {
            JSONObject constructorObject = new JSONObject();
            constructorObject.put("name", constructor.getName());
            JSONArray params = new JSONArray();
            for (Parameter param : constructor.getParameters()) {
                JSONObject paramObject = new JSONObject();
                paramObject.put("name", param.getName());
                paramObject.put("type", param.getType().getCanonicalName());
                params.put(paramObject);
            }
            constructorObject.put("params", params);
            constructors.put(constructorObject);
        }
        for (Method method : loadedClass.getMethods()) {
            JSONObject rootObject = new JSONObject();
            rootObject.put("name", method.getName());
            try {
                rootObject.put("returns", method.getReturnType().getCanonicalName());
            } catch (Exception e) {
                rootObject.put("returns", "");
            } catch (IncompatibleClassChangeError a) {
                //System.out.println(a);
            }

            JSONArray params = new JSONArray();
            for (Parameter param : method.getParameters()) {
                JSONObject paramObject = new JSONObject();
                paramObject.put("name", param.getName());
                paramObject.put("type", param.getType().getCanonicalName());
                params.put(paramObject);
            }
            rootObject.put("params", params);
            methods.put(rootObject);
        }
        for (Field field : loadedClass.getFields()) {
            JSONObject fieldObject = new JSONObject();
            fieldObject.put("name", field.getName());
            fieldObject.put("type", field.getType().toString());
            fields.put(fieldObject);
        }
        JSONObject result = new JSONObject();
        ClassUtils.getAllSuperclasses(loadedClass).forEach((cl) -> {
            // System.out.println("Extends: " + cl.getCanonicalName());
        });
        JSONArray ext = new JSONArray();
        JSONArray impl = new JSONArray();
        for (Class<?> implec : ClassUtils.getAllInterfaces(loadedClass)) {
            impl.put(implec.getCanonicalName());
        }
        for (Class<?> superc : ClassUtils.getAllSuperclasses(loadedClass)) {
            ext.put(superc.getCanonicalName());
        }
        result.put("methods", methods);
        result.put("fields", fields);
        result.put("name", loadedClass.getName());
        result.put("constructors", constructors);
        result.put("extends", ext);
        result.put("implements", impl);
        result.put("isInterface", loadedClass.isInterface());
        return result;
    }
}
