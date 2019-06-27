package com.arcticpenguin;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MethodAnalyzer {

    public static final String COMMA=",";
    public static String csvFileName = "H:\\profile.csv";
    public static String folderName=""; // Enter Java source code's directory here

    public static void main(String[] args) throws IOException {

        if(null != args && args.length > 0 && null!= args[0]) {
            folderName=args[0];
        }

        File directory = new File(folderName);
        if(directory.isDirectory() == false) {
            System.out.println("Not a directory. Quitting");
            System.exit(0);
        }

        writeHeadersToCsv(csvFileName);
        recursivelyLoadFiles(directory);

        if(null !=args && args.length > 0 && null!=args[1]) {
            csvFileName = args[1];
        }
    }

    static void recursivelyLoadFiles(File directory) throws FileNotFoundException, IOException {

        for(File file: directory.listFiles()) {
            if(file.isFile() && file.getAbsolutePath().endsWith(".java")) {
                Map<String, MethodMetrics> metricsMap = retrieveMetricsForClass(file.getAbsolutePath());
                writeResultsToCsv(csvFileName, file.getAbsolutePath(), metricsMap);
            }
            if(file.isDirectory()) {
                recursivelyLoadFiles(file);
            }
        }
    }

    private static void writeHeadersToCsv(String csvFileName) throws IOException  {

        FileWriter fileWriter = new FileWriter(new File(csvFileName), true);
        fileWriter.write("Class Name" + COMMA + "Method Name " + COMMA + "Method Type" + COMMA + "Is static" + COMMA + "Length" + COMMA + "Return Type");
        fileWriter.write("\n");
        fileWriter.flush();
        fileWriter.close();
    }

    private static void writeResultsToCsv(String csvFileName, String className, Map<String, MethodMetrics> metricsMap) throws IOException {

        FileWriter fileWriter = new FileWriter(new File(csvFileName), true);

        for(String methodName: metricsMap.keySet()) {
            MethodMetrics methodMetrics = metricsMap.get(methodName);
            fileWriter.write(className + COMMA + methodMetrics.getMethodName() + COMMA
                    + methodMetrics.getMethodAccessModifierType() + COMMA + methodMetrics.isStaticMethod() +
                    COMMA + methodMetrics.getMethodLength() + COMMA + methodMetrics.getMethodReturnObject());
            fileWriter.write("\n");
        }
        fileWriter.flush();
        fileWriter.close();
    }

    static Map<String, MethodMetrics> retrieveMetricsForClass(final String className) throws FileNotFoundException, IOException {

        //debugging catcher
        if(className.contains("ListRecentAccountActivityInvoker.java") &&
                className.contains("controller")) {
            String x  = "1;";
            x="";
        }

        FileReader fileReader = new FileReader(new File(className));
        BufferedReader bufferedReader = new BufferedReader(fileReader);

        //State variables
        int totalLinesCounter=0;
        int classLInesCounter=0;
        int methodLinesCounter=0;
        int renameCounter=0;

        int stackBalancerForMethodEndFinding = 0;

        boolean inStaticBlockOrInterfaceOrEnum = false;
        boolean inAMethod = false;
        boolean inTheClass = false;

        //Metrics fields
        Map<String, MethodMetrics> methodsMap = new HashMap<String, MethodMetrics>();

        String currentMethodName = "";
        List<String> cachedLines = new ArrayList<String>();

        String currentLine;
        while((currentLine = bufferedReader.readLine()) != null) {

            MethodMetrics currentMethodMetrics = new MethodMetrics();

            // incrementing counters for totalLines, classLines, methodLines
            totalLinesCounter++;

            // Detecting starting point of a class
            if (inTheClass == false && currentLine.contains("{")) {
                inTheClass = true;
                continue;
            }

            if(inTheClass) {
                classLInesCounter++;
            }

            if(inAMethod) {
                methodLinesCounter++;
            }

            if(inTheClass && inAMethod) {
                cachedLines.add(currentLine);
            }
            else {
                cachedLines.clear();
            }

            // Looking for the starting point of a methodLines
            if(!inAMethod && !inStaticBlockOrInterfaceOrEnum && currentLine.contains("{")) {

                // detecting if this is an interface or an enum or a private class
                if(currentLine.contains(" interface ") ||
                        currentLine.contains(" enum ") ||
                        currentLine.contains(" class ")) {
                    System.out.println("Static blok spotted");
                    inStaticBlockOrInterfaceOrEnum = true;
                    stackBalancerForMethodEndFinding++;
                    continue;
                }

                // Detecting if this is a static block
                if(currentLine.replaceAll(" ", "").contains("static{")) {
                    System.out.println("Static block spotted");
                    inStaticBlockOrInterfaceOrEnum = true;
                    stackBalancerForMethodEndFinding++;
                    continue;
                }

                //TODO RISKY CODE
                // Detecting this some string contains "{ or {" instead of a methodLines
                // zyxw is a random word that is unlikely to be present in code . this random word approach is the way to overcome strings that have multipe braces {0} {1} {2} etc. in a single line
                String backupOfCurrentLineForLogging = currentLine;
                if(currentLine.replaceAll("\".?\\{", "zyxw").replaceAll("\\{.*?\"", "zyxw").contains("zyxw")) {
                    // System.out.println("Skipped this line due to annotation = " + backupOfCurrentLineForLogging);
                    // continue;
                }

                // Converting cachedLines to StringBuffer
                StringBuffer cachedLinesStringBuffer = new StringBuffer();
                for (String cachedLine: cachedLines) {
                    cachedLinesStringBuffer.append(cachedLine);
                }

                // Confirmed to be a method. Incrementing method lines
                inAMethod = true;
                methodLinesCounter++;

                String methodSignatureText = null;
                // Looking for start of parameters
                if(currentLine.contains("(")) {
            System.out.println("contains (");
            methodSignatureText = currentLine.split("\\(")[0];
        }
			else {
            System.out.println("doesn't contain (. Loading in the cache. contains (");
            if(cachedLinesStringBuffer.toString().contains("(")) {
                // Removing comments
                methodSignatureText = cachedLinesStringBuffer.toString().replaceAll(".*?/\\*\\*.*?\\*/", "")
                        .replaceAll("\\{@link.*?\\}", "")
                        .replaceAll(".*?;", "")
                        .replaceAll("@.*?(.*?) 	", "")
                        .replaceAll("@.*?[ 	]", "").trim()
                        .split("\\(")[0];
                // \\{@link.*?\\} remove @Link in javadov. Its braces confuse parser
                // ,*?; remove normal statements like i=1; it isn't needed for parsing
                // @.*?(.*?) remove annotation lines like @unit(expected)
                // @.*? remove words like @Override
            }
        }

        String[] wordsInMethodSignature = null;
        try {
            if(null == methodSignatureText) {
                continue;
            }
            wordsInMethodSignature = methodSignatureText.trim().split(" ");
        }catch(Exception e) {
            e.printStackTrace();
            System.out.println("Current line : " + currentLine);
            System.out.println("Cached lines: " + cachedLinesStringBuffer);
        }

        if(wordsInMethodSignature.length == 1) {
            // Can be a default constructor only

            currentMethodMetrics.setMethodReturnObject("void");
            currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.DEFAULT);

            while(methodsMap.get(currentMethodName) != null) {
                currentMethodName = currentMethodName + "_1";
            }
        }

        if(wordsInMethodSignature.length ==2) {
            // Can be a constructor or a default method like a A()

            String methodAccessModifierType = wordsInMethodSignature[0].trim();
            if(("private").equalsIgnoreCase(methodAccessModifierType)) {
                currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PRIVATE);
            }

            if(("public").equalsIgnoreCase(methodAccessModifierType)) {
                currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PUBLIC);
            }
            if(("protected").equalsIgnoreCase(methodAccessModifierType)) {
                currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PROTECTED);
            }

            // It is not a constructor. A default method
            if(currentMethodMetrics.getMethodAccessModifierType() == null || currentMethodMetrics.getMethodAccessModifierType().equals(AccessModifierType.DEFAULT)) {
                String methodReturnType = methodAccessModifierType;
                currentMethodMetrics.setMethodReturnObject(methodReturnType);
            }
            // A consutrctor. Set remaining fields
            else {
                currentMethodMetrics.setMethodReturnObject("void");
            }

            // Set common fields
            currentMethodMetrics.setStaticMethod(false);
            currentMethodName = wordsInMethodSignature[1];

            while(methodsMap.get(currentMethodName) != null) {
                currentMethodName = currentMethodName + "_1";
            }
        }
        if(wordsInMethodSignature.length == 3) {
            if(wordsInMethodSignature[0].trim().equals("static")) {
                currentMethodMetrics.setStaticMethod(true);
                currentMethodMetrics.setMethodReturnObject(wordsInMethodSignature[1]);
                currentMethodName = wordsInMethodSignature[2];
                while(methodsMap.get(currentMethodName) != null) {
                    currentMethodName = currentMethodName + "_1";
                }
            } else {
                currentMethodMetrics.setStaticMethod(false);
                String methodReturnType = wordsInMethodSignature[0].trim();
                if(("private").equalsIgnoreCase(methodReturnType)) {
                    currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PRIVATE);
                } else {
                    if(("public").equalsIgnoreCase(methodReturnType)) {
                        currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PUBLIC);
                    } else {
                        currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PROTECTED);
                    }
                }

                currentMethodMetrics.setMethodReturnObject(wordsInMethodSignature[1]);
                currentMethodMetrics.setStaticMethod(false);
                currentMethodName = wordsInMethodSignature[2];
                while(methodsMap.get(currentMethodName) != null) {
                    currentMethodName = currentMethodName + "_1";
                }
            }
        }

        if(wordsInMethodSignature.length == 4) {
            currentMethodMetrics.setStaticMethod(true);
            String methodReturnType = null;
            if(wordsInMethodSignature[0].trim().equals("static")) {
                methodReturnType = wordsInMethodSignature[1].trim();
            }
            else {
                methodReturnType = wordsInMethodSignature[0].trim();
            }

            if(("private").equalsIgnoreCase(methodReturnType)) {
                currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PRIVATE);
            } else {
                if(("public").equalsIgnoreCase(methodReturnType)) {
                    currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PUBLIC);
                } else {
                    currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PROTECTED);
                }
            }
            currentMethodMetrics.setMethodReturnObject(wordsInMethodSignature[2]);
            currentMethodName = wordsInMethodSignature[3];
            while(methodsMap.get(currentMethodName) != null) {
                currentMethodName = currentMethodName + "_1";
            }
        }

        // Signature Example: private static final List add()
        if(wordsInMethodSignature.length == 5) {
            currentMethodMetrics.setStaticMethod(true);
            String methodReturnType = null;

            for(int i=0; i<3; i++) {
                if(wordsInMethodSignature[i].trim().equals("static") || wordsInMethodSignature[i].trim().equals("final")) {
                    continue;
                }
                else {
                    methodReturnType = wordsInMethodSignature[i];
                    break;
                }
            }

            if(("private").equalsIgnoreCase(methodReturnType)) {
                currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PRIVATE);
            }
            else {
                if(("public").equalsIgnoreCase(methodReturnType)) {
                    currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PUBLIC);
                } else {
                    currentMethodMetrics.setMethodAccessModifierType(AccessModifierType.PROTECTED);
                }
            }
            currentMethodMetrics.setMethodReturnObject(wordsInMethodSignature[3]);
            currentMethodName = wordsInMethodSignature[4];
            while(methodsMap.get(currentMethodName) != null) {
                currentMethodName = currentMethodName + "_1";
            }
        }

        // TODO - Risky phase. Put in try catch
    }


		if(currentLine.contains("{") && (inAMethod == true || inStaticBlockOrInterfaceOrEnum == true)) {
        stackBalancerForMethodEndFinding++;
    }

		if(currentLine.contains("}") && (inAMethod || inStaticBlockOrInterfaceOrEnum)) {
        stackBalancerForMethodEndFinding--;
        if(stackBalancerForMethodEndFinding == 0 && inAMethod == true) {
            // Method ends
            inAMethod = false;
            currentMethodMetrics.setMethodLength(methodLinesCounter);
            currentMethodMetrics.setMethodName(currentMethodName);
            methodsMap.put(currentMethodName, currentMethodMetrics);
            currentMethodName = "";
            System.out.println(currentMethodMetrics.toString());
            currentMethodMetrics = new MethodMetrics();
            renameCounter = 0;
            methodLinesCounter =0 ;
            System.out.println("curr line: " + totalLinesCounter + " " + currentLine);
        }
        if(stackBalancerForMethodEndFinding == 0 && inStaticBlockOrInterfaceOrEnum) {
            inStaticBlockOrInterfaceOrEnum = false;
            System.out.println("Static block ended");
            System.out.println("curr line: "  + totalLinesCounter + " " + currentLine);
        }
    }
}

			bufferedReader.close();
                    fileReader.close();

                    System.out.println(totalLinesCounter + " " + classLInesCounter);

                    for(String method: methodsMap.keySet()) {
                    System.out.println("Is mpa null? + " + methodsMap == null);
                    System.out.println("Method name: " + method + ", Props: " + methodsMap.get(method));
                    }
                    return methodsMap;
                    }
                    }



