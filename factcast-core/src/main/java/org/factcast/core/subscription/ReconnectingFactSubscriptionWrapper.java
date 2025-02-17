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
package org.factcast.core.subscription;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import org.factcast.core.Fact;
import org.factcast.core.store.FactStore;
import org.factcast.core.subscription.observer.FactObserver;

import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ReconnectingFactSubscriptionWrapper implements Subscription {

    @NonNull
    private final AtomicReference<Subscription> currentSubscription = new AtomicReference<>();

    @NonNull
    private final FactStore store;

    @NonNull
    private final SubscriptionRequestTO originalRequest;

    @NonNull
    private final FactObserver originalObserver;

    @NonNull
    private final FactObserver observer;

    private final AtomicReference<UUID> factIdSeen = new AtomicReference<>();

    private final AtomicBoolean closed = new AtomicBoolean(false);

    private final ExecutorService es = Executors.newCachedThreadPool(new ThreadFactory() {

        AtomicLong threadCount = new AtomicLong(0);

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r);
            thread.setDaemon(true);
            thread.setName("factcast-recon-sub-wrapper-" + threadCount.incrementAndGet());
            return thread;
        }
    });

    @Override
    public void close() throws Exception {

        closed.set(true);
        Subscription cur = currentSubscription.get();
        if (cur != null) {
            cur.close();
        }
        currentSubscription.set(null);
    }

    @Override
    public Subscription awaitCatchup() throws SubscriptionCancelledException {

        for (;;) {
            assertSubscriptionStateNotClosed();
            Subscription cur = currentSubscription.get();
            if (cur != null) {
                try {
                    cur.awaitCatchup();
                    return this;
                } catch (SubscriptionCancelledException ignore) {
                }
            } else {
                sleep();
            }
        }
    }

    private void sleep() {
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {
        }
    }

    @Override
    public Subscription awaitCatchup(long waitTimeInMillis) throws SubscriptionCancelledException,
            TimeoutException {

        long startTime = System.currentTimeMillis();
        for (;;) {
            assertSubscriptionStateNotClosed();
            Subscription cur = currentSubscription.get();

            if (cur != null) {
                try {
                    cur.awaitCatchup(waitTimeInMillis);
                    return this;
                } catch (SubscriptionCancelledException ignore) {
                }
                // escalate TimeoutException
            } else {
                sleep();
            }
            if ((System.currentTimeMillis() - startTime) > waitTimeInMillis) {
                throw new TimeoutException();
            }
        }
    }

    @Override
    public Subscription awaitComplete() throws SubscriptionCancelledException {

        for (;;) {
            assertSubscriptionStateNotClosed();
            Subscription cur = currentSubscription.get();

            if (cur != null) {
                try {
                    cur.awaitComplete();
                    return this;
                } catch (SubscriptionCancelledException ignore) {
                }
            } else {
                sleep();
            }
        }

    }

    @Override
    public Subscription awaitComplete(long waitTimeInMillis) throws SubscriptionCancelledException,
            TimeoutException {

        long startTime = System.currentTimeMillis();
        for (;;) {
            assertSubscriptionStateNotClosed();
            Subscription cur = currentSubscription.get();

            if (cur != null) {
                try {
                    cur.awaitComplete(waitTimeInMillis);
                    return this;
                } catch (SubscriptionCancelledException ignore) {
                }
                // escalate TimeoutException
            } else {
                sleep();
            }

            if ((System.currentTimeMillis() - startTime) > waitTimeInMillis) {
                throw new TimeoutException();
            }
        }

    }

    private void assertSubscriptionStateNotClosed() {
        if (closed.get()) {
            throw new SubscriptionCancelledException("Subscription already cancelled");
        }
    }

    public ReconnectingFactSubscriptionWrapper(@NonNull FactStore store,
            @NonNull SubscriptionRequestTO req,
            @NonNull FactObserver obs) {
        this.store = store;
        this.originalObserver = obs;
        this.originalRequest = req;

        observer = new FactObserver() {

            @Override
            public void onNext(@NonNull Fact element) {
                originalObserver.onNext(element);
                factIdSeen.set(element.id());
            }

            @Override
            public void onCatchup() {
                originalObserver.onCatchup();
            }

            @Override
            public void onComplete() {
                originalObserver.onComplete();
            }

            @Override
            public void onError(@NonNull Throwable exception) {

                log.info("Closing & Reconnecting subscription due to onError triggered.",
                        exception);

                closeAndDetachSubscription();
                CompletableFuture.runAsync(
                        ReconnectingFactSubscriptionWrapper.this::initiateReconnect, es);

                // has to be last call, due to older impls. of onError might
                // decide to throw an exception
                originalObserver.onError(exception);
            }

        };
        initiateReconnect();

    }

    private synchronized void initiateReconnect() {
        SubscriptionRequestTO to = SubscriptionRequestTO.forFacts(originalRequest);
        UUID last = factIdSeen.get();
        if (last != null) {
            to.startingAfter(last);
        }

        for (;;) {
            try {
                if (currentSubscription.get() == null) {
                    // might throw exceptions
                    Subscription subscribe = store.subscribe(to, observer);
                    currentSubscription.compareAndSet(null, subscribe);
                }
                return;
            } catch (Exception ignore) {
                sleep();
            }
        }
    }

    private void closeAndDetachSubscription() {
        Subscription current = currentSubscription.getAndSet(null);
        try {
            current.close();
        } catch (Exception ignore) {
            log.warn("Ignoring Exception while closing a subscription:", ignore);
        }
    }

}
