package com.example.gsonoptimize_processor;

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
import com.sun.tools.javac.code.Type;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * date: 2021/11/19
 */
class TypeAdapterGenerator {
    public static HashMap<Symbol.ClassSymbol, String> mSymbolStringHashMap = new HashMap<>();

    private String tName = "T";
    private TypeVariableName tTypeName = TypeVariableName.get(tName);
    private ProcessingEnvironment mProcessingEnvironment;
    private TypeElement typeElement;

    public TypeAdapterGenerator(ProcessingEnvironment roundEnvironment, TypeElement typeElement) {
        this.mProcessingEnvironment = roundEnvironment;
        this.typeElement = typeElement;
    }

    public void generateTypeAdapter() {
        String packageName = "com.example.gsonoptimize_processor";
        String typeAdapterClassName = "NoReflect" + typeElement.getSimpleName().toString() + "TypeAdapter";
        mSymbolStringHashMap.put((Symbol.ClassSymbol) typeElement, typeAdapterClassName);

        ClassName typeAdapter = ClassName.get(packageName, typeAdapterClassName);
        ClassName baseTypeAdapter = ClassName.get(TypeAdapter.class);


//        public NoReflectPersonTypeAdapter(Gson gson) {
//            this.gson = gson;
//        }
        MethodSpec constructorMethod = MethodSpec.constructorBuilder()
                .addParameter(ParameterSpec.builder(ClassName.get(Gson.class), "gson").build())
                .addStatement("this.gson = gson").build();


        SymbolList symbolList = getVarSymbols(typeElement);


        TypeSpec.Builder typeAdapterBuilder = TypeSpec.classBuilder(typeAdapter)
                .addTypeVariable(tTypeName)
                .superclass(ParameterizedTypeName.get(baseTypeAdapter, tTypeName))
                .addField(FieldSpec.builder(ClassName.get(Gson.class), "gson").addModifiers(Modifier.PRIVATE).build())
                .addMethod(constructorMethod)
                .addMethod(writeMethod(symbolList, tTypeName))
                .addMethod(readMethod(symbolList, tTypeName))
                .addModifiers(Modifier.PUBLIC);


        JavaFile file = JavaFile.builder(packageName, typeAdapterBuilder.build()).build();

        try {
            file.writeTo(mProcessingEnvironment.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private MethodSpec writeMethod(SymbolList symbolList, TypeVariableName tTypeName) {
//    @Override
//    public void write(JsonWriter out, T value) throws IOException {
//        if (value == null) {
//            out.nullValue();
//            return;
//        }
//        Person person = (Person) value;
//        out.beginObject();
//        out.name("name").value(person.name);
//        out.name("age").value(person.age);
//        out.name("height").value(person.height);
//        out.name("isMan").value(person.isMan);
//        out.name("mAddress");
//        gson.getAdapter(new TypeToken<Person.Address>() {
//        }).write(out, person.mAddress);
//        out.name("mPhoneList");
//        gson.getAdapter(new TypeToken<List<Person.Phone>>() {
//        }).write(out, person.mPhoneList);
//        out.endObject();
//    }
        ArrayList<ParameterSpec> parameterSpecs = new ArrayList<>();
        parameterSpecs.add(ParameterSpec.builder(ClassName.get(JsonWriter.class), "out").build());
        parameterSpecs.add(ParameterSpec.builder(this.tTypeName, "value").build());
        MethodSpec.Builder builder = MethodSpec.methodBuilder("write")
                .addAnnotation(Override.class)
                .addParameters(parameterSpecs)
                .addCode("if (value == null) {$>\n")
                .addStatement("out.nullValue()")
                .addStatement("return")
                .addCode("$<}\n");
        TypeName currentBeanTypeName = ClassName.get(symbolList.classSymbol.type);
        builder.addStatement("$T $N = ($T) value;", currentBeanTypeName,
                symbolList.classSymbol.name.toString().toLowerCase(), currentBeanTypeName);
        builder.addStatement("out.beginObject()");

        for (int i = 0; i < symbolList.mVarSymbols.size(); i++) {
            Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) symbolList.mVarSymbols.get(i);
            Type type = varSymbol.type;
            if (type.isPrimitive()
                    || "java.lang.String".equals(varSymbol.type.tsym.flatName().toString())) {
                builder.addStatement("out.name($S).value($N.$N)", varSymbol.name, symbolList.classSymbol.name.toString().toLowerCase(), varSymbol.name);
            } else {
                builder.addStatement("out.name($S)", varSymbol.name);
                builder.addStatement("gson.getAdapter(new $T<$T>() {\n" +
                        "}).write(out, $N.$N)", ClassName.get(TypeToken.class), ClassName.get(varSymbol.type), symbolList.classSymbol.name.toString().toLowerCase(), varSymbol.name);
            }
        }
        builder.addStatement("out.endObject()");

        return builder
                .addModifiers(Modifier.PUBLIC)
                .addException(ClassName.get(IOException.class))
                .build();
    }

    private MethodSpec readMethod(SymbolList symbolList, TypeVariableName tTypeName) {
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

        for (int i = 0; i < symbolList.mClassSymbols.size(); i++) {
            new TypeAdapterGenerator(mProcessingEnvironment, (Symbol.ClassSymbol) symbolList.mClassSymbols.get(i)).generateTypeAdapter();
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
                    String paramName = methodSymbol.params.get(j).name.toString().toLowerCase();
                    for (int k = 0; k < symbolList.mVarSymbols.size(); k++) {
                        Symbol.VarSymbol varSymbol = symbolList.mVarSymbols.get(k);
                        String varName = varSymbol.name.toString().toLowerCase();
                        if (varName.equals(paramName) ||
                                varName.equals("m" + paramName)) {
                            //?????????????????????????????????
                            stringBuilder.append(varSymbol.name.toString()).append(", ");
                            break;

                        }
                    }
//                    stringBuilder.append(name).append(", ");
                }
                break;
            }
        }

        builder.addStatement(stringBuilder.substring(0, stringBuilder.length() - 2) + ")", ClassName.get(symbolList.classSymbol.type));

        return builder
                .addModifiers(Modifier.PUBLIC)
                .addException(ClassName.get(IOException.class))
                .returns(tTypeName)
                .build();
    }

    private SymbolList getVarSymbols(TypeElement typeElement) {
        SymbolList result = new SymbolList();
        result.classSymbol = (Symbol.ClassSymbol) typeElement;
        try {
            Class<?> scopeImplClass = Class.forName(Scope.class.getName() + "$ScopeImpl");
            Field tableField = scopeImplClass.getDeclaredField("table");
            tableField.setAccessible(true);
            Class<?> entryClass = Class.forName(Scope.class.getName() + "$Entry");
            Field symField = entryClass.getDeclaredField("sym");
            symField.setAccessible(true);


            Object[] table = (Object[]) tableField.get(result.classSymbol.members_field);
            for (int i = 0; i < table.length; i++) {
                Object entry = table[i];
                if (entry == null) {
                    continue;
                }
                Object o = symField.get(entry);
                if (o instanceof Symbol.VarSymbol) {
                    Symbol.VarSymbol varSymbol = (Symbol.VarSymbol) o;
                    if (!varSymbol.isStatic())
                        result.mVarSymbols.add(varSymbol);
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
        Symbol.ClassSymbol classSymbol;
        ArrayList<Symbol.VarSymbol> mVarSymbols = new ArrayList<>();
        ArrayList<Symbol.ClassSymbol> mClassSymbols = new ArrayList<>();
        ArrayList<Symbol.MethodSymbol> mMethodSymbols = new ArrayList<>();


    }
}
