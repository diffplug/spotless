/*
 * Copyright 2016-2026 DiffPlug
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
package com.diffplug.spotless.extra.java;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.jar.JarInputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Executable class for validating or updating embedded JDT lockfiles from Eclipse P2 metadata.
 * <p>
 * Intended for manual execution via the following entrypoints:
 * <pre>
 * - {@link EclipseJdtLockfileMetadataTool.Verify#main(String[])}
 * - {@link EclipseJdtLockfileMetadataTool.Update#main(String[])}
 * </pre>
 */
public class EclipseJdtLockfileMetadataTool {

	// Lockfiles are only provided for Eclipse versions whose org.eclipse.jdt.core POM on Maven Central
	// declares exact transitive dependency versions rather than version ranges (e.g. "[3.12.0,4.0.0)").
	// org.eclipse.jdt.core:3.32.0 is the first version on Maven Central with exact version dependencies;
	// the likely cause is eclipse-platform/eclipse.platform.releng#135 (issue #128), which updated the
	// CBI aggregator configuration to map bundle requirements to their resolved versions rather than the
	// OSGi version ranges from MANIFEST.MF.
	private static final List<String> TARGET_VERSIONS = List.of("4.39", "4.40");

	/**
	 * Verifies the JDT lockfiles at {@link #TARGET_VERSIONS} against Eclipse P2 metadata.
	 */
	public static class Verify {

		public static void main(String[] args) {
			run(false);
		}

	}

	/**
	 * Updates the JDT lockfiles at {@link #TARGET_VERSIONS} from Eclipse P2 metadata.
	 * <p>
	 * Missing lockfiles will be created.
	 */
	public static class Update {

		public static void main(String[] args) {
			run(true);
		}

	}

	private static final String JDT_ID = "org.eclipse.jdt.core";
	private static final String JDT_MAVEN_PREFIX = "org.eclipse.jdt:org.eclipse.jdt.core:";
	private static final Pattern MAJOR_MINOR_PATCH = Pattern.compile("^([0-9]+\\.[0-9]+\\.[0-9]+).*$");
	private static final int MAX_TRAVERSAL = 200;

	private static final HttpClient HTTP = HttpClient.newBuilder()
			.followRedirects(Redirect.NORMAL)
			.build();

	private static void run(boolean update) {
		Path lockfileDir = lockfileDir();
		Map<String, Path> lockfilesByVersion = targetLockfiles(lockfileDir, TARGET_VERSIONS);

		int checked = 0;
		int failures = 0;
		for (Map.Entry<String, Path> entry : lockfilesByVersion.entrySet()) {
			checked++;
			String eclipseVersion = entry.getKey();
			Path lockfilePath = entry.getValue();
			try {
				String bundleVersion = resolveBundleVersion(eclipseVersion);
				String rootCoordinate = JDT_MAVEN_PREFIX + normalizeToMavenVersion(bundleVersion);
				if (update) {
					List<String> allCoordinates = resolveTransitiveClosure(rootCoordinate);
					Files.writeString(lockfilePath, lockfileContent(eclipseVersion, allCoordinates), StandardCharsets.UTF_8);
					System.out.println("WROTE   v" + eclipseVersion + " -> " + allCoordinates.size() + " artifact(s), root: " + rootCoordinate);
				} else {
					if (!Files.exists(lockfilePath)) {
						System.err.println("MISSING v" + eclipseVersion + " -> " + lockfilePath + " (expected root: " + rootCoordinate + ")");
						failures++;
						continue;
					}
					String actualRootCoordinate = lockfileRootCoordinate(lockfilePath);
					if (!rootCoordinate.equals(actualRootCoordinate)) {
						System.err.println("MISMATCH v" + eclipseVersion + " -> expected root: " + rootCoordinate + " actual: " + actualRootCoordinate);
						failures++;
						continue;
					}
					System.out.println("OK      v" + eclipseVersion + " -> " + actualRootCoordinate);
				}
			} catch (Exception e) {
				System.err.println("ERROR   v" + eclipseVersion + " -> " + e.getMessage());
				failures++;
			}
		}
		if (update) {
			System.out.println("Updated " + checked + " lockfile(s).");
		} else {
			System.out.println("Verified " + checked + " lockfile(s) with " + failures + " issue(s).");
		}
	}

	private static Map<String, Path> targetLockfiles(Path lockfileDir, List<String> versions) {
		Map<String, Path> targets = new LinkedHashMap<>();
		for (String version : versions) {
			targets.put(version, lockfileDir.resolve("v" + version + ".lockfile"));
		}
		return targets;
	}

	private static String lockfileContent(String eclipseVersion, List<String> coordinates) {
		String prefix = "# Spotless formatter based on Eclipse-JDT " + eclipseVersion + "\n";
		StringJoiner joiner = new StringJoiner("\n", prefix, "\n");
		coordinates.forEach(joiner::add);
		return joiner.toString();
	}

