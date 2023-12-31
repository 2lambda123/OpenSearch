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

package org.opensearch.search.internal;

import org.opensearch.common.io.stream.BytesStreamOutput;
import org.opensearch.index.query.QueryBuilder;
import org.opensearch.index.query.QueryBuilders;
import org.opensearch.index.query.TermQueryBuilder;
import org.opensearch.test.EqualsHashCodeTestUtils;
import org.opensearch.test.OpenSearchTestCase;

import java.util.Arrays;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.instanceOf;

public class AliasFilterTests extends OpenSearchTestCase {

    public void testEqualsAndHashCode() {
        final QueryBuilder filter = QueryBuilders.termQuery("field", "value");
        final String[] aliases = new String[] { "alias_0", "alias_1" };
        final AliasFilter aliasFilter = new AliasFilter(filter, aliases);
        final EqualsHashCodeTestUtils.CopyFunction<AliasFilter> aliasFilterCopyFunction = x -> {
            assertThat(x.getQueryBuilder(), instanceOf(TermQueryBuilder.class));
            final BytesStreamOutput out = new BytesStreamOutput();
            x.getQueryBuilder().writeTo(out);
            final QueryBuilder otherFilter = new TermQueryBuilder(out.bytes().streamInput());
            final String[] otherAliases = Arrays.copyOf(x.getAliases(), x.getAliases().length);
            return new AliasFilter(otherFilter, otherAliases);
        };

        final EqualsHashCodeTestUtils.MutateFunction<AliasFilter> aliasFilterMutationFunction = x -> {
            assertThat(x.getQueryBuilder(), instanceOf(TermQueryBuilder.class));
            final BytesStreamOutput out = new BytesStreamOutput();
            x.getQueryBuilder().writeTo(out);
            final QueryBuilder otherFilter = new TermQueryBuilder(out.bytes().streamInput());
            assertThat(x.getAliases().length, greaterThan(0));
            final String[] otherAliases = Arrays.copyOf(x.getAliases(), x.getAliases().length - 1);
            return new AliasFilter(otherFilter, otherAliases);
        };

        EqualsHashCodeTestUtils.checkEqualsAndHashCode(aliasFilter, aliasFilterCopyFunction, aliasFilterMutationFunction);
    }

}
