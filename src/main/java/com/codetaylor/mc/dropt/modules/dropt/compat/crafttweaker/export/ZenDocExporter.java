package com.codetaylor.mc.dropt.modules.dropt.compat.crafttweaker.export;

import com.codetaylor.mc.athenaeum.util.StringHelper;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

public class ZenDocExporter {

  public void export(Path path, Class[] classes) {

    for (int i = 0; i < classes.length; i++) {
      StringBuilder out = new StringBuilder();

      ZenDocClass zenClass = (ZenDocClass) classes[i].getDeclaredAnnotation(ZenDocClass.class);

      if (zenClass == null) {
        continue;
      }

      if (i > 0) {
        out.append("\n");
      }

      // --- Header

      String[] h3 = zenClass.value().split("\\.");
      String zenClassName = h3[h3.length - 1];
      out.append("### Class\n");
      out.append("\n");

      // --- Import

      out.append("```java").append("\n");
      out.append("import ").append(zenClass.value()).append("\n");
      out.append("```").append("\n");
      out.append("\n");

      // --- Class Description

      String[] description = zenClass.description();

      if (description.length > 0) {

        for (String line : description) {
          out.append(this.parse(line)).append("\n");
        }
        out.append("\n");
      }

      // --- Methods

      out.append("#### Methods\n");
      out.append("\n");

      Method[] methods = classes[i].getDeclaredMethods();
      List<MethodAnnotationPair> methodList = this.getSortedMethodList(methods);

      // Add static methods to new list.
      List<MethodAnnotationPair> staticMethodList = methodList.stream()
          .filter(pair -> Modifier.isStatic(pair.method.getModifiers()))
          .collect(Collectors.toList());

      // Remove static methods from main list.
      methodList = methodList.stream()
          .filter(pair -> !Modifier.isStatic(pair.method.getModifiers()))
          .collect(Collectors.toList());

      // --- Static Methods

      if (!staticMethodList.isEmpty()) {
        this.writeMethodList(out, staticMethodList);
      }

      // --- Methods

      if (!methodList.isEmpty()) {
        this.writeMethodList(out, methodList);
      }

      // --- Output

      try {
        Files.write(path.resolve(zenClassName.toLowerCase() + ".md"), out.toString().getBytes());

      } catch (IOException e) {
        e.printStackTrace();
      }
    }

  }

  private void writeMethodList(StringBuilder out, List<MethodAnnotationPair> staticMethodList) {

    for (int j = 0; j < staticMethodList.size(); j++) {

      if (j > 0) {
        out.append("\n");
      }

      this.writeMethod(out, staticMethodList.get(j).method, staticMethodList.get(j).annotation);
    }
  }

  private void writeMethod(StringBuilder out, Method method, ZenDocMethod annotation) {

    String methodName = method.getName();
    Class<?> returnType = method.getReturnType();
    String returnTypeString = this.getSimpleTypeString(returnType);

    out.append("```java").append("\n");

    if (Modifier.isStatic(method.getModifiers())) {
      out.append("static ");
    }

    // Method return type and name
    out.append(returnTypeString).append(" ").append(methodName).append("(");

    Class[] types = method.getParameterTypes();
    String[] names = annotation.args();

    if (types.length != names.length) {
      throw new IllegalStateException("Wrong number of parameter names found for method: " + methodName);
    }

    boolean expand = (types.length > 4);

    if (expand) {
      out.append("\n");
    }

    for (int k = 0; k < types.length; k++) {
      String typeString = this.getSimpleTypeString(types[k]);
      String nameString = names[k];

      if (expand) {
        out.append("  ");
      }

      out.append(typeString).append(" ").append(nameString);

      if (k < types.length - 1) {
        out.append(", ");

        if (expand) {
          out.append("\n");
        }
      }
    }

    if (expand) {
      out.append("\n");
    }

    out.append(");\n");

    out.append("```").append("\n\n");

    String[] description = annotation.description();

    if (description.length > 0) {

      for (String line : description) {
        out.append(this.parse(line));
      }
    }
  }

  private String parse(String line) {

    if (line.startsWith("@see")) {
      String[] links = line.substring(4).trim().split(" ");

      StringBuilder sb = new StringBuilder("For more information, see:\n{: .zen-description }\n\n");

      for (String link : links) {
        sb.append("  * [").append(link).append("](").append(link).append(")\n");
        sb.append("{: .zen-description }\n\n");
      }

      return sb.toString();
    }

    return line + "\n{: .zen-description }\n\n";
  }

  private List<MethodAnnotationPair> getSortedMethodList(Method[] methods) {

    List<MethodAnnotationPair> methodList = new ArrayList<>();

    for (int j = 0; j < methods.length; j++) {
      ZenDocMethod annotation = methods[j].getDeclaredAnnotation(ZenDocMethod.class);

      if (annotation != null) {
        methodList.add(new MethodAnnotationPair(methods[j], annotation));
      }
    }

    methodList.sort(Comparator.comparingInt(o -> o.annotation.order()));
    return methodList;
  }

  private String getSimpleTypeString(Class type) {

    String result = type.getSimpleName();

    if (result.startsWith("Zen")) {
      result = result.substring(3);

    } else if (result.startsWith("String")) {
      result = StringHelper.lowercaseFirstLetter(result);
    }
    return result;
  }

  private static class MethodAnnotationPair {

    public final Method method;
    public final ZenDocMethod annotation;

    private MethodAnnotationPair(Method method, ZenDocMethod annotation) {

      this.method = method;
      this.annotation = annotation;
    }
  }

}
