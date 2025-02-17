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

import org.factcast.core.DefaultFact.Header;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.NonNull;

/**
 * Defines a fact to be either published or consumed. Consists of two JSON
 * Strings: jsonHeader and jsonPayload. Also provides convenience getters for
 * id,ns,type and aggId.
 *
 * @author uwe.schaefer@mercateo.com
 */
public interface Fact {

    @NonNull
    UUID id();

    @NonNull
    String ns();

    String type();

    @NonNull
    Set<UUID> aggIds();

    @NonNull
    String jsonHeader();

    @NonNull
    String jsonPayload();

    String meta(String key);

    default long serial() {
        String s = meta("_ser");
        if (s != null) {
            return Long.valueOf(s);
        } else {
            throw new IllegalStateException("'_ser' Meta attribute not found");
        }
    }

    // hint to where to get the default from
    static Fact of(@NonNull String jsonHeader, @NonNull String jsonPayload) {
        return DefaultFact.of(jsonHeader, jsonPayload);
    }

    static Fact of(@NonNull JsonNode jsonHeader, @NonNull JsonNode jsonPayload) {
        return DefaultFact.of(jsonHeader.toString(), jsonPayload.toString());
    }

    default boolean before(Fact other) {
        return serial() < other.serial();
    }

    static Fact.Builder builder() {
        return new Builder();
    }

    class Builder {

        final Header header = new Header().id(UUID.randomUUID()).ns("default");

        public Builder aggId(@NonNull UUID aggId) {
            header.aggIds().add(aggId);
            return this;
        }

        public Builder ns(@NonNull String ns) {
            if (ns.trim().isEmpty()) {
                throw new IllegalArgumentException("Namespace must not be empty");
            }
            header.ns(ns);
            return this;
        }

        public Builder id(@NonNull UUID id) {
            header.id(id);
            return this;
        }

        public Builder type(@NonNull String type) {
            if (type.trim().isEmpty()) {
                throw new IllegalArgumentException("type must not be empty");
            }

            header.type(type);
            return this;
        }

        public Builder meta(@NonNull String key, String value) {
            header.meta().put(key, value);
            return this;
        }

        public Fact buildWithoutPayload() {
            return build(null);
        }

        public Fact build(String payload) {
            String pl = payload;
            if (payload == null || payload
                    .trim()
                    .isEmpty()) {
                pl = "{}";
            }
            return new DefaultFact(header, pl);
        }
    }
}
