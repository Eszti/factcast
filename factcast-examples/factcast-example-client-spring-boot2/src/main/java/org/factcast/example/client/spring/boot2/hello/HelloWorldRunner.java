/*
 * Copyright © 2019 factcast
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
package org.factcast.example.client.spring.boot2.hello;

import java.util.UUID;

import org.factcast.core.Fact;
import org.factcast.core.FactCast;
import org.factcast.core.lock.Attempt;
import org.factcast.core.lock.PublishingResult;
import org.factcast.core.spec.FactSpec;
import org.factcast.core.subscription.Subscription;
import org.factcast.core.subscription.SubscriptionRequest;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Component
public class HelloWorldRunner implements CommandLineRunner {

    @NonNull
    private final FactCast fc;

    @Override
    public void run(String... args) throws Exception {

        Fact fact = Fact.builder().ns("smoke").type("foo").build("{\"bla\":\"fasel\"}");
        fc.publish(fact);
        System.out.println("published " + fact);

        Subscription sub = fc.subscribe(SubscriptionRequest.catchup(FactSpec.ns("smoke"))
                .fromScratch(),
                System.out::println).awaitCatchup(5000);

        sub.close();

        UUID id = UUID.randomUUID();
        System.out.println("trying to publish with optimistic locking");

        PublishingResult success = fc.lock("foo")
                .on(id)
                .optimistic()
                .attempt(() -> Attempt.publish(Fact
                        .builder()
                        .aggId(id)
                        .ns("foo")
                        .buildWithoutPayload()));
        System.out.println("published succeeded: " + (success != null));
        System.out.println("published id: " + success);

        System.out.println("trying another with optimistic locking");
        success = fc.lock("foo")
                .on(id)
                .optimistic()
                .attempt(() -> Attempt.publish(Fact.builder()
                        .aggId(id)
                        .ns("foo")
                        .buildWithoutPayload()));
        System.out.println("published succeeded: " + (success != null));
        System.out.println("published id: " + success);

        sub = fc.subscribe(SubscriptionRequest.catchup(FactSpec.ns("foo").aggId(id))
                .fromScratch(),
                System.out::println).awaitCatchup(5000);

        sub.close();

    }

}
