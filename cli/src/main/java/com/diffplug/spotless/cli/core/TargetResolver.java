/*
 * Copyright 2024 DiffPlug
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
package com.diffplug.spotless.cli.core;

import static java.util.function.Predicate.not;

import java.io.File;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitOption;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.Nonnull;

import com.diffplug.spotless.ThrowingEx;

public class TargetResolver {

	private final List<String> targets;

	private final FileResolver fileResolver;

	public TargetResolver(@Nonnull Path baseDir, @Nonnull List<String> targets) {
		this.fileResolver = new FileResolver(baseDir);
		this.targets = Objects.requireNonNull(targets);
	}

	public Stream<Path> resolveTargets() {
		return targets.parallelStream()
				.map(this::resolveTarget)
				.reduce(Stream::concat) // beware! when using flatmap, the stream goes to sequential
				.orElse(Stream.empty());
	}

	private Stream<Path> resolveTarget(String target) {

		final boolean isGlob = target.contains("*") || target.contains("?");
		System.out.println("isGlob: " + isGlob + " target: " + target);

		if (isGlob) {
			return resolveGlob(target);
		}
		Path targetPath = fileResolver.resolvePath(Path.of(target));
		if (Files.isReadable(targetPath)) {
			return Stream.of(targetPath);
		}
		if (Files.isDirectory(targetPath)) {
			return resolveDir(targetPath);
		}
		// TODO log warn?
		return Stream.empty();
	}

	private Stream<Path> resolveDir(Path startDir) {
		List<Path> collected = new ArrayList<>();
		ThrowingEx.run(() -> Files.walkFileTree(startDir,
				EnumSet.of(FileVisitOption.FOLLOW_LINKS),
				Integer.MAX_VALUE,
				new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						collected.add(file);
						return FileVisitResult.CONTINUE;
					}
				}));
		return collected.parallelStream();
	}

	private Stream<Path> resolveGlob(String glob) {
		Path startDir;
		String globPart;
		// if the glob is absolute, we need to split the glob into its parts and use all parts except glob chars '*', '**', and '?'
		String[] parts = glob.split("\\Q" + File.separator + "\\E");
		List<String> startDirParts = Stream.of(parts)
				.takeWhile(not(TargetResolver::isGlobPathPart))
				.collect(Collectors.toList());

		startDir = Path.of(glob.startsWith(File.separator) ? File.separator : fileResolver.baseDir().toString(), startDirParts.toArray(String[]::new));
		globPart = Stream.of(parts)
				.skip(startDirParts.size())
				.collect(Collectors.joining(File.separator));

		PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:" + globPart);
		List<Path> collected = new ArrayList<>();
		ThrowingEx.run(() -> Files.walkFileTree(startDir,
				EnumSet.of(FileVisitOption.FOLLOW_LINKS),
				Integer.MAX_VALUE,
				new SimpleFileVisitor<>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						Path relativeFile = startDir.relativize(file);
						if (matcher.matches(relativeFile)) {
							System.out.println("Matched: " + file);
							collected.add(file);
						}
						return FileVisitResult.CONTINUE;
					}
				}));
		return collected.parallelStream()
				.map(Path::normalize);
		//				.map(Path::toAbsolutePath);
	}

	private static boolean isGlobPathPart(String part) {
		return part.contains("*") || part.contains("?") || part.matches(".*\\[.*].*") || part.matches(".*\\{.*}.*");
	}
}
