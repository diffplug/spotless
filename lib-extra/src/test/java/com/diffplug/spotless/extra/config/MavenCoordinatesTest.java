/*
 * Copyright 2016 DiffPlug
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
package com.diffplug.spotless.extra.config;

import static com.diffplug.spotless.extra.config.MavenCoordinates.Coordinate;
import static org.assertj.core.api.Assertions.*;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.diffplug.spotless.ResourceHarness;

public class MavenCoordinatesTest extends ResourceHarness {
	private static final Coordinate C1 = new Coordinate("a:b:c:d:e");
	private static final Coordinate C2 = new Coordinate("i:j:k:l");
	private static final Coordinate C3 = new Coordinate("x:y:z");
	private static final String TEST_FILES_FOLDER = "extra/config/";
	private static final String COORDINATES_VALID = TEST_FILES_FOLDER + "coordinates_valid.txt";

	@Test
	public void addFromUrl() throws IOException {
		URL userDepURL = createTestFile(COORDINATES_VALID).toURI().toURL();
		MavenCoordinates config = new MavenCoordinates();
		config.add(userDepURL);
		assertThat(config.get()).contains(C1.toString(), C2.toString(), C3.toString());
	}

	@Test
	public void addRestrictions() {
		MavenCoordinates config = new MavenCoordinates();
		config.add(C3);
		config.update(C1.toString(), C2.toString());
		assertThat(config.get()).contains(C1.toString(), C2.toString(), C3.toString());
	}

	@Test
	public void dependencySet() {
		MavenCoordinates config = new MavenCoordinates();
		config.add(C1, C2);
		assertThat(config.get().length).isEqualTo(2);
		config.update(C1.toString(), C2.toString());
		assertThat(config.get().length).isEqualTo(2);
		Coordinate otherVersionRange = new Coordinate(C1.getDependency(), "otherVersion");
		config.update(otherVersionRange.toString());
		assertThat(config.get()).contains(otherVersionRange.toString(), C2.toString());
		assertThat(config.get()).doesNotContain(C1.toString());
	}

	@Test
	public void userArgumentRobustness() {
		List<String> complete = Arrays.asList("a:b:c:d:e", " a: b: c: d: e", "a :b :c :d :e");
		List<String> withoutClassifier = Arrays.asList("a:b:c:e", " a: b: c: e", "a :b :c :e");
		List<String> withoutPackaging = Arrays.asList("a:b:e", " a: b: e", "a :b :e");
		Arrays.asList(complete, withoutClassifier, withoutPackaging).forEach(validArgumentSet -> {
			Coordinate expected = null;
			for (String validArgument : validArgumentSet) {
				if (null == expected) {
					expected = new Coordinate(validArgument);
				}
				Coordinate current = new Coordinate(validArgument);
				assertThat(current.toString()).isEqualTo(expected.toString());
			}
		});
	}

	@Test
	public void userArgumentValidation() {
		Arrays.asList(":", "a:e", "a:b:", "a::c:e", ":b:c:e").forEach(invalidArgument -> {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new Coordinate(invalidArgument));
		});
	}

}
