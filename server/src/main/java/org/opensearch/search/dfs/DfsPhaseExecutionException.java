/*
 * SPDX-License-Identifier: Apache-2.0
 *
 * The OpenSearch Contributors require contributions made to
 * this file be licensed under the Apache-2.0 license or a
 * compatible open source license.
 */

/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

/*
 * Modifications Copyright OpenSearch Contributors. See
 * GitHub history for details.
 */

package org.opensearch.search.dfs;

import org.opensearch.core.common.io.stream.StreamInput;
import org.opensearch.search.SearchException;
import org.opensearch.search.SearchShardTarget;

import java.io.IOException;

/**
 * Thrown if there are any errors in the DFS phase
 *
 * @opensearch.internal
 */
public class DfsPhaseExecutionException extends SearchException {

    public DfsPhaseExecutionException(SearchShardTarget shardTarget, String msg, Throwable t) {
        super(shardTarget, "Dfs Failed [" + msg + "]", t);
    }

    public DfsPhaseExecutionException(SearchShardTarget shardTarget, String msg) {
        super(shardTarget, "Dfs Failed [" + msg + "]");
    }

    public DfsPhaseExecutionException(StreamInput in) throws IOException {
        super(in);
    }
}
