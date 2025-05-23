/*
 * Copyright 2012-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.plugin.core.config;

import java.util.Map;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AutowireCandidateQualifier;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.plugin.core.OrderAwarePluginRegistry;
import org.springframework.plugin.core.Plugin;
import org.springframework.plugin.core.support.PluginRegistryFactoryBean;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * {@link ImportBeanDefinitionRegistrar} to register {@link PluginRegistryFactoryBean} instances for type listed in
 * {@link EnablePluginRegistries}. Picks up {@link Qualifier} annotations used on the plugin interface and forwards them
 * to the bean definition for the factory.
 *
 * @author Oliver Gierke
 */
public class PluginRegistriesBeanDefinitionRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Logger LOG = LoggerFactory.getLogger(PluginRegistriesBeanDefinitionRegistrar.class);

	/*
	 * (non-Javadoc)
	 * @see org.springframework.context.annotation.ImportBeanDefinitionRegistrar#registerBeanDefinitions(org.springframework.core.type.AnnotationMetadata, org.springframework.beans.factory.support.BeanDefinitionRegistry)
	 */
	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {

		Map<String, Object> annotationAttributes = importingClassMetadata
				.getAnnotationAttributes(EnablePluginRegistries.class.getName());

		if (annotationAttributes == null) {
			LOG.info("No EnablePluginRegistries annotation found on type {}!", importingClassMetadata.getClassName());
			return;
		}

		@Nullable
		Class<?>[] types = (Class<?>[]) annotationAttributes.get("value");

		if (types == null) {
			return;
		}

		for (Class<?> type : types) {

			RootBeanDefinition beanDefinition = new RootBeanDefinition(PluginRegistryFactoryBean.class);
			beanDefinition.setTargetType(getTargetType(type, OrderAwarePluginRegistry.class));
			beanDefinition.getPropertyValues().addPropertyValue("type", type);

			Qualifier annotation = type.getAnnotation(Qualifier.class);

			// If the plugin interface has a Qualifier annotation, propagate that to the bean definition of the registry
			if (annotation != null) {
				AutowireCandidateQualifier qualifierMetadata = new AutowireCandidateQualifier(Qualifier.class);
				qualifierMetadata.setAttribute(AutowireCandidateQualifier.VALUE_KEY, annotation.value());
				beanDefinition.addQualifier(qualifierMetadata);
			}

			// Default
			String beanName = annotation == null //
					? StringUtils.uncapitalize(type.getSimpleName() + "Registry") //
					: annotation.value();

			registry.registerBeanDefinition(beanName, beanDefinition);
		}
	}

	/**
	 * Returns the target type of the {@link PluginRegistry} for the given plugin type.
	 *
	 * @param pluginType must not be {@literal null}.
	 * @return
	 */
	private static ResolvableType getTargetType(Class<?> pluginClass, Class<?> wrapper) {

		Assert.notNull(pluginClass, "Plugin type must not be null!");

		ResolvableType delimiterType = ResolvableType.forClass(Plugin.class, pluginClass).getGeneric(0);
		ResolvableType pluginType = ResolvableType.forClass(pluginClass);

		return ResolvableType.forClassWithGenerics(wrapper, pluginType, delimiterType);
	}
}
