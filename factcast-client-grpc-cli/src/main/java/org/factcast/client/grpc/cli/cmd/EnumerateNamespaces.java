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
package org.factcast.client.grpc.cli.cmd;

import org.factcast.client.grpc.cli.util.Command;
import org.factcast.client.grpc.cli.util.Parser.Options;
import org.factcast.core.FactCast;

import com.beust.jcommander.Parameters;

@Parameters(
        commandNames = "enumerateNamespaces",
        commandDescription = "lists all namespaces in the factstore in no particular order")
public class EnumerateNamespaces implements Command {

    @Override
    public void runWith(FactCast fc, Options opt) {
        fc.enumerateNamespaces().forEach(System.out::println);
    }
}
