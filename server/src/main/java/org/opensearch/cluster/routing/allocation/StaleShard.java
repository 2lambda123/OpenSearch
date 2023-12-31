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
 *     http://www.apache.org/licenses/LICENSE-2.0
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

package org.opensearch.cluster.routing.allocation;

import org.opensearch.core.index.shard.ShardId;

/**
 * A class that represents a stale shard copy.
 *
 * @opensearch.internal
 */
public class StaleShard {
    private final ShardId shardId;
    private final String allocationId;

    public StaleShard(ShardId shardId, String allocationId) {
        this.shardId = shardId;
        this.allocationId = allocationId;
    }

    @Override
    public String toString() {
        return "stale shard, shard " + shardId + ", alloc. id [" + allocationId + "]";
    }

    /**
     * The shard id of the stale shard.
     */
    public ShardId getShardId() {
        return shardId;
    }

    /**
     * The allocation id of the stale shard.
     */
    public String getAllocationId() {
        return allocationId;
    }
}
