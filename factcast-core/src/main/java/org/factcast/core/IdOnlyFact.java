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

import java.util.Set;
import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public final class IdOnlyFact implements Fact {

    @Getter
    @NonNull
    final UUID id;

    @Override
    public String ns() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String type() {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<UUID> aggIds() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String jsonHeader() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String jsonPayload() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String meta(String key) {
        throw new UnsupportedOperationException();
    }
}
