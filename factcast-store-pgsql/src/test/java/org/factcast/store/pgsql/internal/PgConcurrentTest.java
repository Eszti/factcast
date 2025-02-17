/*
 * Copyright © 2018 factcast (http://factcast.org)
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
package org.factcast.store.pgsql.internal;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.factcast.core.Fact;
import org.factcast.core.FactCast;
import org.factcast.core.spec.FactSpec;
import org.factcast.core.store.FactStore;
import org.factcast.core.subscription.SubscriptionRequest;
import org.factcast.core.subscription.observer.FactObserver;
import org.factcast.store.test.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ContextConfiguration(
        classes = { PgTestConfiguration.class })
@Sql(scripts = "/test_schema.sql", config = @SqlConfig(separator = "#"))
@ExtendWith(SpringExtension.class)
@IntegrationTest
public class PgConcurrentTest {

    @Autowired
    FactStore store;

    FactCast uut;

    @BeforeEach
    void setUp() {
        uut = FactCast.from(store);
    }

    private Fact newConcurrentTestFact() {
        return Fact.builder().ns("concurrenttest").id(UUID.randomUUID()).build("{}");
    }

    @Test
    public void testConcurrent() throws Exception {
        // prepare facts
        List<Fact> factsForAsyncBatchPublish = IntStream.range(0, 1000)
                .mapToObj(i -> newConcurrentTestFact())
                .collect(Collectors.toList());
        Fact lonelyFact = newConcurrentTestFact();
        int totalNoOfFacts = factsForAsyncBatchPublish.size() + 1;

        AtomicReference<CountDownLatch> subscriptionBeforePublish = subscribe(totalNoOfFacts);

        boolean letTestFail = true;

        CompletableFuture<Void> batchPublishFuture = CompletableFuture
                .runAsync(() -> uut.publish(factsForAsyncBatchPublish));
        Thread.sleep(200);
        if (letTestFail) {
            uut.publish(lonelyFact);
            batchPublishFuture.get(10, TimeUnit.SECONDS);
        } else {
            batchPublishFuture.get(10, TimeUnit.SECONDS);
            uut.publish(lonelyFact);
        }

        AtomicReference<CountDownLatch> subscriptionAfterPublish = subscribe(totalNoOfFacts);
        assertTrue(subscriptionAfterPublish.get().await(5, TimeUnit.SECONDS));

        // this fails if letTestFail is true
        assertTrue(subscriptionBeforePublish.get().await(5, TimeUnit.SECONDS));
    }

    private AtomicReference<CountDownLatch> subscribe(int expectedNoOfFacts) {
        AtomicReference<CountDownLatch> l = new AtomicReference<>(new CountDownLatch(
                expectedNoOfFacts));
        FactObserver observer = element -> l.get().countDown();
        SubscriptionRequest request = SubscriptionRequest.follow(FactSpec.ns("concurrenttest"))
                .fromScratch();
        uut.subscribeEphemeral(request, observer);
        return l;
    }

}
