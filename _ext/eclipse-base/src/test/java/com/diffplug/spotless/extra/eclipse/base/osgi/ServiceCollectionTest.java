/*
 * Copyright 2016-2021 DiffPlug
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
package com.diffplug.spotless.extra.eclipse.base.osgi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.HashMap;

import org.assertj.core.api.AbstractAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.Bundle;
import org.osgi.framework.ServiceException;
import org.osgi.framework.ServiceReference;

class ServiceCollectionTest {

	ServiceCollection instance;

	@BeforeEach
	void initialize() {
		Bundle systemBundle = new TestBundle(0, "test.system");
		instance = new ServiceCollection(systemBundle, new HashMap<String, String>());
	}

	@Test
	void testAddGet() {
		Service1 service1 = new Service1();
		Service2 service2 = new Service2();
		instance.add(Interf1.class, service1);
		instance.add(Interf2a.class, service2);
		instance.add(Interf2b.class, service2);
		assertFor(instance).getServiceForReferences(Interf1.class).matchesService(service1);
		assertFor(instance).getServiceForReferences(Interf2a.class).matchesService(service2);
		assertFor(instance).getServiceForReferences(Interf2b.class).matchesService(service2);
	}

	@Test
	void testMultipleServicesPerInterface() {
		Service1 serviceX = new Service1();
		Service1 serviceY = new Service1();
		instance.add(Interf1.class, serviceX);
		ServiceException e = assertThrows(ServiceException.class, () -> instance.add(Interf1.class, serviceY));
		assertThat(e.getMessage()).as("ServiceException does not contain interface class name.").contains(Interf1.class.getName());
	}

	private static class ServiceReferenceAssert extends AbstractAssert<ServiceReferenceAssert, ServiceCollection> {

		private final ServiceCollection actual;
		private final ServiceReference<?> reference;

		public ServiceReferenceAssert(ServiceCollection actual) {
			this(actual, null);
		}

		public ServiceReferenceAssert(ServiceCollection actual, ServiceReference<?> reference) {
			super(actual, ServiceReferenceAssert.class);
			this.reference = reference;
			this.actual = actual;

		}

		ServiceReferenceAssert getServiceForReferences(Class<?> interfaceClass) {
			ServiceReference<?>[] references = actual.getReferences(interfaceClass.getName());
			int numberOfFoundReferences = null == references ? 0 : references.length;
			if (numberOfFoundReferences != 1) {
				failWithMessage("Expected to find exactly 1 reference for <%s> , but found %d.", interfaceClass.getName(), numberOfFoundReferences);
			}
			return new ServiceReferenceAssert(actual, references[0]);
		}

		ServiceReferenceAssert matchesService(Object expected) {
			if (null == reference) {
				failWithMessage("No reference requested.");
			}
			Object serviceForRef = actual.getService(reference);
			if (null == serviceForRef) {
				failWithMessage("No service provided for reference.");
			}
			if (!serviceForRef.equals(expected)) {
				failWithMessage("Unexpected service found.");
			}

			return this;
		}
	}

	private static ServiceReferenceAssert assertFor(ServiceCollection actual) {
		return new ServiceReferenceAssert(actual);
	}

	private static interface Interf1 {};

	private static interface Interf2a {};

	private static interface Interf2b {};

	private static class Service1 implements Interf1 {};

	private static class Service2 implements Interf2a, Interf2b {};
}
