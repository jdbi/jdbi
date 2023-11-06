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
import java.util.Map;
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
        Iterable<BeanDefinition> repositoryBeanDefinitions = resolveRepositoryBeanDefinitions(metadata);
        for (BeanDefinition repositoryBeanDefinition : repositoryBeanDefinitions) {
            AnnotationMetadata annotationMetadata = ((AnnotatedBeanDefinition) repositoryBeanDefinition).getMetadata();
            registerJdbiRepositoryFactoryBean(registry, annotationMetadata);
        }
    }

    private void registerJdbiRepositoryFactoryBean(BeanDefinitionRegistry registry, AnnotationMetadata annotationMetadata) {
        String className = annotationMetadata.getClassName();
        @SuppressWarnings("rawtypes")
        Class clazz = ClassUtils.resolveClassName(className, null);

        Map<String, Object> attributes = getAnnotationAttributes(JdbiRepository.class, annotationMetadata);
        String jdbiQualifier = (String) attributes.get("jdbiQualifier");
        String value = (String) attributes.get("value");

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

    private Iterable<BeanDefinition> resolveRepositoryBeanDefinitions(AnnotationMetadata metadata) {
        LinkedHashSet<BeanDefinition> repositoryDefinitions = new LinkedHashSet<>();
        Map<String, Object> attrs = getAnnotationAttributes(EnableJdbiRepositories.class, metadata);
        final Class<?>[] clients = (Class<?>[]) attrs.get("repositories");
        if (clients.length > 0) {
            for (Class<?> clazz : clients) {
                repositoryDefinitions.add(new AnnotatedGenericBeanDefinition(clazz));
            }
        } else {
            ClassPathScanningCandidateComponentProvider scanner = createScanner();
            Set<String> basePackages = resolveBasePackages(metadata);
            for (String basePackage : basePackages) {
                repositoryDefinitions.addAll(scanner.findCandidateComponents(basePackage));
            }
        }
        return repositoryDefinitions;
    }

    private Set<String> resolveBasePackages(AnnotationMetadata metadata) {
        Map<String, Object> attributes = getAnnotationAttributes(EnableJdbiRepositories.class, metadata);
        Set<String> basePackages = new HashSet<>();
        for (String pkg : (String[]) attributes.get("value")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (String pkg : (String[]) attributes.get("basePackages")) {
            if (StringUtils.hasText(pkg)) {
                basePackages.add(pkg);
            }
        }
        for (Class<?> clazz : (Class<?>[]) attributes.get("basePackageClasses")) {
            basePackages.add(ClassUtils.getPackageName(clazz));
        }

        if (basePackages.isEmpty()) {
            basePackages.add(ClassUtils.getPackageName(metadata.getClassName()));
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

    private Map<String, Object> getAnnotationAttributes(Class<?> annotationClass, AnnotationMetadata metadata) {
        Map<String, Object> attributes = metadata.getAnnotationAttributes(annotationClass.getCanonicalName());
        if (attributes == null) {
            throw new IllegalStateException("Annotation for " + annotationClass + " not found in metadata?!");
        }
        return attributes;
    }
}
