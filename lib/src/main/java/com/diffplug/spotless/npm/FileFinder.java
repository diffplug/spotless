/*
 * Copyright 2020-2023 DiffPlug
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
package com.diffplug.spotless.npm;

import static java.util.Objects.requireNonNull;

import java.io.File;
import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

class FileFinder {

	private final List<Supplier<Optional<File>>> fileCandidateFinders;

	private FileFinder(Builder builder) {
		this.fileCandidateFinders = List.copyOf(builder.candidateFinders);
	}

	static Builder finderForFilename(String fileName) {
		return new Builder(fileName, null);
	}

	static Builder finderForExecutableFilename(String fileName) {
		return new Builder(fileName, true);
	}

	Optional<File> tryFind() {
		return fileCandidateFinders
				.stream()
				.map(Supplier::get)
				.filter(Optional::isPresent)
				.map(Optional::get)
				.findFirst();
	}

	static class Builder {

		private final String fileName;

		private final Boolean executable;

		private final List<Supplier<Optional<File>>> candidateFinders = new ArrayList<>();

		Builder(String fileName, Boolean executable) {
			this.fileName = requireNonNull(fileName);
			this.executable = executable;
		}

		public Builder candidateEnvironmentPath(String environmentVar) {
			candidateFinders.add(new CandidateOnSinglePathEnvironmentVar(environmentVar, fileName, FileIsExecutableFilter.executable(this.executable)));
			return this;
		}

		public Builder candidateEnvironmentPathList(String environmentVar, Function<File, File> fileTransformer) {
			candidateFinders.add(new CandidateOnPathListEnvironmentVar(environmentVar, fileName, fileTransformer, FileIsExecutableFilter.executable(this.executable)));
			return this;
		}

		public Builder candidateSystemProperty(String systemProperty) {
			candidateFinders.add(new CandidateOnSystemPropertyVar(systemProperty, FileIsExecutableFilter.executable(this.executable)));
			return this;
		}

		public Builder candidateFileInFolder(File folder) {
			candidateFinders.add(new CandidateInFolder(folder, fileName, FileIsExecutableFilter.executable(this.executable)));
			return this;
		}

		public FileFinder build() {
			return new FileFinder(this);
		}
	}

	private static class FileIsExecutableFilter implements Predicate<File> {
		@Override
		public boolean test(File file) {
			return file.canExecute();
		}

		static Predicate<File> executable() {
			return new FileIsExecutableFilter();
		}

		static Predicate<File> executable(Boolean executable) {
			if (executable == null) {
				return AnyFileFilter.any();
			}
			if (executable) {
				return executable();
			}
			// !executable
			return executable().negate();
		}
	}

	private static class AnyFileFilter implements Predicate<File> {

		@Override
		public boolean test(File file) {
			return true;
		}

		static AnyFileFilter any() {
			return new AnyFileFilter();
		}
	}

	private static class CandidateOnSinglePathEnvironmentVar implements Supplier<Optional<File>> {
		private final String environmentVar;
		private final String fileName;
		private final Predicate<File> additionalFilters;

		public CandidateOnSinglePathEnvironmentVar(String environmentVar, String fileName, Predicate<File> additionalFilter) {
			this.environmentVar = environmentVar;
			this.fileName = fileName;
			this.additionalFilters = additionalFilter == null ? AnyFileFilter.any() : additionalFilter;
		}

		@Override
		public Optional<File> get() {
			return Optional.ofNullable(environmentVar)
					.map(File::new)
					.map(file -> new File(file, fileName))
					.filter(File::exists)
					.filter(additionalFilters);
		}
	}

	private static class CandidateOnPathListEnvironmentVar implements Supplier<Optional<File>> {
		private final String environmentVar;
		private final String fileName;
		private final Function<File, File> fileTransformer;
		private final Predicate<File> additionalFilter;

		public CandidateOnPathListEnvironmentVar(String environmentVar, String fileName, Function<File, File> fileTransformer, Predicate<File> additionalFilter) {
			this.environmentVar = environmentVar;
			this.fileName = fileName;
			this.fileTransformer = fileTransformer;
			this.additionalFilter = additionalFilter;
		}

		@Override
		public Optional<File> get() {
			String pathList = System.getenv(environmentVar);
			if (pathList != null) {
				return Arrays.stream(pathList.split(System.getProperty("path.separator", ":")))
						.map(File::new)
						.filter(File::exists)
						.map(fileTransformer)
						.map(dir -> new File(dir, fileName))
						.filter(File::exists)
						.filter(additionalFilter)
						.findFirst();
			}
			return Optional.empty();
		}
	}

	private static class CandidateOnSystemPropertyVar implements Supplier<Optional<File>> {
		private final String systemProperty;
		private final Predicate<File> additionalFilter;

		public CandidateOnSystemPropertyVar(String systemProperty, Predicate<File> additionalFilter) {
			this.systemProperty = systemProperty;
			this.additionalFilter = additionalFilter == null ? AnyFileFilter.any() : additionalFilter;

		}

		@Override
		public Optional<File> get() {
			return Optional.ofNullable(System.getProperty(this.systemProperty))
					.map(File::new)
					.filter(File::exists)
					.filter(additionalFilter);
		}
	}

	private static class CandidateInFolder implements Supplier<Optional<File>> {

		private final File folder;
		private final String fileName;
		private final Predicate<File> additionalFilter;

		public CandidateInFolder(File folder, String fileName, Predicate<File> additionalFilter) {
			this.folder = folder;
			this.fileName = fileName;
			this.additionalFilter = additionalFilter == null ? AnyFileFilter.any() : additionalFilter;
		}

		@Override
		public Optional<File> get() {
			return Optional.of(folder)
					.filter(File::exists)
					.filter(File::isDirectory)
					.map(folder -> new File(folder, fileName))
					.filter(File::exists)
					.filter(additionalFilter);
		}
	}
}
