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
package org.factcast.store.pgsql.internal.lock;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class AdvisoryWriteLock implements FactTableWriteLock {
    private final JdbcTemplate tpl;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void aquireExclusiveTXLock() {
        tpl.execute("SELECT pg_advisory_xact_lock(" + AdvisoryLocks.PUBLISH.code() + ")");
    }

}
