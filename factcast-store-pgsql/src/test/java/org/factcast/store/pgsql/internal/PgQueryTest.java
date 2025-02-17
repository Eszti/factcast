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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.UUID;

import org.factcast.core.Fact;
import org.factcast.core.spec.FactSpec;
import org.factcast.core.subscription.Subscription;
import org.factcast.core.subscription.SubscriptionRequest;
import org.factcast.core.subscription.SubscriptionRequestTO;
import org.factcast.core.subscription.observer.FactObserver;
import org.factcast.store.test.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import com.google.common.eventbus.EventBus;

import lombok.Data;

@ContextConfiguration(classes = { PgTestConfiguration.class })
@Sql(scripts = "/test_schema.sql", config = @SqlConfig(separator = "#"))
@ExtendWith(SpringExtension.class)
@IntegrationTest
public class PgQueryTest {

    static final FactSpec DEFAULT_SPEC = FactSpec.ns("default-ns").type("type1");

    @Data
    public static class TestHeader {

        String id = UUID.randomUUID().toString();

        String ns = "default-ns";

        String type = "type1";

        @Override
        public String toString() {
            return String.format("{\"id\":\"%s\",\"ns\":\"%s\",\"type\":\"%s\"}", id, ns, type);
        }

        public static TestHeader create() {
            return new TestHeader();
        }
    }

    @Autowired
    PgSubscriptionFactory pq;

    @Autowired
    JdbcTemplate tpl;

    @Bean
    @Primary
    public EventBus eventBus() {
        return new EventBus(this.getClass().getSimpleName());
    }

    @DirtiesContext
    @Test
    void testRoundtrip() {
        SubscriptionRequestTO req = SubscriptionRequestTO
                .forFacts(SubscriptionRequest.catchup(DEFAULT_SPEC).fromScratch());
        FactObserver c = mock(FactObserver.class);
        pq.subscribe(req, c).awaitComplete();
        verify(c, never()).onNext(any());
        verify(c).onCatchup();
        verify(c).onComplete();
    }

    @DirtiesContext
    @Test
    void testRoundtripInsertBefore() {
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create().ns("other-ns"));
        insertTestFact(TestHeader.create().type("type2"));
        insertTestFact(TestHeader.create().ns("other-ns").type("type2"));
        SubscriptionRequestTO req = SubscriptionRequestTO
                .forFacts(SubscriptionRequest.catchup(DEFAULT_SPEC).fromScratch());
        FactObserver c = mock(FactObserver.class);
        pq.subscribe(req, c).awaitComplete();
        verify(c).onCatchup();
        verify(c).onComplete();
        verify(c, times(2)).onNext(any());
    }

    private void insertTestFact(TestHeader header) {
        tpl.execute("INSERT INTO fact(header,payload) VALUES ('" + header + "','{}')");
    }

    @DirtiesContext()
    @Test
    void testRoundtripInsertAfter() throws Exception {
        SubscriptionRequestTO req = SubscriptionRequestTO
                .forFacts(SubscriptionRequest.follow(DEFAULT_SPEC).fromScratch());
        FactObserver c = mock(FactObserver.class);
        pq.subscribe(req, c).awaitCatchup();
        verify(c).onCatchup();
        verify(c, never()).onNext(any(Fact.class));
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create().ns("other-ns"));
        insertTestFact(TestHeader.create().type("type2"));
        insertTestFact(TestHeader.create().ns("other-ns").type("type2"));
        sleep(200);
        verify(c, times(2)).onNext(any(Fact.class));
    }

    @DirtiesContext()
    @Test
    void testRoundtripCatchupEventsInsertedAfterStart() throws Exception {
        SubscriptionRequestTO req = SubscriptionRequestTO
                .forFacts(SubscriptionRequest.follow(DEFAULT_SPEC).fromScratch());
        FactObserver c = mock(FactObserver.class);
        doAnswer(i -> {
            sleep(50);
            return null;
        }).when(c).onNext(any());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        Subscription s = pq.subscribe(req, c);
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        s.awaitCatchup();
        verify(c).onCatchup();
        verify(c, times(8)).onNext(any(Fact.class));
        insertTestFact(TestHeader.create());
        sleep(200);
        verify(c, times(9)).onNext(any(Fact.class));
    }

    // TODO remove all the Thread.sleeps
    private void sleep(long ms) throws InterruptedException {
        Thread.sleep(ms);
    }

    @DirtiesContext()
    @Test
    void testRoundtripCompletion() throws Exception {
        SubscriptionRequestTO req = SubscriptionRequestTO
                .forFacts(SubscriptionRequest.catchup(DEFAULT_SPEC).fromScratch());
        FactObserver c = mock(FactObserver.class);
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        pq.subscribe(req, c).awaitComplete();
        verify(c).onCatchup();
        verify(c).onComplete();
        verify(c, times(5)).onNext(any(Fact.class));
        insertTestFact(TestHeader.create());
        sleep(300);
        verify(c).onCatchup();
        verify(c).onComplete();
        verify(c, times(5)).onNext(any(Fact.class));
    }

    @DirtiesContext()
    @Test
    void testCancel() throws Exception {
        SubscriptionRequestTO req = SubscriptionRequestTO
                .forFacts(SubscriptionRequest.follow(DEFAULT_SPEC).fromScratch());
        FactObserver c = mock(FactObserver.class);
        insertTestFact(TestHeader.create());
        Subscription sub = pq.subscribe(req, c).awaitCatchup();
        verify(c).onCatchup();
        verify(c, times(1)).onNext(any());
        insertTestFact(TestHeader.create());
        insertTestFact(TestHeader.create());
        sleep(200);
        verify(c, times(3)).onNext(any());
        sub.close();
        // must not show up
        insertTestFact(TestHeader.create());
        // must not show up
        insertTestFact(TestHeader.create());
        sleep(200);
        verify(c, times(3)).onNext(any());
    }
}
