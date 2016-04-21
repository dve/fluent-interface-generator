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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.Generated;
import javax.lang.model.element.Modifier;

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

	public Generator(File targetDir, String abstractPrefix, String interfacePrefix, String prefix, List<String> methodPrefixes) {
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

	public void createFluentFor(Class<?> sourceClass, String targetPackage, List<String> ignoreMethods)
			throws ClassNotFoundException, IOException {
		String fluentClassName = abstractPrefix + sourceClass.getSimpleName();
		
		Class<?> interfaceClass = createInterfaceClass(sourceClass, targetPackage);

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
		String interfaceClassName = interfacePrefix + sourceClass.getSimpleName();
		Class<?> interfaceClass;
		try {
			System.out.println(targetPackage + "." + interfaceClassName);
			interfaceClass = Class.forName(targetPackage + "." + interfaceClassName);
		} catch (ClassNotFoundException ex) {
			System.out.println("ClassNotFoundException ex");
			return null;
		}
		if (!interfaceClass.isInterface()) {
			System.out.println("!interfaceClass.isInterface()");
			return null;
		}
		System.out.println(interfaceClass);
		return interfaceClass;
	}

	private Set<MethodSpec> createMethodSpecs(Class<?> sourceClass, String targetPackage, List<String> ignoreMethods, Class<?> interfaceClass) {
		String targetClassName = prefix + sourceClass.getSimpleName();
		Set<MethodSpec> withMethodSpecs = new HashSet<>();

		for (Method sourceMethod : sourceClass.getMethods()) {

			if (!ignoreMethods.contains(sourceMethod.getName())) {
				if (!(sourceMethod.isBridge() || sourceMethod.isSynthetic()
						|| sourceMethod.isAnnotationPresent(Deprecated.class))) {
					for (String methodPrefix : methodPrefixes) {
						if (sourceMethod.getName().startsWith(methodPrefix)
								&& sourceMethod.getReturnType().equals(Void.TYPE)) {
							withMethodSpecs.add(
									createWithMethodSpec(sourceMethod, targetPackage + "." + targetClassName, methodPrefix, sourceClass, interfaceClass));
							break;
						}
					}
				}
			}

		}
		return withMethodSpecs;
	}

	private MethodSpec createConstructorMethodSpec(Constructor<?> constructor) {
		List<ParameterSpec> parameterSpecs = createParameterSpecs(constructor.getParameters());
		List<String> parameterNames = new ArrayList<>();
		for (ParameterSpec parameterSpec : parameterSpecs) {
			parameterNames.add(parameterSpec.name);
		}
		MethodSpec constructorSpec = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addStatement("super(" + StringUtils.join(parameterNames.iterator(), ", ") + ")")
				.addParameters(parameterSpecs).build();
		return constructorSpec;
	}

	private MethodSpec createWithMethodSpec(Method setter, String targetType, String prefix, Class<?> sourceClass, Class<?> interfaceClass) {
		String methodName = "with" + setter.getName().substring(prefix.length());

		List<ParameterSpec> parameters = createParameterSpecs(setter.getParameters());
		List<String> parameterNames = new ArrayList<>();
		for (ParameterSpec parameterSpec : parameters) {
			parameterNames.add(parameterSpec.name);
		}
		ClassName bestGuess = ClassName.bestGuess(targetType);
		
		MethodSpec.Builder builder = MethodSpec.methodBuilder(methodName)
				.addModifiers(Modifier.PUBLIC).addParameters(parameters)
				.addStatement("this." + setter.getName() + "(" + StringUtils.join(parameterNames.iterator(), ", ") + ")")
				.addStatement("return ($T) this", bestGuess)
				.returns(bestGuess);
		
		if (hasMethod(sourceClass, methodName, setter.getParameterTypes())) {
			builder.addAnnotation(Override.class);
		} else if (interfaceClass != null) {
			if (hasMethod(interfaceClass, methodName, setter.getParameterTypes())) {
				builder.addAnnotation(Override.class);
			}
		}
		
		return builder.build();
	}

	private List<ParameterSpec> createParameterSpecs(Parameter[] parameters) {
		List<ParameterSpec> specs = new ArrayList<>();
		for (Parameter parameter : parameters) {
			specs.add(ParameterSpec.builder(parameter.getType(), parameter.getName()).build());
		}
		return specs;
	}

	private boolean isSetter(Method method) {
		return method.getName().startsWith("set") && method.getReturnType().equals(Void.TYPE);
	}
	
	private boolean hasMethod(Class<?> type, String name, Class<?>...parameterTypes) {
		try {
			type.getMethod(name, parameterTypes);
		} catch (NoSuchMethodException ex) {
			return false;
		}
		return true;
	}

}
