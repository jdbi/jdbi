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
package org.jdbi.v3.spring5;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * This bean registers the bean definitions of all repositories.
 * Interfaces found using the configuration of {@link EnableJdbiRepositories}
 * and annotated with {@link JdbiRepository} will be registered.
 */
public class JdbiRepositoryRegistrar implements ImportBeanDefinitionRegistrar {

    @Override
    public void registerBeanDefinitions(AnnotationMetadata metadata, BeanDefinitionRegistry registry) {
        EnableJdbiRepositories annotation = metadata.getAnnotations().get(EnableJdbiRepositories.class).synthesize();
        String annotatedClass = metadata.getClassName();
        Iterable<BeanDefinition> repositoryBeanDefinitions = resolveRepositoryBeanDefinitions(annotation, annotatedClass);
        for (BeanDefinition repositoryBeanDefinition : repositoryBeanDefinitions) {
            AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) repositoryBeanDefinition).getMetadata();
            String repositoryClass = annotationMetadata.getClassName();
            JdbiRepository repositoryAnnotation = annotationMetadata.getAnnotations().get(JdbiRepository.class).synthesize();
            registerJdbiRepositoryFactoryBean(registry, repositoryAnnotation, repositoryClass);
        }
    }

    private void registerJdbiRepositoryFactoryBean(BeanDefinitionRegistry registry, JdbiRepository annotation, String className) {
        @SuppressWarnings("rawtypes")
        Class clazz = ClassUtils.resolveClassName(className, null);
        String jdbiQualifier = annotation.jdbiQualifier();
        String value = annotation.value();

        JdbiRepositoryFactoryBean factoryBean = new JdbiRepositoryFactoryBean();
        factoryBean.setObjectType(clazz);
        factoryBean.setBeanFactory((BeanFactory) registry);
        if (StringUtils.hasText(jdbiQualifier)) {
            factoryBean.setJdbiQualifier(jdbiQualifier);
        }

        @SuppressWarnings("unchecked")
        BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(clazz, factoryBean::getObject);
        String beanName = StringUtils.hasText(value) ? value : className;
        registry.registerBeanDefinition(beanName, builder.getBeanDefinition());
    }

    private Iterable<BeanDefinition> resolveRepositoryBeanDefinitions(EnableJdbiRepositories annotation, String annotatedClass) {
        LinkedHashSet<BeanDefinition> repositoryDefinitions = new LinkedHashSet<>();
        if (annotation.repositories().length > 0) {
            for (Class<?> clazz : annotation.repositories()) {
                repositoryDefinitions.add(new AnnotatedGenericBeanDefinition(clazz));
            }
        } else {
            ClassPathScanningCandidateComponentProvider scanner = createScanner();
            Set<String> basePackages = resolveBasePackages(annotation, annotatedClass);
            for (String basePackage : basePackages) {
                repositoryDefinitions.addAll(scanner.findCandidateComponents(basePackage));
            }
        }
        return repositoryDefinitions;
    }

    private Set<String> resolveBasePackages(EnableJdbiRepositories annotation, String annotatedClass) {
        Set<String> basePackages = new HashSet<>();
        for (String pkg : annotation.value()) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : annotation.basePackages()) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : annotation.basePackageClasses()) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(annotatedClass));
        }
        return basePackages;
    }

    private ClassPathScanningCandidateComponentProvider createScanner() {
        var scanner = new ClassPathScanningCandidateComponentProvider() {
            @Override
            protected boolean isCandidateComponent(AnnotatedBeanDefinition beanDefinition) {
                return beanDefinition.getMetadata().isInterface();
            }
        };
        scanner.addIncludeFilter(new AnnotationTypeFilter(JdbiRepository.class));
        return scanner;
    }
}
