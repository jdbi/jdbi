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
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import javax.tools.Diagnostic.Kind;
import javax.tools.JavaFileObject;

import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.config.ConfigRegistry;
import org.jdbi.v3.core.extension.ExtensionMetadata;
import org.jdbi.v3.core.extension.ExtensionMetadata.ExtensionHandlerInvoker;
import org.jdbi.v3.core.extension.HandleSupplier;
import org.jdbi.v3.core.internal.JdbiClassUtils;
import org.jdbi.v3.sqlobject.SqlObject;

import static java.lang.String.format;

@SupportedAnnotationTypes(GenerateSqlObjectProcessor.GENERATE_SQL_OBJECT_ANNOTATION_NAME)
public class GenerateSqlObjectProcessor extends AbstractProcessor {

    public static final String GENERATE_SQL_OBJECT_ANNOTATION_NAME = "org.jdbi.v3.sqlobject.GenerateSqlObject";

    private static final Set<ElementKind> ACCEPTABLE_ELEMENT_TYPES = EnumSet.of(ElementKind.CLASS, ElementKind.INTERFACE);

    private Elements elementUtils;
    private Types typeUtils;
    private Filer filer;
    private Messager messager;

    @Override
    public SourceVersion getSupportedSourceVersion() {
        // latest supported version of javac. The code itself builds on all release >= 8.
        return SourceVersion.latestSupported();
    }

    @Override
    @SuppressWarnings("PMD.AvoidSynchronizedAtMethodLevel")
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.elementUtils = processingEnv.getElementUtils();
        this.typeUtils = processingEnv.getTypeUtils();
        this.filer = processingEnv.getFiler();
        this.messager = processingEnv.getMessager();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        TypeElement generateSqlAnnotation = elementUtils.getTypeElement(GENERATE_SQL_OBJECT_ANNOTATION_NAME);

        Set<? extends Element> annotatedElements = roundEnv.getElementsAnnotatedWith(generateSqlAnnotation);

        for (Element element : annotatedElements) {
            if (!ACCEPTABLE_ELEMENT_TYPES.contains(element.getKind())) {
                throw new IllegalStateException("@GenerateSqlObject annotation on unsupported element: " + element);
            }
            if (!element.getModifiers().contains(Modifier.ABSTRACT)) {
                throw new IllegalStateException("@GenerateSqlObject on a non-abstract class: " + element);
            }

            generateSourceFile(element);
        }

