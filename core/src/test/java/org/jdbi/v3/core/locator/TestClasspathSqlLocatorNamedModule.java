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
package org.jdbi.v3.core.locator;

import java.io.InputStream;
import java.lang.module.Configuration;
import java.lang.module.ModuleFinder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;

import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@code init.sql} beside a class in a named module is its own package. That package is not open,
 * so {@code ClassLoader.getResourceAsStream} and a cross-module {@code Class.getResourceAsStream}
 * both miss it. {@link ClasspathSqlLocator#locate(Class, String)} must still return the file text.
 */
public class TestClasspathSqlLocatorNamedModule {

    private static final String MODULE_NAME = "org.jdbi.locator.jpmstest";
    private static final String CLASS_NAME = MODULE_NAME + ".NamedModuleSql";
    private static final String INIT_SQL = "select 1\n";

    @Test
    public void testLocateInitSqlBesideNamedModuleClass(@TempDir Path tempDir) throws Exception {
        Path moduleDir = compileNamedModule(tempDir);
        Class<?> type = ModuleLayer.boot()
                .defineModulesWithOneLoader(moduleConfiguration(moduleDir), ClassLoader.getSystemClassLoader())
                .findLoader(MODULE_NAME)
                .loadClass(CLASS_NAME);

        String resourcePath = CLASS_NAME.replace('.', '/') + "/init.sql";
        String resourcePackage = CLASS_NAME;

        assertThat(type.getModule().isNamed()).isTrue();
        assertThat(type.getModule().getLayer()).isNotNull();
        assertThat(type.getModule().getPackages()).contains(resourcePackage);
        assertThat(type.getModule().isOpen(resourcePackage)).isFalse();
        assertThat(type.getClassLoader()).isNotNull();
        try (InputStream classLoaderStream = type.getClassLoader().getResourceAsStream(resourcePath)) {
            assertThat(classLoaderStream).isNull();
        }
        try (InputStream classStream = type.getResourceAsStream("/" + resourcePath)) {
            assertThat(classStream).isNull();
        }

        assertThat(ClasspathSqlLocator.create().locate(type, "init")).isEqualTo(INIT_SQL);
    }

    private static Configuration moduleConfiguration(Path moduleDir) {
        ModuleFinder finder = ModuleFinder.of(moduleDir);
        return ModuleLayer.boot().configuration().resolve(finder, ModuleFinder.of(), Set.of(MODULE_NAME));
    }

    private static Path compileNamedModule(Path tempDir) throws Exception {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new IllegalStateException("JDK compiler is required to host the named-module test");
        }

        Path sourceRoot = tempDir.resolve("src").resolve(MODULE_NAME);
        Path packageDir = sourceRoot.resolve("org/jdbi/locator/jpmstest");
        Files.createDirectories(packageDir);
        Files.writeString(sourceRoot.resolve("module-info.java"), "module " + MODULE_NAME + " {\n}\n", StandardCharsets.UTF_8);
        Files.writeString(packageDir.resolve("NamedModuleSql.java"), """
                package org.jdbi.locator.jpmstest;

                public class NamedModuleSql {}
                """, StandardCharsets.UTF_8);

        Path moduleDir = tempDir.resolve("mod");
        Files.createDirectories(moduleDir);
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            fileManager.setLocation(StandardLocation.CLASS_OUTPUT, List.of(moduleDir.toFile()));
            Iterable<? extends JavaFileObject> units = fileManager.getJavaFileObjects(
                    sourceRoot.resolve("module-info.java").toFile(),
                    packageDir.resolve("NamedModuleSql.java").toFile());
            Boolean compiled = compiler.getTask(null, fileManager, diagnostics, List.of("--release", "17"), null, units).call();
            if (!Boolean.TRUE.equals(compiled)) {
                throw new IllegalStateException("named-module test failed to compile: " + diagnostics.getDiagnostics());
            }
        }

        Path sqlDir = moduleDir.resolve("org/jdbi/locator/jpmstest/NamedModuleSql");
        Files.createDirectories(sqlDir);
        Files.writeString(sqlDir.resolve("init.sql"), INIT_SQL, StandardCharsets.UTF_8);
        return moduleDir;
    }
}
