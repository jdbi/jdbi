/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.generator;

import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.sqlobject.SqlObject;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData;
import org.jdbi.v3.sqlobject.internal.SqlObjectInitData.InContextInvoker;

@SupportedAnnotationTypes("org.jdbi.v3.sqlobject.GenerateSqlObject")
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class GenerateSqlObjectProcessor extends AbstractProcessor {
    private static final Set<ElementKind> ACCEPTABLE = EnumSet.of(ElementKind.CLASS, ElementKind.INTERFACE);
    private long counter = 0;

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (annotations.isEmpty()) {
            return false;
        }
        final TypeElement gens = annotations.iterator().next();
        final Set<? extends Element> annoTypes = roundEnv.getElementsAnnotatedWith(gens);
        annoTypes.forEach(this::tryGenerate);
        return true;
    }

    private void tryGenerate(Element sqlObj) {
        try {
            generate(sqlObj);
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failure: " + ex, sqlObj);
            throw new RuntimeException(ex);
        }
    }

    private void generate(Element sqlObjE) throws IOException {
        processingEnv.getMessager().printMessage(Kind.NOTE, String.format("[jdbi] generating for %s", sqlObjE));
        if (!ACCEPTABLE.contains(sqlObjE.getKind())) {
            throw new IllegalStateException("Generate on non-class: " + sqlObjE);
        }
        if (!sqlObjE.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new IllegalStateException("Generate on non-abstract class: " + sqlObjE);
        }

        final TypeElement sqlObj = (TypeElement) sqlObjE;
        final String implName = sqlObj.getSimpleName() + "Impl";
        final TypeSpec.Builder implSpec = TypeSpec.classBuilder(implName).addModifiers(Modifier.PUBLIC);
        final TypeName superName = TypeName.get(sqlObj.asType());
        addSuper(implSpec, sqlObj, superName);
        implSpec.addSuperinterface(SqlObject.class);

        final CodeBlock.Builder staticInit = CodeBlock.builder()
                .add("initData = $T.initData();\n", SqlObjectInitData.class);
        final CodeBlock.Builder constructor = CodeBlock.builder();

        implSpec.addField(SqlObjectInitData.class, "initData", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        List<ExecutableElement> implMethods = sqlObj.getEnclosedElements().stream()
                .filter(ee -> ee.getKind() == ElementKind.METHOD)
                .map(ExecutableElement.class::cast)
                .filter(ee -> !ee.getModifiers().contains(Modifier.PRIVATE))
                .collect(Collectors.toCollection(ArrayList::new));
        implMethods.add(element(SqlObject.class, "getHandle"));
        implMethods.add(element(SqlObject.class, "withHandle"));

        implMethods.stream()
                   .map(ee -> generateMethod(implSpec, staticInit, constructor, ee))
                   .forEach(implSpec::addMethod);

        final TypeSpec.Builder onDemand = TypeSpec.classBuilder("OnDemand");
        onDemand.addModifiers(Modifier.PUBLIC, Modifier.STATIC);
        onDemand.addField(Jdbi.class, "db", Modifier.PRIVATE, Modifier.FINAL);
        onDemand.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(Jdbi.class, "db")
                .addCode("this.db = db;\n")
                .build());
        addSuper(onDemand, sqlObj, superName);
        implMethods.stream()
                   .map(method -> generateOnDemand(sqlObj, method))
                   .forEach(onDemand::addMethod);
        implSpec.addType(onDemand.build());

        implSpec.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(HandleSupplier.class, "handle")
                .addParameter(ConfigRegistry.class, "config")
                .addCode(constructor.build())
                .build());

        implSpec.addStaticBlock(staticInit.build());

        final JavaFileObject file = processingEnv.getFiler().createSourceFile(processingEnv.getElementUtils().getPackageOf(sqlObjE) + "." + implName, sqlObjE);
        try (Writer out = file.openWriter()) {
            JavaFile.builder(packageName(sqlObj), implSpec.build()).build().writeTo(out);
        }
    }

    private MethodSpec generateMethod(TypeSpec.Builder typeBuilder, CodeBlock.Builder staticInit, CodeBlock.Builder init, Element el) {
        final Types typeUtils = processingEnv.getTypeUtils();
        final ExecutableElement method = (ExecutableElement) el;
        final String paramList = paramList(method);
        final String paramTypes = method.getParameters().stream()
                .map(VariableElement::asType)
                .map(typeUtils::erasure)
                .map(t -> t + ".class")
                .collect(Collectors.joining(","));
        final String methodField = "m_" + el.getSimpleName() + "_" + counter;
        final String invokerField = "i_" + el.getSimpleName() + "_" + counter++;
        typeBuilder.addField(Method.class, methodField, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        typeBuilder.addField(new GenericType<Supplier<InContextInvoker>>() {}.getType(), invokerField, Modifier.PRIVATE, Modifier.FINAL);

        staticInit.add("$L = initData.lookupMethod($S, new Class<?>[] {$L});\n",
                methodField,
                el.getSimpleName(),
                paramTypes);

        init.add("$L = initData.lazyInvoker(this, $L, handle, config);\n",
                invokerField,
                methodField);

        final String castReturn =
                method.getReturnType().getKind() == TypeKind.VOID
                ? ""
                : ("return (" + method.getReturnType().toString() + ")"); // NOPMD
        final CodeBlock.Builder body;
        if (method.getModifiers().contains(Modifier.ABSTRACT)) {
            body = CodeBlock.builder()
                    .add("$L $L.get().invoke(new Object[] {$L});\n",
                            castReturn, invokerField, paramList);
        } else {
            body = CodeBlock.builder()
                    .add("$L $L.get().call(() -> ",
                            castReturn,
                            invokerField);
            if (el.getModifiers().contains(Modifier.DEFAULT)) {
                body.add("$T.", method.getEnclosingElement().asType());
            }
            body.add("super.$L($L));\n",
                            el.getSimpleName(),
                            paramList);
        }

        return MethodSpec.overriding(method)
                .addCode(body.build())
                .build();
    }

    private MethodSpec generateOnDemand(TypeElement sqlObjectType, ExecutableElement method) {
        return MethodSpec.overriding(method)
                .addCode(CodeBlock.builder()
                    .add("$L db.$L($T.class, e -> e.$L($L));\n",
                            method.getReturnType().getKind() == TypeKind.VOID ? "" : ("return (" + method.getReturnType().toString() + ")"), // NOPMD
                            method.getReturnType().getKind() == TypeKind.VOID ? "useExtension" : "withExtension",
                            sqlObjectType.asType(),
                            method.getSimpleName(),
                            paramList(method))
                    .build())
                .build();
    }

    private ExecutableElement element(Class<?> klass, String name) {
        return processingEnv.getElementUtils().getTypeElement(klass.getName()).getEnclosedElements()
                .stream()
                .filter(e -> e.getSimpleName().toString().equals(name))
                .findFirst()
                .map(ExecutableElement.class::cast)
                .orElseThrow(() -> new IllegalStateException("no " + klass + "." + name + " found"));
    }

    private String paramList(final ExecutableElement method) {
        return method.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .map(Object::toString)
                .collect(Collectors.joining(","));
    }

    private String packageName(Element e) {
        return processingEnv.getElementUtils().getPackageOf(e).toString();
    }

    private void addSuper(TypeSpec.Builder spec, TypeElement el, TypeName superName) {
        if (el.getKind() == ElementKind.CLASS) {
            spec.superclass(superName);
        } else {
            spec.addSuperinterface(superName);
        }
    }
}
