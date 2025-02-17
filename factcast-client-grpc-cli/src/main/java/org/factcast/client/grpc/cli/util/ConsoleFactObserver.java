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
package org.factcast.client.grpc.cli.util;

import java.util.concurrent.atomic.AtomicBoolean;

import org.factcast.client.grpc.cli.util.Parser.Options;
import org.factcast.core.Fact;
import org.factcast.core.subscription.observer.FactObserver;

import lombok.SneakyThrows;

public class ConsoleFactObserver implements FactObserver {

    private final FactRenderer factRenderer;

    private final AtomicBoolean done = new AtomicBoolean(false);

    public ConsoleFactObserver(Options opt) {
        this.factRenderer = new FactRenderer(opt);
    }

    @Override
    public synchronized void onNext(Fact f) {
        System.out.println(factRenderer.render(f));
    }

    @SneakyThrows
    public synchronized void awaitTermination() {
        while (!done.get())
            this.wait();
    }

    @Override
    public synchronized void onCatchup() {
        System.out.println("-> Signal: Catchup");
    }

    @Override
    public synchronized void onComplete() {
        System.out.println("-> Signal: Complete");
        done.set(true);
        notify();
    }

    @Override
    public synchronized void onError(Throwable exception) {
        System.out.println("-> Signal: Error");
        exception.printStackTrace();
        notify();
    }
}
