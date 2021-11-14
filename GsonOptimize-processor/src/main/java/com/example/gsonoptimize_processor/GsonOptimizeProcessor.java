package com.example.gsonoptimize_processor;

import com.example.gsonoptimize_annotation.GsonOptimize;
import com.google.auto.service.AutoService;
import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.util.Name;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.JavaFileObject;

/**
 * 描述信息：
 *
 * @author xujiafeng
 * @date 2021/10/31
 */
@AutoService(Processor.class)
public class GsonOptimizeProcessor extends AbstractProcessor {

    private Elements mElementUtils;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnvironment) {
        super.init(processingEnvironment);
        mElementUtils = processingEnvironment.getElementUtils();
    }

    @Override
    public Set<String> getSupportedAnnotationTypes() {
        HashSet<String> supportType = new HashSet<>();
        supportType.add(GsonOptimize.class.getCanonicalName());
        return supportType;
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        //返回java版本
        return SourceVersion.latestSupported();
    }

    @Override
    public boolean process(Set<? extends TypeElement> set, RoundEnvironment roundEnvironment) {
        //得到所有包含该注解的element集合
        Set<? extends Element> elements = roundEnvironment.getElementsAnnotatedWith(GsonOptimize.class);

        for (Element element : elements) {
            //转换为VariableElement，VariableElement为element的子类
            TypeElement typeElement = (TypeElement) element;


            generateTypeAdapter(typeElement);


        }
        return true;
    }

    private void generateTypeAdapter(TypeElement typeElement) {
        String packageName = "com.example.gsonoptimize_processor";
        ClassName typeAdapter = ClassName.get(packageName, "NoReflect" + typeElement.getSimpleName().toString() + "TypeAdapter");
        ClassName baseTypeAdapter = ClassName.get(TypeAdapter.class);
        String tName = "T";
        TypeVariableName tTypeName = TypeVariableName.get(tName);

//        public NoReflectPersonTypeAdapter(Gson gson) {
//            this.gson = gson;
//        }
        MethodSpec constructorMethod = MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(ClassName.get(Gson.class), "gson").build())
                .addStatement("this.gson = gson").build();