        return false;
    }

    private void generateSourceFile(Element element) {
        messager.printMessage(Kind.NOTE, format("[jdbi] generating for %s", element));

        try {
            final SqlObjectFile sqlObjectFile = new SqlObjectFile((TypeElement) element);

            // create extra methods
            sqlObjectFile.addMethod(forMethod(SqlObject.class, "getHandle"));
            sqlObjectFile.addMethod(forMethod(SqlObject.class, "withHandle"));

            // write the source file
            sqlObjectFile.writeFile();

        } catch (RuntimeException e) {
            messager.printMessage(Kind.ERROR, format("@GenerateSqlObject processor threw an exception for '%s': %s", element, e));
            throw e;
        }
    }

    private ExecutableElement forMethod(Class<?> klass, String name) {
        return elementUtils.getTypeElement(klass.getName()).getEnclosedElements()
                .stream()
                .filter(e -> e.getSimpleName().toString().equals(name))
                .findFirst()
                .map(ExecutableElement.class::cast)
                .orElseThrow(() -> new IllegalStateException(format("no %s.%s found!", klass, name)));
    }

    // can't be in the inner class b/c static.
    private static String getImplementationClassName(TypeElement typeElement) {
        return typeElement.getSimpleName() + "Impl";
    }

    private final class SqlObjectFile {

        private final TypeElement typeElement;
        private final TypeName typeName;
        private final TypeSpec.Builder implementationBuilder;
        private final TypeSpec.Builder onDemandBuilder;
        private final CodeBlock.Builder implementationCtorBuilder = CodeBlock.builder();
        private long counter = 0;


        private SqlObjectFile(TypeElement typeElement) {
            this.typeElement = typeElement;
            this.typeName = TypeName.get(typeElement.asType());

            // create the implementation type, by convention its name ends with "Impl"
            this.implementationBuilder = TypeSpec.classBuilder(getImplementationClassName(typeElement))
                    .addModifiers(Modifier.PUBLIC);
            // add SqlObject and the element itself as supertypes
            addSupertypes(this.implementationBuilder);

            // create the onDemand type, by convention it is a nested class within the implementation type
            this.onDemandBuilder = TypeSpec.classBuilder("OnDemand")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC);
            // add SqlObject and the element itself as supertypes
            addSupertypes(this.onDemandBuilder);

            // OnDemand has a c'tor that takes a Jdbi object.
            this.onDemandBuilder.addField(Jdbi.class, "jdbi", Modifier.PRIVATE, Modifier.FINAL);
            this.onDemandBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(Jdbi.class, "jdbi")
                    .addCode("this.jdbi = jdbi;\n")
                    .build());

            // create all internal methods
            getMethods().forEach(this::addMethod);
        }

        private void addMethod(ExecutableElement method) {
            addImplementationMethod(method);
            addOnDemandMethod(method);
        }

        private List<ExecutableElement> getMethods() {
            // returns all non-private methods enclosed by the type
            return typeElement.getEnclosedElements().stream()
                    .filter(element -> element.getKind() == ElementKind.METHOD)
                    .map(ExecutableElement.class::cast)
                    .filter(element -> !element.getModifiers().contains(Modifier.PRIVATE))
                    .collect(Collectors.toList());
        }

        private void addSupertypes(TypeSpec.Builder builder) {
            if (this.typeElement.getKind() == ElementKind.CLASS) {
                builder.superclass(this.typeName);
            } else {
                builder.addSuperinterface(this.typeName);
            }
            builder.addSuperinterface(SqlObject.class);
        }

        private void addImplementationMethod(ExecutableElement method) {
            final String paramTypes = method.getParameters().stream()
                    .map(VariableElement::asType)
                    .map(typeUtils::erasure)
                    .map(t -> t + ".class")
                    .collect(Collectors.joining(","));

            final Name methodName = method.getSimpleName();
            final String methodField = "m_" + methodName + "_" + counter;
            final String invokerField = "i_" + methodName + "_" + counter++;

            // the method field is initialized with a call to JdbiClassUtils.methodLookup
            implementationBuilder.addField(FieldSpec.builder(Method.class, methodField, Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("$T.methodLookup($T.class, $S$L$L)",
                            JdbiClassUtils.class,
                            method.getEnclosingElement().asType(),
                            methodName,
                            paramTypes.isEmpty() ? "" : ", ",
                            paramTypes)
                    .build());

            // the invoker field is initialized in the c'tor
            implementationBuilder.addField(ExtensionHandlerInvoker.class, invokerField, Modifier.PRIVATE, Modifier.FINAL);

            implementationCtorBuilder.add("$L = extensionMetadata.createExtensionHandlerInvoker(this, $L, handleSupplier, config);\n",
                    invokerField,
                    methodField);

            final CodeBlock.Builder body = CodeBlock.builder();
            final String castReturn = method.getReturnType().getKind() == TypeKind.VOID
                            ? ""
                            : format("return (%s)", method.getReturnType());
            final String paramList = paramList(method);

            if (method.getModifiers().contains(Modifier.ABSTRACT)) {
                body.add("$L $L.invoke($L);\n", castReturn, invokerField, paramList);
            } else {
                body.add("$L $L.call(() -> ", castReturn, invokerField);

                if (method.getModifiers().contains(Modifier.DEFAULT)) {
                    body.add("$T.", method.getEnclosingElement().asType());
                }
                body.add("super.$L($L));\n", methodName, paramList);
            }

            implementationBuilder.addMethod(MethodSpec.overriding(method)
                    .addCode(body.build())
                    .build());
        }

        private void addOnDemandMethod(ExecutableElement method) {
            final String castReturn;
            final String jdbiMethod;

            if (method.getReturnType().getKind() == TypeKind.VOID) {
                jdbiMethod = "useExtension";
                castReturn = "";
            } else {
                jdbiMethod = "withExtension";
                castReturn = format("return (%s)", method.getReturnType());
            }

            onDemandBuilder.addMethod(MethodSpec.overriding(method)
                    .addCode(CodeBlock.builder()
                            .add("$L jdbi.$L($T.class, e -> (($L) e).$L($L));\n",
                                    castReturn,
                                    jdbiMethod,
                                    typeElement.asType(),
                                    getImplementationClassName(typeElement),
                                    method.getSimpleName(),
                                    paramList(method))
                            .build())
                    .build());
        }

        private String paramList(final ExecutableElement method) {
            return method.getParameters().stream()
                    .map(VariableElement::getSimpleName)
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }

        private void writeFile() {
            implementationBuilder.addType(onDemandBuilder.build());

            // add constructor at the end, every method added code to it.
            implementationBuilder.addMethod(MethodSpec.constructorBuilder()
                    .addModifiers(Modifier.PUBLIC)
                    .addParameter(ExtensionMetadata.class, "extensionMetadata")
                    .addParameter(HandleSupplier.class, "handleSupplier")
                    .addParameter(ConfigRegistry.class, "config")
                    .addCode(implementationCtorBuilder.build())
                    .build());

            try {
                final PackageElement typePackage = elementUtils.getPackageOf(typeElement);

                final JavaFileObject file = filer.createSourceFile(format("%s.%s", typePackage, getImplementationClassName(typeElement)), typeElement);
                try (Writer out = file.openWriter()) {
                    JavaFile.builder(typePackage.toString(), implementationBuilder.build())
                            .build()
                            .writeTo(out);
                }
            } catch (IOException e) {
                // Similar to the auto/value annotation processor, try to work around
                // https://bugs.eclipse.org/bugs/show_bug.cgi?id=367599. If that bug manifests, we may get
                // invoked more than once for the same file, so ignoring the ability to overwrite it is the
                // right thing to do. If we are unable to write for some other reason, we should get a compile
                // error later because user code will have a reference to the code we were supposed to
                // generate.
                messager.printMessage(Kind.WARNING, format("Could not write generated class %s: %s", typeElement, e));
            }
        }
    }
}
