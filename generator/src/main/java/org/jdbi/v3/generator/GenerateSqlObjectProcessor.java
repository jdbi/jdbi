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
import java.util.EnumSet;
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
import com.squareup.javapoet.MethodSpec.Builder;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
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
        annoTypes.forEach(this::generate);
        return true;
    }

    private void generate(Element e) {
        try {
            generate0(e);
        } catch (Exception ex) {
            processingEnv.getMessager().printMessage(Kind.ERROR, "Failure: " + ex, e);
            throw new RuntimeException(ex);
        }
    }

    private void generate0(Element e) throws IOException {
        processingEnv.getMessager().printMessage(Kind.NOTE, String.format("[jdbi] generating for %s", e));
        if (!ACCEPTABLE.contains(e.getKind())) {
            throw new IllegalStateException("Generate on non-class: " + e);
        }
        if (!e.getModifiers().contains(Modifier.ABSTRACT)) {
            throw new IllegalStateException("Generate on non-abstract class: " + e);
        }

        final TypeElement te = (TypeElement) e;
        final String implName = te.getSimpleName() + "Impl";
        final TypeSpec.Builder builder = TypeSpec.classBuilder(implName).addModifiers(Modifier.PUBLIC);
        final TypeName superName = TypeName.get(te.asType());
        if (te.getKind() == ElementKind.CLASS) {
            builder.superclass(superName);
        } else {
            builder.addSuperinterface(superName);
        }
        builder.addSuperinterface(SqlObject.class);

        final CodeBlock.Builder staticInit = CodeBlock.builder()
                .add("initData = $T.initData();\n", SqlObjectInitData.class);
        final CodeBlock.Builder constructor = CodeBlock.builder();

        builder.addMethod(generateMethod(builder, staticInit, constructor, element(SqlObject.class, "getHandle")));
        builder.addMethod(generateMethod(builder, staticInit, constructor, element(SqlObject.class, "withHandle")));
        builder.addField(SqlObjectInitData.class, "initData", Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);

        te.getEnclosedElements().stream()
                .filter(ee -> ee.getKind() == ElementKind.METHOD)
                .filter(ee -> !ee.getModifiers().contains(Modifier.PRIVATE))
                .map(ee -> generateMethod(builder, staticInit, constructor, ee))
                .forEach(builder::addMethod);

        builder.addMethod(MethodSpec.constructorBuilder()
                .addModifiers(Modifier.PUBLIC)
                .addParameter(HandleSupplier.class, "handle")
                .addParameter(ConfigRegistry.class, "config")
                .addCode(constructor.build())
                .build());

        builder.addStaticBlock(staticInit.build());

        final JavaFileObject file = processingEnv.getFiler().createSourceFile(processingEnv.getElementUtils().getPackageOf(e) + "." + implName, e);
        try (Writer out = file.openWriter()) {
            JavaFile.builder(packageName(te), builder.build()).build().writeTo(out);
        }
    }

    private MethodSpec generateMethod(TypeSpec.Builder typeBuilder, CodeBlock.Builder staticInit, CodeBlock.Builder init, Element e) {
        final Types typeUtils = processingEnv.getTypeUtils();
        final ExecutableElement ee = (ExecutableElement) e;
        final Builder builder = MethodSpec.overriding(ee);
        final String paramNames = ee.getParameters().stream()
                .map(VariableElement::getSimpleName)
                .map(Object::toString)
                .collect(Collectors.joining(","));
        final String paramTypes = ee.getParameters().stream()
                .map(VariableElement::asType)
                .map(typeUtils::erasure)
                .map(t -> t + ".class")
                .collect(Collectors.joining(","));
        final String methodField = "m_" + e.getSimpleName() + "_" + counter;
        final String invokerField = "i_" + e.getSimpleName() + "_" + counter++;
        typeBuilder.addField(Method.class, methodField, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
        typeBuilder.addField(new GenericType<Supplier<InContextInvoker>>() {}.getType(), invokerField, Modifier.PRIVATE, Modifier.FINAL);

        staticInit.add("$L = initData.lookupMethod($S, new Class<?>[] {$L});\n",
                methodField,
                e.getSimpleName(),
                paramTypes);

        init.add("$L = initData.lazyInvoker(this, $L, handle, config);\n",
                invokerField,
                methodField);

        final CodeBlock.Builder body;
        final String castReturn =
                ee.getReturnType().getKind() == TypeKind.VOID
                ? ""
                : ("return (" + ee.getReturnType().toString() + ")"); // NOPMD
        if (ee.getModifiers().contains(Modifier.ABSTRACT)) {
            body = CodeBlock.builder()
                    .add("$L $L.get().invoke(new Object[] {$L});\n",
                            castReturn, invokerField, paramNames);
        } else {
            body = CodeBlock.builder()
                    .add("$L $L.get().call(() -> super.$L($L));\n",
                            castReturn,
                            invokerField,
                            e.getSimpleName(),
                            paramNames);
        }

        return builder.addCode(body.build()).build();
    }

    private Element element(Class<?> klass, String name) {
        return processingEnv.getElementUtils().getTypeElement(klass.getName()).getEnclosedElements()
                .stream()
                .filter(e -> e.getSimpleName().toString().equals(name))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("no " + klass + "." + name + " found"));
    }

    private String packageName(Element e) {
        return processingEnv.getElementUtils().getPackageOf(e).toString();
    }
}
