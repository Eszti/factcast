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
package org.factcast.store.pgsql.internal.rowmapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.factcast.core.Fact;
import org.factcast.store.pgsql.internal.PgConstants;
import org.junit.jupiter.api.Test;

public class PgFactExtractorTest {

    private AtomicLong serial = new AtomicLong(5);

    private PgFactExtractor uut = new PgFactExtractor(serial);

    @Test
    void testMapRow() throws Exception {

        ResultSet rs = mock(ResultSet.class);
        final UUID id = UUID.randomUUID();

        when(rs.getString(PgConstants.ALIAS_ID)).thenReturn(id.toString());
        when(rs.getString(PgConstants.ALIAS_NS)).thenReturn("ns");
        when(rs.getString(PgConstants.COLUMN_HEADER)).thenReturn("{\"ns\":\"ns\",\"id\":\"" + id
                + "\"}");
        when(rs.getString(PgConstants.COLUMN_PAYLOAD)).thenReturn("{}");
        when(rs.getLong(PgConstants.COLUMN_SER)).thenReturn(27L);
        Fact mapRow = uut.mapRow(rs, 1);
        assertEquals(27, serial.get());
        assertEquals(id, mapRow.id());
    }

    @Test
    void testMapRowNullContracts() throws Exception {

        assertThrows(NullPointerException.class, () -> {
            uut.mapRow(null, 1);
        });
    }
}
