package com.example.gsonoptimize_processor;

import com.example.gsonoptimize_annotation.GsonOptimize;
import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

import java.io.IOException;
import java.io.Writer;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
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


            TypeSpec.Builder mainActivityBuilder = TypeSpec.classBuilder(typeElement.getQualifiedName().toString())
                    .addModifiers(Modifier.PUBLIC);


            JavaFile file = JavaFile.builder("com.test", mainActivityBuilder.build()).build();

            try {
                file.writeTo(processingEnv.getFiler());
            } catch (IOException e) {
                e.printStackTrace();
            }


        }
        return true;
    }

}
