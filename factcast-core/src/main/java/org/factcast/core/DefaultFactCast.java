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
package org.factcast.core;

import java.util.List;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

import org.factcast.core.lock.LockedOperationBuilder;
import org.factcast.core.store.FactStore;
import org.factcast.core.subscription.ReconnectingFactSubscriptionWrapper;
import org.factcast.core.subscription.Subscription;
import org.factcast.core.subscription.SubscriptionRequest;
import org.factcast.core.subscription.SubscriptionRequestTO;
import org.factcast.core.subscription.observer.FactObserver;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * Default impl for FactCast used by FactCast.from* methods.
 *
 * @author uwe.schaefer@mercateo.com
 */
@RequiredArgsConstructor
class DefaultFactCast implements FactCast {

    @NonNull
    final FactStore store;

    @Override
    @NonNull
    public Subscription subscribeEphemeral(@NonNull SubscriptionRequest req,
            @NonNull FactObserver observer) {
        return store.subscribe(SubscriptionRequestTO.forFacts(req), observer);
    }

    @Override
    public void publish(@NonNull List<? extends Fact> factsToPublish) {
        FactValidation.validate(factsToPublish);
        store.publish(factsToPublish);
    }

    @Override
    @NonNull
    public OptionalLong serialOf(@NonNull UUID id) {
        return store.serialOf(id);
    }

    @Override
    public Set<String> enumerateNamespaces() {
        return store.enumerateNamespaces();
    }

    @Override
    public Set<String> enumerateTypes(@NonNull String ns) {
        return store.enumerateTypes(ns);
    }

    @Override
    public LockedOperationBuilder lock(@NonNull String ns) {
        if (ns.trim().isEmpty())
            throw new IllegalArgumentException("Namespace must not be empty");
        return new LockedOperationBuilder(this.store, ns);
    }

    @Override
    public LockedOperationBuilder lockGlobally() {
        return new LockedOperationBuilder(this.store, null);
    }

    @Override
    public Subscription subscribe(@NonNull SubscriptionRequest request,
            @NonNull FactObserver observer) {
        return new ReconnectingFactSubscriptionWrapper(store, SubscriptionRequestTO.forFacts(
                request),
                observer);
    }
}
