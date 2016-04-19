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
package net.vergien.fig.mojo;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;

import net.vergien.fig.Generator;

/**
 * Goal which touches a timestamp file.
 * 
 */
@Mojo(name = "touch", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class FIGMojo extends AbstractMojo {
	/**
	 * Location of the file.
	 */
	@Parameter(defaultValue = "${project.build.directory}", property = "outputDir", required = true)
	private File outputDirectory;

	@Parameter(defaultValue = "${project}", required = true, readonly = true)
	private MavenProject project;

	/**
	 * Full qualified class names, of the classes for which a fluent interface
	 * version should be created.
	 */
	@Parameter(required = true)
	private List<PkgConf> mapping;

	/**
	 * The prefix for the abstract generated interim classes. Defaults to "A"
	 */
	@Parameter(defaultValue = "A", required = true)
	private String abstractPrefix;
	/**
	 * The prefix for the classes. Defaults to "F"
	 */
	@Parameter(defaultValue = "F", required = true)
	private String prefix;

	public void execute() throws MojoExecutionException {

		File targetDir = new File(outputDirectory, "generated-sources/fluent-interface-generator-maven-plugin");
		project.addCompileSourceRoot(targetDir.getAbsolutePath());
		if (!targetDir.exists()) {
			targetDir.mkdirs();
		}

		getLog().info("Prefix for generated abstract classes: " + abstractPrefix);
		getLog().info("Prefix for classes: " + prefix);
		Generator generator = new Generator(targetDir, abstractPrefix, prefix);
		for (PkgConf targetPackage : mapping) {
			for (String className : targetPackage.getClassNames()) {
				getLog().info("Create class for: " + className);
				try {
					Class<?> sourceClass = Thread.currentThread().getContextClassLoader().loadClass(className);
					generator.createFluentFor(sourceClass, targetPackage.getPkgName());
				} catch (ClassNotFoundException e) {
					throw new MojoExecutionException("Error creating file " + className, e);
				} catch (IOException e) {
					throw new MojoExecutionException("Error creating file " + className, e);
				}
			}
		}
	}
}
