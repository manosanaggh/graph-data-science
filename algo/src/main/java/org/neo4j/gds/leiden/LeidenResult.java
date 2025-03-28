/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.gds.leiden;

import org.immutables.value.Value;
import org.neo4j.gds.annotation.ValueClass;
import org.neo4j.gds.core.utils.paged.HugeLongArray;

import java.util.function.LongUnaryOperator;

@ValueClass
@SuppressWarnings("immutables:subtype")
public interface LeidenResult {

    HugeLongArray communities();

    int ranLevels();

    boolean didConverge();

    LeidenDendrogramManager dendrogramManager();

    double[] modularities();

    double modularity();

    default long[] getIntermediateCommunities(long nodeId) {
        var dendrograms = dendrogramManager().getAllDendrograms();
        int levels = ranLevels();
        long[] communities = new long[levels];
        for (int i = 0; i < levels; i++) {
            communities[i] = dendrograms[i].get(nodeId);
        }

        return communities;
    }

    @Value.Derived
    default LongUnaryOperator communitiesFunction() {
        return communities()::get;
    }

    static LeidenResult of(
        HugeLongArray communities,
        int ranLevels,
        boolean didConverge,
        LeidenDendrogramManager dendrogramManager,
        double[] modularities,
        double modularity
    ) {
        return ImmutableLeidenResult.of(
            communities,
            ranLevels,
            didConverge,
            dendrogramManager,
            modularities,
            modularity
        );
    }

}