	private static String lockfileRootCoordinate(Path lockfilePath) throws IOException {
		try (var lines = Files.lines(lockfilePath, StandardCharsets.UTF_8)) {
			return lines.map(String::trim)
					.filter(line -> !line.isEmpty())
					.filter(line -> !line.startsWith("#"))
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException("No dependency coordinate found in " + lockfilePath));
		}
	}

	private static Path lockfileDir() {
		Path fromRepoRoot = Path.of("lib-extra", "src", "main", "resources", "com", "diffplug", "spotless", "extra", "eclipse_jdt_formatter");
		if (Files.isDirectory(fromRepoRoot)) {
			return fromRepoRoot;
		}
		Path fromLibExtra = Path.of("src", "main", "resources", "com", "diffplug", "spotless", "extra", "eclipse_jdt_formatter");
		if (Files.isDirectory(fromLibExtra)) {
			return fromLibExtra;
		}
		throw new IllegalStateException("Unable to locate eclipse_jdt_formatter resource directory");
	}

	/**
	 * Resolves the full Maven transitive closure of the given root coordinate using
	 * Maven Central POM traversal, with highest-version-wins conflict resolution.
	 * <p>
	 * Optional dependencies and test/provided/system-scoped dependencies are excluded.
	 * The root coordinate appears first in the result; remaining entries are sorted.
	 */
	private static List<String> resolveTransitiveClosure(String rootCoordinate) throws Exception {
		// "g:a" -> selected version
		Map<String, String> selected = new LinkedHashMap<>();
		ArrayDeque<String[]> queue = new ArrayDeque<>();
		queue.add(rootCoordinate.split(":"));

		while (!queue.isEmpty()) {
			String[] parts = queue.removeFirst();
			String groupId = parts[0], artifactId = parts[1], version = parts[2];
			String key = groupId + ":" + artifactId;

			String existing = selected.get(key);
			if (existing != null && compareVersions(existing, version) >= 0) {
				continue;
			}
			selected.put(key, version);

			List<String[]> deps = fetchNonOptionalDepsFromPom(groupId, artifactId, version);
			for (String[] dep : deps) {
				String depKey = dep[0] + ":" + dep[1];
				String existingDepVersion = selected.get(depKey);
				if (existingDepVersion == null || compareVersions(existingDepVersion, dep[2]) < 0) {
					queue.add(dep);
				}
			}
		}

		String[] rootParts = rootCoordinate.split(":");
		String rootKey = rootParts[0] + ":" + rootParts[1];
		List<String> result = new ArrayList<>();
		result.add(rootCoordinate);
		selected.entrySet().stream()
				.filter(e -> !e.getKey().equals(rootKey))
				.map(e -> e.getKey() + ":" + e.getValue())
				.sorted()
				.forEach(result::add);
		return result;
	}

	private static List<String[]> fetchNonOptionalDepsFromPom(String groupId, String artifactId, String version) throws Exception {
		String path = groupId.replace('.', '/') + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".pom";
		Optional<byte[]> bytes = download("https://repo1.maven.org/maven2/" + path);
		if (bytes.isEmpty()) {
			System.err.println("WARNING: POM not found on Maven Central: " + groupId + ":" + artifactId + ":" + version);
			return List.of();
		}
		return parseNonOptionalDepsFromPom(new String(bytes.get(), StandardCharsets.UTF_8));
	}

	private static List<String[]> parseNonOptionalDepsFromPom(String pomXml) throws Exception {
		Document doc = parseXml(pomXml);
		var xpath = XPathFactory.newInstance().newXPath();
		NodeList deps = (NodeList) xpath.evaluate("/project/dependencies/dependency", doc, XPathConstants.NODESET);
		List<String[]> result = new ArrayList<>();
		for (int i = 0; i < deps.getLength(); i++) {
			Element dep = (Element) deps.item(i);
			String g = textContent(dep, "groupId");
			String a = textContent(dep, "artifactId");
			String v = textContent(dep, "version");
			String scope = textContent(dep, "scope");
			String optional = textContent(dep, "optional");
			String type = textContent(dep, "type");

			if ("true".equals(optional))
				continue;
			if ("test".equals(scope) || "provided".equals(scope) || "system".equals(scope))
				continue;
			if ("pom".equals(type))
				continue;
			if (v.isEmpty() || v.startsWith("[") || v.startsWith("(") || v.startsWith("$"))
				continue;
			if (g.isEmpty() || a.isEmpty())
				continue;

			result.add(new String[]{g, a, v});
		}
		return result;
	}

	private static String textContent(Element parent, String tag) {
		var nodes = parent.getElementsByTagName(tag);
		return nodes.getLength() > 0 ? nodes.item(0).getTextContent().trim() : "";
	}

	/** Compares two version strings numerically, segment by segment. Returns positive if v1 &gt; v2. */
	private static int compareVersions(String v1, String v2) {
		String[] p1 = v1.split("\\.");
		String[] p2 = v2.split("\\.");
		int len = Math.max(p1.length, p2.length);
		for (int i = 0; i < len; i++) {
			int n1 = i < p1.length ? parseVersionSegment(p1[i]) : 0;
			int n2 = i < p2.length ? parseVersionSegment(p2[i]) : 0;
			if (n1 != n2)
				return Integer.compare(n1, n2);
		}
		return 0;
	}

	private static int parseVersionSegment(String segment) {
		try {
			return Integer.parseInt(segment);
		} catch (NumberFormatException e) {
			return 0;
		}
	}

	private static String resolveBundleVersion(String eclipseVersion) throws Exception {
		ArrayDeque<String> queue = new ArrayDeque<>();
		Set<String> visited = new LinkedHashSet<>();
		queue.add("https://download.eclipse.org/eclipse/updates/" + eclipseVersion + "/");
		int traversed = 0;
		while (!queue.isEmpty()) {
			String repoUrl = queue.removeFirst();
			if (!visited.add(repoUrl)) {
				continue;
			}
			traversed++;
			if (traversed > MAX_TRAVERSAL) {
				throw new IllegalStateException("Traversal exceeded " + MAX_TRAVERSAL + " repositories while resolving Eclipse " + eclipseVersion);
			}
			Optional<String> contentXml = readJarEntry(repoUrl, "content.jar", "content.xml");
			if (contentXml.isPresent()) {
				String bundleVersion = extractBundleVersion(contentXml.get());
				if (bundleVersion != null) {
					return bundleVersion;
				}
			}
			Optional<String> compositeXml = readJarEntry(repoUrl, "compositeContent.jar", "compositeContent.xml");
			if (compositeXml.isPresent()) {
				queue.addAll(compositeChildren(repoUrl, compositeXml.get(), visited));
			}
		}
		throw new IllegalStateException("Unable to resolve " + JDT_ID + " from Eclipse " + eclipseVersion + " update site");
	}

	private static Optional<String> readJarEntry(String repoUrl, String jarName, String entryName) throws Exception {
		Optional<byte[]> bytes = download(repoUrl + jarName);
		if (bytes.isEmpty()) {
			return Optional.empty();
		}
		try (JarInputStream jarInputStream = new JarInputStream(new ByteArrayInputStream(bytes.get()))) {
			var entry = jarInputStream.getNextJarEntry();
			while (entry != null) {
				if (!entry.isDirectory() && entryName.equals(entry.getName())) {
					return Optional.of(new String(jarInputStream.readAllBytes(), StandardCharsets.UTF_8));
				}
				entry = jarInputStream.getNextJarEntry();
			}
		}
		throw new IllegalStateException("Entry " + entryName + " not found in " + repoUrl + jarName);
	}

	private static Optional<byte[]> download(String url) throws Exception {
		HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
		HttpResponse<byte[]> response = HTTP.send(request, HttpResponse.BodyHandlers.ofByteArray());
		if (response.statusCode() == 200) {
			return Optional.of(response.body());
		}
		if (response.statusCode() == 404) {
			return Optional.empty();
		}
		throw new IOException("Unexpected HTTP status " + response.statusCode() + " from " + url);
	}

	private static String extractBundleVersion(String contentXml) throws Exception {
		Document document = parseXml(contentXml);
		var xpath = XPathFactory.newInstance().newXPath();
		String value = (String) xpath.evaluate("//unit[@id='" + JDT_ID + "']/@version", document, XPathConstants.STRING);
		return value.isBlank() ? null : value;
	}

	private static List<String> compositeChildren(String parentRepoUrl, String compositeXml, Set<String> visited) throws Exception {
		Document document = parseXml(compositeXml);
		var xpath = XPathFactory.newInstance().newXPath();
		var nodes = (NodeList) xpath.evaluate("//child/@location", document, XPathConstants.NODESET);
		List<String> children = new ArrayList<>(nodes.getLength());
		for (int i = 0; i < nodes.getLength(); i++) {
			String location = nodes.item(i).getNodeValue();
			String childUrl = parentRepoUrl + trimTrailingSlash(location) + "/";
			if (!visited.contains(childUrl)) {
				children.add(childUrl);
			}
		}
		return children;
	}

	private static String trimTrailingSlash(String value) {
		return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
	}

	private static Document parseXml(String xml) throws Exception {
		var factory = DocumentBuilderFactory.newInstance();
		// Disable DTDs/external entities and enable secure processing to prevent XXE
		factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		factory.setFeature("http://xml.org/sax/features/external-general-entities", false);
		factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
		factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
		factory.setExpandEntityReferences(false);
		factory.setNamespaceAware(false);
		var builder = factory.newDocumentBuilder();
		return builder.parse(new InputSource(new StringReader(xml)));
	}

	private static String normalizeToMavenVersion(String bundleVersion) {
		Matcher matcher = MAJOR_MINOR_PATCH.matcher(bundleVersion);
		if (!matcher.matches()) {
			throw new IllegalArgumentException("Unexpected org.eclipse.jdt.core bundle version: " + bundleVersion);
		}
		return matcher.group(1);
	}

}
