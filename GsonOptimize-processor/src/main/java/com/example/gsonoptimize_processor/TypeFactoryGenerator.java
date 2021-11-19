package com.example.gsonoptimize_processor;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeSpec;
import com.squareup.javapoet.TypeVariableName;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Symbol;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;

/**
 * author: yuxu
 * date: 2021/11/19
 * TODO yuxu（徐郁） 补充说明
 */
class TypeFactoryGenerator {
    public static HashMap<Symbol.ClassSymbol, String> mSymbolStringHashMap = new HashMap<>();

    private String tName = "T";
    private TypeVariableName tTypeName = TypeVariableName.get(tName);
    private ProcessingEnvironment mProcessingEnvironment;

    public TypeFactoryGenerator(ProcessingEnvironment roundEnvironment) {
        this.mProcessingEnvironment = roundEnvironment;
    }

    public void generateTypeFactory() {
        String packageName = "com.example.gsonoptimize_processor";
        ClassName typeAdapterFactory = ClassName.get(packageName, "NoReflectTypeAdapterFactory");

        TypeSpec.Builder typeAdapterBuilder = TypeSpec.classBuilder(typeAdapterFactory)
                .addSuperinterface(ClassName.get(TypeAdapterFactory.class))
                .addMethod(getCreateMethod())
                .addModifiers(Modifier.PUBLIC);


        JavaFile file = JavaFile.builder(packageName, typeAdapterBuilder.build()).build();

        try {
            file.writeTo(mProcessingEnvironment.getFiler());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private MethodSpec getCreateMethod() {
        ArrayList<ParameterSpec> parameterSpecs = new ArrayList<>();
        parameterSpecs.add(ParameterSpec.builder(ParameterizedTypeName.get(Gson.class), "gson").build());
        parameterSpecs.add(ParameterSpec.builder(ParameterizedTypeName.get(ClassName.get(TypeToken.class), tTypeName), "type").build());

        MethodSpec.Builder builder = MethodSpec.methodBuilder("create")
                .addParameters(parameterSpecs);

        for (Map.Entry<Symbol.ClassSymbol, String> entry : mSymbolStringHashMap.entrySet()) {
//        if (Person.class.equals(type.getType())) {
//            return new NoReflectPersonTypeAdapter<>(gson);
//        }
            builder.addCode("if ($T.class.equals(type.getType())) {\n", entry.getKey());
            builder.addStatement("return new $N<>(gson)", entry.getValue());
            builder.addCode("}\n");

        }
        builder.addStatement("return null");
        return builder
                .returns(ParameterizedTypeName.get(ClassName.get(TypeAdapter.class), tTypeName))
                .addModifiers(Modifier.PUBLIC)
                .addTypeVariable(tTypeName)
                .build();
    }
}
