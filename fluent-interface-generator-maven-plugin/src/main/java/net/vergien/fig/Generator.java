/*
 * Copyright 2016 Daniel Nordhoff-Vergien.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.vergien.fig;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;

import com.squareup.javapoet.AnnotationSpec;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.TypeSpec;

public class Generator {

	private File targetDir;
	private String abstractPrefix;
	private String interfacePrefix;
	private String prefix;
	private List<String> methodPrefixes;
	private Log log;

	public Generator(Log log, File targetDir, String abstractPrefix, String interfacePrefix, String prefix,
			List<String> methodPrefixes) {
		this.log = log;
		this.targetDir = targetDir;
		this.abstractPrefix = abstractPrefix;
		this.interfacePrefix = interfacePrefix;
		this.prefix = prefix;
		this.methodPrefixes = new ArrayList<>(methodPrefixes);
		Collections.sort(this.methodPrefixes, new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				return Integer.compare(o1.length(), o2.length());
			}
		});
	}

	public void createFluentFor(Class<?> sourceClass, String targetPackage, List<String> ignoreMethods,
			String interfaceTargetPackage) throws ClassNotFoundException, IOException {
		String fluentClassName = abstractPrefix + sourceClass.getSimpleName();

		if (interfaceTargetPackage == null) {
			interfaceTargetPackage = targetPackage;
		}
		Class<?> interfaceClass = createInterfaceClass(sourceClass, interfaceTargetPackage);

		Set<MethodSpec> withMethodSpecs = createMethodSpecs(sourceClass, targetPackage, ignoreMethods, interfaceClass);

		Set<MethodSpec> constructorMehtodSpecs = new HashSet<>();
		for (Constructor<?> constructor : sourceClass.getConstructors()) {
			if (java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) {
				constructorMehtodSpecs.add(createConstructorMethodSpec(constructor));
			}
		}
		AnnotationSpec generatedAnnotationSpec = AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", this.getClass().getName()).build();
		TypeSpec.Builder builder = TypeSpec.classBuilder(fluentClassName).addAnnotation(generatedAnnotationSpec)
				.addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).addMethods(constructorMehtodSpecs)
				.addMethods(withMethodSpecs).superclass(sourceClass);
		if (interfaceClass != null) {
			builder.addSuperinterface(interfaceClass);
		}
		TypeSpec fluentClass = builder.build();

		JavaFile fluentFile = JavaFile.builder(targetPackage, fluentClass).build();

		fluentFile.writeTo(targetDir);
	}

	private Class<?> createInterfaceClass(Class<?> sourceClass, String targetPackage) {
		String interfaceClassNameSimple = interfacePrefix + sourceClass.getSimpleName();
		String interfaceClassName = targetPackage + "." + interfaceClassNameSimple;
		Class<?> interfaceClass;
		try {
			log.debug("Search for interface " + interfaceClassName);
			interfaceClass = Thread.currentThread().getContextClassLoader().loadClass(interfaceClassName);
		} catch (ClassNotFoundException ex) {
			log.debug("Interface " + interfaceClassName + " not found");
			return null;
		}
		if (!interfaceClass.isInterface()) {
			log.debug("Interface " + interfaceClassName + " not an interface!");
			return null;
		}
		log.info("Found interface " + interfaceClassName);
		return interfaceClass;
	}

	protected Set<MethodSpec> createMethodSpecs(Class<?> sourceClass, String targetPackage, List<String> ignoreMethods,
			Class<?> interfaceClass) {
		String targetClassName = prefix + sourceClass.getSimpleName();
		Set<MethodSpec> withMethodSpecs = new HashSet<>();
		Type type = sourceClass.getGenericSuperclass();
		Map<String, Type> typeMapping = new HashMap<>();
		if (type instanceof ParameterizedType) {
			ParameterizedType pType = (ParameterizedType) type;
			Type rawType = pType.getRawType();
			Class rawTypeClass = (Class) rawType;

			log.debug("rawType: " + rawType.toString());
			log.debug("pType.getActualTypeArguements: " + Arrays.toString(pType.getActualTypeArguments()));
			log.debug("rawTypeClass.getTypedParamters: " + Arrays.toString(rawTypeClass.getTypeParameters()));
			Type[] actualTypes = pType.getActualTypeArguments();
			System.out.println(actualTypes);
			int i = 0;
			for (TypeVariable<?> typeVariable : rawTypeClass.getTypeParameters()) {
				typeMapping.put(typeVariable.getName(), pType.getActualTypeArguments()[i]);
				i++;
			}
		}
		log.debug("Methods of " + sourceClass.getName() + ":");

		for (Method sourceMethod : sourceClass.getMethods()) {
			log.debug("\tsourceMethod: " + sourceMethod);
			log.debug("\t" + Arrays.toString(sourceMethod.getGenericParameterTypes()));
			if (!ignoreMethods.contains(sourceMethod.getName())) {
				if (!(sourceMethod.isBridge() || sourceMethod.isSynthetic()
						|| sourceMethod.isAnnotationPresent(Deprecated.class))) {
					for (String methodPrefix : methodPrefixes) {
						if (sourceMethod.getName().startsWith(methodPrefix)
								&& sourceMethod.getReturnType().equals(Void.TYPE)) {
							withMethodSpecs
									.add(createWithMethodSpec(sourceMethod, targetPackage + "." + targetClassName,
											methodPrefix, sourceClass, interfaceClass, typeMapping));
							break;
						}
					}
				}
			}

		}
		return withMethodSpecs;
	}

	private MethodSpec createConstructorMethodSpec(Constructor<?> constructor) {
		List<ParameterSpec> parameterSpecs = createParameterSpecs(constructor.getParameters(), new Type[0], null);
		List<String> parameterNames = new ArrayList<>();
		for (ParameterSpec parameterSpec : parameterSpecs) {
			parameterNames.add(parameterSpec.name);
		}
		MethodSpec constructorSpec = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addStatement("super(" + StringUtils.join(parameterNames.iterator(), ", ") + ")")
				.addParameters(parameterSpecs).build();
		return constructorSpec;
	}

	private MethodSpec createWithMethodSpec(Method setter, String targetType, String prefix, Class<?> sourceClass,
			Class<?> interfaceClass, Map<String, Type> typeMapping) {
		String methodName = "with" + setter.getName().substring(prefix.length());

		List<ParameterSpec> parameters = createParameterSpecs(setter.getParameters(), setter.getGenericParameterTypes(),
				typeMapping);
		boolean varargs = false;
		if (!parameters.isEmpty()) {
			varargs = setter.getParameters()[parameters.size() - 1].isVarArgs();
		}
		List<String> parameterNames = new ArrayList<String>();
		for (ParameterSpec parameterSpec : parameters) {
			parameterNames.add(parameterSpec.name);
		}
		ClassName bestGuess = ClassName.bestGuess(targetType);

		MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC)
				.addParameters(parameters)
				.addStatement(
						"this." + setter.getName() + "(" + StringUtils.join(parameterNames.iterator(), ", ") + ")")
				.addStatement("return ($T) this", bestGuess).returns(bestGuess);
		if (varargs) {
			builder.varargs(true);
		}
		if (hasMethod(sourceClass, methodName, setter.getParameterTypes())) {
			builder.addAnnotation(Override.class);
		} else if (interfaceClass != null) {
			if (hasMethod(interfaceClass, methodName, setter.getParameterTypes())) {
				builder.addAnnotation(Override.class);
			}
		}

		return builder.build();
	}

	private List<ParameterSpec> createParameterSpecs(Parameter[] parameters, Type[] genericParameters,
			Map<String, Type> typeMapping) {
		List<ParameterSpec> specs = new ArrayList<>();
		for (int i = 0; i < parameters.length; i++) {
			Parameter parameter = parameters[i];
			Type type = parameter.getType();
			if (typeMapping != null && typeMapping.containsKey(parameter.getParameterizedType().getTypeName())) {
				type = typeMapping.get(parameter.getParameterizedType().getTypeName());
			}
			specs.add(ParameterSpec.builder(type, parameter.getName()).build());
		}

		return specs;
	}

	private boolean isSetter(Method method) {
		return method.getName().startsWith("set") && method.getReturnType().equals(Void.TYPE);
	}

	private boolean hasMethod(Class<?> type, String name, Class<?>... parameterTypes) {
		try {
			type.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException ex) {
			return false;
		}
		return true;
	}

}
