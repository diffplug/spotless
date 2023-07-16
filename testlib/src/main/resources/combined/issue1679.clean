/*
 * Copyright 2021-2022 Creek Contributors (https://github.com/creek-service)
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

package io.github.creek.service.basic.kafka.streams.demo.services;

// formatting:off
// begin-snippet: includes-1
import static io.github.creek.service.basic.kafka.streams.demo.internal.TopicConfigBuilder.withPartitions;
import static io.github.creek.service.basic.kafka.streams.demo.internal.TopicDescriptors.inputTopic;
import static io.github.creek.service.basic.kafka.streams.demo.internal.TopicDescriptors.outputTopic;
// end-snippet
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
// begin-snippet: includes-2
import org.creekservice.api.kafka.metadata.OwnedKafkaTopicInput;
import org.creekservice.api.kafka.metadata.OwnedKafkaTopicOutput;
// end-snippet
import org.creekservice.api.platform.metadata.ComponentInput;
import org.creekservice.api.platform.metadata.ComponentInternal;
import org.creekservice.api.platform.metadata.ComponentOutput;
import org.creekservice.api.platform.metadata.ServiceDescriptor;
// formatting:on

// begin-snippet: class-name
public final class HandleOccurrenceServiceDescriptor implements ServiceDescriptor {
    // end-snippet
    private static final List<ComponentInput> INPUTS = new ArrayList<>();
    private static final List<ComponentInternal> INTERNALS = new ArrayList<>();
    private static final List<ComponentOutput> OUTPUTS = new ArrayList<>();

    // formatting:off
// begin-snippet: topic-resources
    // Define the tweet-text input topic, conceptually owned by this service:
    public static final OwnedKafkaTopicInput<Long, String> TweetTextStream =
            register(
                    inputTopic(
                            "twitter.tweet.text", // Topic name
                            Long.class, // Topic key: Tweet id
                            String.class, // Topic value: Tweet text
                            withPartitions(5))); // Topic config

    // Define the output topic, again conceptually owned by this service:
    public static final OwnedKafkaTopicOutput<String, Integer> TweetHandleUsageStream =
            register(outputTopic(
                    "twitter.handle.usage",
                    String.class, // Twitter handle
                    Integer.class,  // Usage count
                    withPartitions(6)
                        .withRetentionTime(Duration.ofHours(12))
            ));
// end-snippet
// formatting:on

    public HandleOccurrenceServiceDescriptor() {}

    @Override
    public String dockerImage() {
        return "ghcr.io/creek-service/basic-kafka-streams-demo-handle-occurrence-service";
    }

    @Override
    public Collection<ComponentInput> inputs() {
        return List.copyOf(INPUTS);
    }

    @Override
    public Collection<ComponentInternal> internals() {
        return List.copyOf(INTERNALS);
    }

    @Override
    public Collection<ComponentOutput> outputs() {
        return List.copyOf(OUTPUTS);
    }

    private static <T extends ComponentInput> T register(final T input) {
        INPUTS.add(input);
        return input;
    }

    // Uncomment if needed:
    // private static <T extends ComponentInternal> T register(final T internal) {
    //     INTERNALS.add(internal);
    //     return internal;
    // }

    private static <T extends ComponentOutput> T register(final T output) {
        OUTPUTS.add(output);
        return output;
    }
}
