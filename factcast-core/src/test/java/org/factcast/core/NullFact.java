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
package org.factcast.core;

import java.util.*;

import lombok.*;

class NullFact implements Fact {
    @Override
    public @NonNull UUID id() {

        return null;
    }

    @Override
    public @NonNull String ns() {

        return null;
    }

    @Override
    public String type() {

        return null;
    }

    @Override
    public @NonNull Set<UUID> aggIds() {

        return null;
    }

    @Override
    public @NonNull String jsonHeader() {

        return null;
    }

    @Override
    public @NonNull String jsonPayload() {

        return null;
    }

    @Override
    public String meta(String key) {

        return null;
    }
}