/*
 * Copyright 2016-2025 DiffPlug
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.diffplug.gradle.spotless;

import static com.diffplug.gradle.spotless.PluginGradlePreconditions.requireElementsNonNull;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;

import org.gradle.api.Project;
import org.gradle.api.tasks.SourceSet;

import com.diffplug.spotless.FormatterStep;
import com.diffplug.spotless.extra.java.EclipseJdtFormatterStep;
import com.diffplug.spotless.generic.LicenseHeaderStep;
import com.diffplug.spotless.java.CleanthatJavaStep;
import com.diffplug.spotless.java.FormatAnnotationsStep;
import com.diffplug.spotless.java.GoogleJavaFormatStep;
import com.diffplug.spotless.java.ImportOrderStep;
import com.diffplug.spotless.java.PalantirJavaFormatStep;
import com.diffplug.spotless.java.RemoveUnusedImportsStep;
import com.diffplug.spotless.java.RemoveWildcardImportsStep;

public class JavaExtension extends FormatExtension implements HasBuiltinDelimiterForLicense, JvmLang {
	static final String NAME = "java";

	@Inject
	public JavaExtension(SpotlessExtension spotless) {
		super(spotless);
	}

	static final String LICENSE_HEADER_DELIMITER = LicenseHeaderStep.DEFAULT_JAVA_HEADER_DELIMITER;

	@Override
	public LicenseHeaderConfig licenseHeader(String licenseHeader) {
		return licenseHeader(licenseHeader, LICENSE_HEADER_DELIMITER);
	}

	@Override
	public LicenseHeaderConfig licenseHeaderFile(Object licenseHeaderFile) {
		return licenseHeaderFile(licenseHeaderFile, LICENSE_HEADER_DELIMITER);
	}

	public ImportOrderConfig importOrder(String... importOrder) {
		return new ImportOrderConfig(importOrder);
	}

	public ImportOrderConfig importOrderFile(Object importOrderFile) {
		Objects.requireNonNull(importOrderFile);
		return new ImportOrderConfig(getProject().file(importOrderFile));
	}

	public class ImportOrderConfig {
		final String[] importOrder;
		final File importOrderFile;

		boolean wildcardsLast = false;
		boolean semanticSort = false;
		Set<String> treatAsPackage = Set.of();
		Set<String> treatAsClass = Set.of();

		ImportOrderConfig(String[] importOrder) {
			this.importOrder = importOrder;
			importOrderFile = null;
			addStep(createStep());
		}

		ImportOrderConfig(File importOrderFile) {
			importOrder = null;
			this.importOrderFile = importOrderFile;
			addStep(createStep());
		}

		/** Sorts wildcard imports after non-wildcard imports, instead of before. */
		public ImportOrderConfig wildcardsLast() {
			return wildcardsLast(true);
		}

		public ImportOrderConfig wildcardsLast(boolean wildcardsLast) {
			this.wildcardsLast = wildcardsLast;
			replaceStep(createStep());
			return this;
		}

		public ImportOrderConfig semanticSort() {
			return semanticSort(true);
		}

		public ImportOrderConfig semanticSort(boolean semanticSort) {
			this.semanticSort = semanticSort;
			replaceStep(createStep());
			return this;
		}

		public ImportOrderConfig treatAsPackage(String... treatAsPackage) {
			return treatAsPackage(Arrays.asList(treatAsPackage));
		}

		public ImportOrderConfig treatAsPackage(Collection<String> treatAsPackage) {
			this.treatAsPackage = new HashSet<>(treatAsPackage);
			replaceStep(createStep());
			return this;
		}

		public ImportOrderConfig treatAsClass(String... treatAsClass) {
			return treatAsClass(Arrays.asList(treatAsClass));
		}

		public ImportOrderConfig treatAsClass(Collection<String> treatAsClass) {
			this.treatAsClass = new HashSet<>(treatAsClass);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			ImportOrderStep importOrderStep = ImportOrderStep.forJava();

			return importOrderFile != null
					? importOrderStep.createFrom(wildcardsLast, semanticSort, treatAsPackage, treatAsClass, getProject().file(importOrderFile))
					: importOrderStep.createFrom(wildcardsLast, semanticSort, treatAsPackage, treatAsClass, importOrder);
		}
	}

	/** Removes any unused imports. */
	public void removeUnusedImports() {
		addStep(RemoveUnusedImportsStep.create(RemoveUnusedImportsStep.defaultFormatter(), provisioner()));
	}

	public void removeUnusedImports(String formatter) {
		addStep(RemoveUnusedImportsStep.create(formatter, provisioner()));
	}

	public void removeWildcardImports() {
		addStep(RemoveWildcardImportsStep.create());
	}

	/** Uses the <a href="https://github.com/google/google-java-format">google-java-format</a> jar to format source code. */
	public GoogleJavaFormatConfig googleJavaFormat() {
		return googleJavaFormat(GoogleJavaFormatStep.defaultVersion());
	}

	/**
	 * Uses the given version of <a href="https://github.com/google/google-java-format">google-java-format</a> to format source code.
	 * <p>
	 * Limited to published versions.  See <a href="https://github.com/diffplug/spotless/issues/33#issuecomment-252315095">issue #33</a>
	 * for a workaround for using snapshot versions.
	 */
	public GoogleJavaFormatConfig googleJavaFormat(String version) {
		Objects.requireNonNull(version);
		return new GoogleJavaFormatConfig(version);
	}

	public class GoogleJavaFormatConfig {
		final String version;
		String groupArtifact;
		String style;
		boolean reflowLongStrings;
		boolean reorderImports;
		boolean formatJavadoc = true;

		GoogleJavaFormatConfig(String version) {
			this.version = Objects.requireNonNull(version);
			this.groupArtifact = GoogleJavaFormatStep.defaultGroupArtifact();
			this.style = GoogleJavaFormatStep.defaultStyle();
			addStep(createStep());
		}

		public GoogleJavaFormatConfig groupArtifact(String groupArtifact) {
			this.groupArtifact = Objects.requireNonNull(groupArtifact);
			replaceStep(createStep());
			return this;
		}

		public GoogleJavaFormatConfig style(String style) {
			this.style = Objects.requireNonNull(style);
			replaceStep(createStep());
			return this;
		}

		public GoogleJavaFormatConfig aosp() {
			return style("AOSP");
		}

		public GoogleJavaFormatConfig reflowLongStrings() {
			return reflowLongStrings(true);
		}

		public GoogleJavaFormatConfig reflowLongStrings(boolean reflowLongStrings) {
			this.reflowLongStrings = reflowLongStrings;
			replaceStep(createStep());
			return this;
		}

		public GoogleJavaFormatConfig reorderImports(boolean reorderImports) {
			this.reorderImports = reorderImports;
			replaceStep(createStep());
			return this;
		}

		public GoogleJavaFormatConfig skipJavadocFormatting() {
			return formatJavadoc(false);
		}

		public GoogleJavaFormatConfig formatJavadoc(boolean formatJavadoc) {
			this.formatJavadoc = formatJavadoc;
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return GoogleJavaFormatStep.create(
					groupArtifact,
					version,
					style,
					provisioner(),
					reflowLongStrings,
					reorderImports,
					formatJavadoc);
		}
	}

	/** Uses the <a href="https://github.com/palantir/palantir-java-format">palantir-java-format</a> jar to format source code. */
	public PalantirJavaFormatConfig palantirJavaFormat() {
		return palantirJavaFormat(PalantirJavaFormatStep.defaultVersion());
	}

	/**
	 * Uses the given version of <a href="https://github.com/palantir/palantir-java-format">palantir-java-format</a> to format source code.
	 * <p>
	 * Limited to published versions.  See <a href="https://github.com/diffplug/spotless/issues/33#issuecomment-252315095">issue #33</a>
	 * for a workaround for using snapshot versions.
	 */
	public PalantirJavaFormatConfig palantirJavaFormat(String version) {
		Objects.requireNonNull(version);
		return new PalantirJavaFormatConfig(version);
	}

	public class PalantirJavaFormatConfig {
		final String version;
		String style;
		boolean formatJavadoc;

		PalantirJavaFormatConfig(String version) {
			this.version = Objects.requireNonNull(version);
			this.style = PalantirJavaFormatStep.defaultStyle();
			addStep(createStep());
		}

		public PalantirJavaFormatConfig style(String style) {
			this.style = Objects.requireNonNull(style);
			replaceStep(createStep());
			return this;
		}

		public PalantirJavaFormatConfig formatJavadoc(boolean formatJavadoc) {
			this.formatJavadoc = formatJavadoc;
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return PalantirJavaFormatStep.create(version, style, formatJavadoc, provisioner());
		}
	}

	public EclipseConfig eclipse() {
		return eclipse(EclipseJdtFormatterStep.defaultVersion());
	}

	public EclipseConfig eclipse(String version) {
		return new EclipseConfig(version);
	}

	public class EclipseConfig {
		private final EclipseJdtFormatterStep.Builder builder;

		EclipseConfig(String version) {
			builder = EclipseJdtFormatterStep.createBuilder(provisioner());
			builder.setVersion(version);
			addStep(builder.build());
		}

		public EclipseConfig configFile(Object... configFiles) {
			requireElementsNonNull(configFiles);
			Project project = getProject();
			builder.setPreferences(project.files(configFiles).getFiles());
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig configProperties(String... configs) {
			requireElementsNonNull(configs);
			builder.setPropertyPreferences(List.of(configs));
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig sortMembersDoNotSortFields(boolean doNotSortFields) {
			builder.sortMembersDoNotSortFields(doNotSortFields);
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig sortMembersEnabled(boolean enabled) {
			builder.sortMembersEnabled(enabled);
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig sortMembersOrder(String order) {
			requireElementsNonNull(order);
			builder.sortMembersOrder(order);
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig sortMembersVisibilityOrder(String order) {
			requireElementsNonNull(order);
			builder.sortMembersVisibilityOrder(order);
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig sortMembersVisibilityOrderEnabled(boolean enabled) {
			builder.sortMembersVisibilityOrderEnabled(enabled);
			replaceStep(builder.build());
			return this;
		}

		public EclipseConfig withP2Mirrors(Map<String, String> mirrors) {
			builder.setP2Mirrors(mirrors);
			replaceStep(builder.build());
			return this;
		}

	}

	/** Removes newlines between type annotations and types. */
	public FormatAnnotationsConfig formatAnnotations() {
		return new FormatAnnotationsConfig();
	}

	public class FormatAnnotationsConfig {
		/** Annotations in addition to those in the default list. */
		final List<String> addedTypeAnnotations = new ArrayList<>();
		/** Annotations that the user doesn't want treated as type annotations. */
		final List<String> removedTypeAnnotations = new ArrayList<>();

		FormatAnnotationsConfig() {
			addStep(createStep());
		}

		public FormatAnnotationsConfig addTypeAnnotation(String simpleName) {
			Objects.requireNonNull(simpleName);
			addedTypeAnnotations.add(simpleName);
			replaceStep(createStep());
			return this;
		}

		public FormatAnnotationsConfig removeTypeAnnotation(String simpleName) {
			Objects.requireNonNull(simpleName);
			removedTypeAnnotations.add(simpleName);
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return FormatAnnotationsStep.create(
					addedTypeAnnotations,
					removedTypeAnnotations);
		}
	}

	/** Apply CleanThat refactoring rules. */
	public CleanthatJavaConfig cleanthat() {
		return new CleanthatJavaConfig();
	}

	public class CleanthatJavaConfig {
		private String groupArtifact = CleanthatJavaStep.defaultGroupArtifact();

		private String version = CleanthatJavaStep.defaultVersion();

		private String sourceJdk = CleanthatJavaStep.defaultSourceJdk();

		private List<String> mutators = new ArrayList<>(CleanthatJavaStep.defaultMutators());

		private List<String> excludedMutators = new ArrayList<>(CleanthatJavaStep.defaultExcludedMutators());

		private boolean includeDraft = CleanthatJavaStep.defaultIncludeDraft();

		CleanthatJavaConfig() {
			addStep(createStep());
		}

		public CleanthatJavaConfig groupArtifact(String groupArtifact) {
			Objects.requireNonNull(groupArtifact);
			this.groupArtifact = groupArtifact;
			replaceStep(createStep());
			return this;
		}

		public CleanthatJavaConfig version(String version) {
			Objects.requireNonNull(version);
			this.version = version;
			replaceStep(createStep());
			return this;
		}

		public CleanthatJavaConfig sourceCompatibility(String jdkVersion) {
			Objects.requireNonNull(jdkVersion);
			this.sourceJdk = jdkVersion;
			replaceStep(createStep());
			return this;
		}

		// Especially useful to clear default mutators
		public CleanthatJavaConfig clearMutators() {
			this.mutators.clear();
			replaceStep(createStep());
			return this;
		}

		// An id of a mutator (see IMutator.getIds()) or
		// tThe fully qualified name of a class implementing eu.solven.cleanthat.engine.java.refactorer.meta.IMutator
		public CleanthatJavaConfig addMutator(String mutator) {
			this.mutators.add(mutator);
			replaceStep(createStep());
			return this;
		}

		public CleanthatJavaConfig addMutators(Collection<String> mutators) {
			this.mutators.addAll(mutators);
			replaceStep(createStep());
			return this;
		}

		// useful to exclude a mutator amongst the default list of mutators
		public CleanthatJavaConfig excludeMutator(String mutator) {
			this.excludedMutators.add(mutator);
			replaceStep(createStep());
			return this;
		}

		public CleanthatJavaConfig includeDraft(boolean includeDraft) {
			this.includeDraft = includeDraft;
			replaceStep(createStep());
			return this;
		}

		private FormatterStep createStep() {
			return CleanthatJavaStep.create(
					groupArtifact,
					version,
					sourceJdk, mutators, excludedMutators, includeDraft, provisioner());
		}
	}

	/** If the user hasn't specified the files yet, we'll assume he/she means all of the java files. */
	@Override
	protected void setupTask(SpotlessTask task) {
		if (target == null) {
			target = getSources(getProject(),
					"You must either specify 'target' manually or apply the 'java' plugin.",
					SourceSet::getAllJava,
					file -> file.getName().endsWith(".java"));
		}

		steps.replaceAll(step -> {
			if (isLicenseHeaderStep(step)) {
				return step.filterByFile(LicenseHeaderStep.unsupportedJvmFilesFilter());
			} else {
				return step;
			}
		});
		super.setupTask(task);
	}
}
