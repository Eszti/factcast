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

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class IdOnlyFactTest {

    @Test
    void testNs() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new IdOnlyFact(UUID.randomUUID()).ns();
        });
    }

    @Test
    void testType() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new IdOnlyFact(UUID.randomUUID()).type();
        });
    }

    @Test
    void testAggIds() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new IdOnlyFact(UUID.randomUUID()).aggIds();
        });
    }

    @Test
    void testJsonHeader() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new IdOnlyFact(UUID.randomUUID()).jsonHeader();
        });
    }

    @Test
    void testJsonPayload() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new IdOnlyFact(UUID.randomUUID()).jsonPayload();
        });
    }

    @Test
    void testMeta() {
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            new IdOnlyFact(UUID.randomUUID()).meta("");
        });
    }

    @Test
    void testNullId() {
        Assertions.assertThrows(NullPointerException.class, () -> {
            new IdOnlyFact(null);
        });
    }
}
