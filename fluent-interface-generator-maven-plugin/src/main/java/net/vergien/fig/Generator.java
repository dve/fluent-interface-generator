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
	private String prefix;
	private List<String> methodPrefixes;

	public Generator(File targetDir, String abstractPrefix, String prefix, List<String> methodPrefixes) {
		this.targetDir = targetDir;
		this.abstractPrefix = abstractPrefix;
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

		Set<MethodSpec> withMethodSpecs = createMethodSpecs(sourceClass, targetPackage, ignoreMethods);

		Set<MethodSpec> constructorMehtodSpecs = new HashSet<MethodSpec>();
		for (Constructor constructor : sourceClass.getConstructors()) {
			if (java.lang.reflect.Modifier.isPublic(constructor.getModifiers())) {
				constructorMehtodSpecs.add(createConstructorMethodSpec(constructor));
			}
		}
		AnnotationSpec generatedAnnotationSpec = AnnotationSpec.builder(Generated.class)
				.addMember("value", "$S", this.getClass().getName()).build();
		TypeSpec fluentClass = TypeSpec.classBuilder(fluentClassName).addAnnotation(generatedAnnotationSpec)
				.addModifiers(Modifier.ABSTRACT, Modifier.PUBLIC).addMethods(constructorMehtodSpecs)
				.addMethods(withMethodSpecs).superclass(sourceClass).build();

		JavaFile fluentFile = JavaFile.builder(targetPackage, fluentClass).build();

		fluentFile.writeTo(targetDir);
	}

	private Set<MethodSpec> createMethodSpecs(Class<?> sourceClass, String targetPackage, List<String> ignoreMethods) {
		String targetClassName = prefix + sourceClass.getSimpleName();
		Set<MethodSpec> withMethodSpecs = new HashSet<MethodSpec>();

		for (Method sourceMethod : sourceClass.getMethods()) {
			if (!ignoreMethods.contains(sourceMethod.getName())) {
				if (!(sourceMethod.isBridge() || sourceMethod.isSynthetic())) {
					for (String prefix : methodPrefixes) {
						if (sourceMethod.getName().startsWith(prefix)
								&& sourceMethod.getReturnType().equals(Void.TYPE)) {
							withMethodSpecs.add(
									createWithMethodSpec(sourceMethod, targetPackage + "." + targetClassName, prefix));
							break;
						}
					}
				}
			}
		}
		return withMethodSpecs;
	}

	private MethodSpec createConstructorMethodSpec(Constructor constructor) {
		List<ParameterSpec> parameterSpecs = createParameterSpecs(constructor.getParameters());
		List<String> parameterNames = new ArrayList<String>();
		for (ParameterSpec parameterSpec : parameterSpecs) {
			parameterNames.add(parameterSpec.name);
		}
		MethodSpec constructorSpec = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC)
				.addStatement("super(" + StringUtils.join(parameterNames.iterator(), ", ") + ")")
				.addParameters(parameterSpecs).build();
		return constructorSpec;
	}

	private MethodSpec createWithMethodSpec(Method setter, String targetType, String prefix) {
		String methodName = "with" + setter.getName().substring(prefix.length());

		List<ParameterSpec> parameters = createParameterSpecs(setter.getParameters());
		List<String> parameterNames = new ArrayList<String>();
		for (ParameterSpec parameterSpec : parameters) {
			parameterNames.add(parameterSpec.name);
		}
		ClassName bestGuess = ClassName.bestGuess(targetType);
		return MethodSpec.methodBuilder(methodName).addModifiers(Modifier.PUBLIC).addParameters(parameters)
				.addStatement(
						"this." + setter.getName() + "(" + StringUtils.join(parameterNames.iterator(), ", ") + ")")
				.addStatement("return ($T) this", bestGuess).returns(bestGuess).build();
	}

	private List<ParameterSpec> createParameterSpecs(Parameter[] parameters) {
		List<ParameterSpec> specs = new ArrayList<ParameterSpec>();
		for (Parameter parameter : parameters) {
			specs.add(ParameterSpec.builder(parameter.getType(), parameter.getName()).build());
		}
		return specs;
	}

	private boolean isSetter(Method method) {
		return method.getName().startsWith("set") && method.getReturnType().equals(Void.TYPE);
	}

}
