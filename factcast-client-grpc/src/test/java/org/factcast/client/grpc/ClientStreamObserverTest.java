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
package org.factcast.client.grpc;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.UUID;

import org.factcast.core.Fact;
import org.factcast.core.IdOnlyFact;
import org.factcast.core.subscription.SubscriptionImpl;
import org.factcast.core.subscription.observer.FactObserver;
import org.factcast.grpc.api.conv.ProtoConverter;
import org.factcast.grpc.api.gen.FactStoreProto.MSG_Notification;
import org.factcast.grpc.api.gen.FactStoreProto.MSG_Notification.Type;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientStreamObserverTest {

    @Mock
    FactObserver factObserver;

    ClientStreamObserver uut;

    ProtoConverter converter = new ProtoConverter();

    private SubscriptionImpl<Fact> subscription;

    @BeforeEach
    void setUp() {
        subscription = spy(new SubscriptionImpl<>(factObserver));
        uut = new ClientStreamObserver(subscription);
    }

    @Test
    void testConstructorNull() {
        Assertions.assertThrows(NullPointerException.class, () -> new ClientStreamObserver(null));
    }

    @Test
    void testOnNext() {
        Fact f = Fact.of("{\"ns\":\"ns\",\"id\":\"" + UUID.randomUUID() + "\"}", "{}");
        MSG_Notification n = converter.createNotificationFor(f);
        uut.onNext(n);
        verify(factObserver).onNext(eq(f));
    }

    @Test
    void testOnNextFailsOnUnknownMessage() {
        assertThrows(IllegalArgumentException.class, () -> {
            MSG_Notification n = MSG_Notification.newBuilder().setType(Type.UNRECOGNIZED).build();
            uut.onNext(n);
        });
    }

    @Test
    void testOnNextId() {
        MSG_Notification n = converter.createNotificationFor(UUID.randomUUID());
        uut.onNext(n);
        verify(factObserver).onNext(any(IdOnlyFact.class));
    }

    @Test
    void testOnCatchup() {
        uut.onNext(converter.createCatchupNotification());
        verify(factObserver).onCatchup();
    }

    @Test
    void testFailOnUnknownType() {
            uut.onNext(MSG_Notification.newBuilder().setTypeValue(999).build());
            verify(subscription).notifyError(any(RuntimeException.class));
    }

    @Test
    void testOnComplete() {
        uut.onNext(converter.createCompleteNotification());
        verify(factObserver).onComplete();
    }

    @Test
    void testOnTransportComplete() {
        uut.onCompleted();
        verify(factObserver).onComplete();
    }

    @Test
    void testOnError() {
        uut.onError(new IOException());
        verify(factObserver).onError(any());
    }
}