//        @Override
//        public void write(JsonWriter out, T value) {
//
//        }
        ArrayList<ParameterSpec> parameterSpecs = new ArrayList<>();
        parameterSpecs.add(ParameterSpec.builder(ClassName.get(JsonWriter.class), "out").build());
        parameterSpecs.add(ParameterSpec.builder(tTypeName, "value").build());
        MethodSpec writeMethod = MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class)
                .addParameters(parameterSpecs)
                .addModifiers(Modifier.PUBLIC)
                .build();


        TypeSpec.Builder typeAdapterBuilder = TypeSpec.classBuilder(typeAdapter)
                .addTypeVariable(tTypeName)
                .superclass(ParameterizedTypeName.get(baseTypeAdapter, tTypeName))
                .addField(FieldSpec.builder(ClassName.get(Gson.class), "gson").addModifiers(Modifier.PRIVATE).build())
                .addMethod(constructorMethod)
                .addMethod(writeMethod)
                .addMethod(readMethod(typeElement, tTypeName))
                .addModifiers(Modifier.PUBLIC);


        JavaFile file = JavaFile.builder(packageName, typeAdapterBuilder.build()).build();

        try {
            file.writeTo(processingEnv.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MethodSpec readMethod(TypeElement typeElement, TypeVariableName tTypeName) {
//        @Override
//        public T read(JsonReader in) throws IOException {
//            in.beginObject();
//            String name = null;
//            int age = 0;
//            double height = 0;
//            boolean isMan = false;
//            Person.Address mAddress = null;
//            List<Person.Phone> mPhoneList = null;
//            while (in.hasNext()) {
//                switch (in.nextName()) {
//                    case "name":
//                        name = in.nextString();
//                        break;
//                    case "age":
//                        age = in.nextInt();
//                        break;
//                    case "height":
//                        height = in.nextDouble();
//                        break;
//                    case "isMan":
//                        isMan = in.nextBoolean();
//                        break;
//                    case "mAddress":
//                        mAddress = gson.getAdapter(new TypeToken<Person.Address>() {
//                        }).read(in);
//                        break;
//                    case "mPhoneList":
//                        mPhoneList = gson.getAdapter(new TypeToken<List<Person.Phone>>() {
//                        }).read(in);
//                        break;
//                }
//            }
//            in.endObject();
//            Log.d(TAG, "invoke NoReflectPersonTypeAdapter");
//            return (T) new Person(name, age, height, isMan, mAddress, mPhoneList);
//        }

        MethodSpec.Builder builder = MethodSpec.methodBuilder("read")
                .addAnnotation(Override.class)
                .addParameter(ParameterSpec.builder(ParameterizedTypeName.get(JsonReader.class), "in").build())
                .addStatement("in.beginObject()");

        SymbolList symbolList = getVarSymbols(typeElement);
        for (int i = 0; i < symbolList.mClassSymbols.size(); i++) {
            generateTypeAdapter((Symbol.ClassSymbol) symbolList.mClassSymbols.get(i));

        }
        for (int i = 0; i < symbolList.mVarSymbols.size(); i++) {

            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbolList.mVarSymbols.get(i);
            if ("int".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if ("byte".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if ("short".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if ("long".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if ("boolean".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = false", ClassName.get(varSymbol.type));
                continue;
            }
            if ("char".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if ("float".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if ("double".equals(varSymbol.type.tsym.name.toString())) {
                builder.addStatement("$T " + varSymbol.name + " = 0", ClassName.get(varSymbol.type));
                continue;
            }
            if (!varSymbol.type.isPartial()) {
                builder.addStatement("$T " + varSymbol.name + " = null", ClassName.get(varSymbol.type));
                continue;
            }
        }


        builder.addCode("while (in.hasNext()) {\n");
        builder.addCode("switch (in.nextName()) {\n");
        for (int i = 0; i < symbolList.mVarSymbols.size(); i++) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbolList.mVarSymbols.get(i);
            Symbol.ClassSymbol tsym = (Symbol.ClassSymbol) varSymbol.type.tsym;
            String name = tsym.fullname.toString();
            if ("int".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextInt()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("byte".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextInt()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("short".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextInt()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("long".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextLong()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("boolean".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextBoolean()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("char".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextInt()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("float".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextDouble()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("double".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextDouble()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
            if ("java.lang.String".equals(name)) {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = in.nextString()", varSymbol.name);
                builder.addStatement("break");
                continue;
            }
//            if (name.startsWith("java.util.List")) {
//                builder.addCode("case $S:\n", varSymbol.name);
//                builder.addStatement("$N = gson.getAdapter(new $T<$T<$T>>() {\n" +
//                        "                    }).read(in)", varSymbol.name, ClassName.get(TypeToken.class), ClassName.get(List.class), ClassName.get(varSymbol.type));
//                builder.addStatement("break");
//                continue;
//            }
            else {
                builder.addCode("case $S:\n", varSymbol.name);
                builder.addStatement("$N = gson.getAdapter(new $T<$T>() {\n" +
                        "                    }).read(in)", varSymbol.name, ClassName.get(TypeToken.class), ClassName.get(varSymbol.type));
                builder.addStatement("break");
            }

        }
        builder.addCode("}\n");
        builder.addCode("}\n");
        builder.addStatement("in.endObject()");

        StringBuilder stringBuilder = new StringBuilder("return (T) new $T(");

        for (int i = 0; i < symbolList.mMethodSymbols.size(); i++) {
            Symbol.MethodSymbol methodSymbol = symbolList.mMethodSymbols.get(i);
            if ("<init>".equals(methodSymbol.name.toString())) {
                for (int j = 0; j < methodSymbol.params.size(); j++) {
                    String paramName= methodSymbol.params.get(j).name.toString().toLowerCase();
                    for (int k = 0; k < symbolList.mVarSymbols.size(); k++) {
                        Symbol.VarSymbol varSymbol = symbolList.mVarSymbols.get(k);
                        String varName = varSymbol.name.toString().toLowerCase();
                        if (varName.equals(paramName) ||
                                varName.equals("m" + paramName)) {
                            //之前定义的变量名是这个
                            stringBuilder.append(varSymbol.name.toString()).append(", ");
                            break;

                        }
                    }
//                    stringBuilder.append(name).append(", ");
                }
                break;
            }
        }


        Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) typeElement;
        builder.addStatement(stringBuilder.substring(0, stringBuilder.length() - 2) + ")", ClassName.get(classSymbol.type));

        return builder
                .addModifiers(Modifier.PUBLIC)
                .addException(ClassName.get(IOException.class))
                .returns(tTypeName)
                .build();
    }

    private SymbolList getVarSymbols(TypeElement typeElement) {
        SymbolList result = new SymbolList();
        Symbol.ClassSymbol classSymbol = (Symbol.ClassSymbol) typeElement;
        try {
            Class<?> scopeImplClass = Class.forName(Scope.class.getName() + "$ScopeImpl");
            Field tableField = scopeImplClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Class<?> entryClass = Class.forName(Scope.class.getName() + "$Entry");
            Field symField = entryClass.getDeclaredField("sym");
            symField.setAccessible(true);


            Object[] table = (Object[]) tableField.get(classSymbol.members_field);
            for (int i = 0; i < table.length; i++) {
                Object entry = table[i];
                if (entry == null) {
                    continue;
                }
                Object o = symField.get(entry);
                if (o instanceof Symbol.VarSymbol) {
                    result.mVarSymbols.add((Symbol.VarSymbol) o);
                }
                if (o instanceof Symbol.ClassSymbol) {
                    result.mClassSymbols.add((Symbol.ClassSymbol) o);
                }
                if (o instanceof Symbol.MethodSymbol) {
                    result.mMethodSymbols.add((Symbol.MethodSymbol) o);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    static class SymbolList {
        ArrayList<Symbol.VarSymbol> mVarSymbols = new ArrayList<>();
        ArrayList<Symbol.ClassSymbol> mClassSymbols = new ArrayList<>();
        ArrayList<Symbol.MethodSymbol> mMethodSymbols = new ArrayList<>();


    }

}
