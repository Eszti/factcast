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
package org.factcast.server.grpc.auth;

import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import org.factcast.core.util.FactCastJson;

import lombok.Data;

public interface CredentialConfiguration {
    List<FullAccessCredential> fullAccess();

    List<ReadOnlyAccessCredential> readOnlyAccess();

    static CredentialConfiguration read(InputStream f) {
        return FactCastJson.readValue(CredentialConfigurationImpl.class, f);
    }

    static CredentialConfiguration read(String json) {
        return FactCastJson.readValue(CredentialConfigurationImpl.class, json);
    }

    @Data
    class CredentialConfigurationImpl implements CredentialConfiguration {
        private List<FullAccessCredential> fullAccess = new LinkedList<>();

        private List<ReadOnlyAccessCredential> readOnlyAccess = new LinkedList<>();

    }

    default Optional<? extends AccessCredential> find(String username) {
        Predicate<AccessCredential> p = a -> a.name().equals(username);
        LinkedList<AccessCredential> all = new LinkedList<>(fullAccess());
        all.addAll(readOnlyAccess());
        return all.stream().filter(p).findFirst();
    }
}