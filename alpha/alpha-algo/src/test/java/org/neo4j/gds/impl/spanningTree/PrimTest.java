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
package org.neo4j.gds.impl.spanningTree;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.neo4j.gds.Orientation;
import org.neo4j.gds.api.Graph;
import org.neo4j.gds.core.utils.progress.tasks.ProgressTracker;
import org.neo4j.gds.extension.GdlExtension;
import org.neo4j.gds.extension.GdlGraph;
import org.neo4j.gds.extension.IdFunction;
import org.neo4j.gds.extension.Inject;
import org.neo4j.gds.impl.spanningTrees.Prim;
import org.neo4j.gds.impl.spanningTrees.SpanningTree;

import java.util.stream.Stream;

import static org.junit.jupiter.params.provider.Arguments.arguments;


/**
 * Tests if MSTPrim returns a valid tree for each node
 *
 *         a                  a                  a
 *     1 /   \ 2            /  \                  \
 *      /     \            /    \                  \
 *     b --3-- c          b      c          b       c
 *     |       |  =min=>  |      |  =max=>  |       |
 *     4       5          |      |          |       |
 *     |       |          |      |          |       |
 *     d --6-- e          d      e          d-------e
 */
@GdlExtension
class PrimTest {

    @GdlGraph(orientation = Orientation.UNDIRECTED)
    private static final String DB_CYPHER =
        "CREATE" +
        "  (a:Node)" +
        ", (b:Node)" +
        ", (c:Node)" +
        ", (d:Node)" +
        ", (e:Node)" +
        ", (y:Node)" +
        ", (z:Node)" +

        ", (a)-[:TYPE {cost: 1.0}]->(b)" +
        ", (a)-[:TYPE {cost: 2.0}]->(c)" +
        ", (b)-[:TYPE {cost: 3.0}]->(c)" +
        ", (b)-[:TYPE {cost: 4.0}]->(d)" +
        ", (c)-[:TYPE {cost: 5.0}]->(e)" +
        ", (d)-[:TYPE {cost: 6.0}]->(e)";

    private static long a, b, c, d, e, y, z;

    @Inject
    private Graph graph;

    @Inject
    private IdFunction idFunction;

    private static final String ROOT = "-1";
    @BeforeEach
    void setUp() {
        a = idFunction.of("a");
        b = idFunction.of("b");
        c = idFunction.of("c");
        d = idFunction.of("d");
        e = idFunction.of("e");
        y = idFunction.of("y");
        z = idFunction.of("z");
    }

    static Stream<Arguments> parametersMinimum() {
        return Stream.of(
            arguments("a", ROOT, "a", "a", "b", "c"),
            arguments("b", "b", ROOT, "a", "b", "c"),
            arguments("c", "c", "a", ROOT, "b", "c"),
            arguments("d", "b", "d", "a", ROOT, "c"),
            arguments("e", "c", "a", "e", "b", ROOT)
        );
    }

    static Stream<Arguments> parametersMaximum() {
        return Stream.of(
            arguments("a", ROOT, "d", "a", "e", "c"),
            arguments("b", "c", ROOT, "e", "b", "d"),
            arguments("c", "c", "d", ROOT, "e", "c"),
            arguments("d", "c", "d", "e", ROOT, "d"),
            arguments("e", "c", "d", "e", "e", ROOT)
        );
    }


    @ParameterizedTest
    @MethodSource("parametersMaximum")
    void testMaximum(String nodeId, String parentA, String parentB, String parentC, String parentD, String parentE) {
        var mst = (new Prim(
            graph,
            graph,
            Prim.MAX_OPERATOR,
            idFunction.of(nodeId),
            ProgressTracker.NULL_TRACKER
        ).compute());
        assertTreeIsCorrect(mst, parentA, parentB, parentC, parentD, parentE);
    }

    @ParameterizedTest
    @MethodSource("parametersMinimum")
    void testMinimum(String nodeId, String parentA, String parentB, String parentC, String parentD, String parentE) {
        var mst = (new Prim(
            graph,
            graph,
            Prim.MIN_OPERATOR,
            idFunction.of(nodeId),
            ProgressTracker.NULL_TRACKER
        ).compute());
        assertTreeIsCorrect(mst, parentA, parentB, parentC, parentD, parentE);
    }


    private void assertTreeIsCorrect(
        SpanningTree mst,
        String parentA,
        String parentB,
        String parentC,
        String parentD,
        String parentE
    ) {
        SoftAssertions softAssertions = new SoftAssertions();

        softAssertions.assertThat(mst.effectiveNodeCount).isEqualTo(5);

        softAssertions.assertThat(getExpectedParent(parentA)).as("a").isEqualTo(mst.parent.get(a));
        softAssertions.assertThat(getExpectedParent(parentB)).as("b").isEqualTo(mst.parent.get(b));
        softAssertions.assertThat(getExpectedParent(parentC)).as("c").isEqualTo(mst.parent.get(c));
        softAssertions.assertThat(getExpectedParent(parentD)).as("d").isEqualTo(mst.parent.get(d));
        softAssertions.assertThat(getExpectedParent(parentE)).as("e").isEqualTo(mst.parent.get(e));

        softAssertions.assertThat(mst.parent.get(y)).as("y").isEqualTo(-1);
        softAssertions.assertThat(mst.parent.get(z)).as("z").isEqualTo(-1);

        softAssertions.assertAll();

    }

    long getExpectedParent(String expectedParent) {
        if (expectedParent.equals(ROOT)) {
            return -1;
        } else {
            return idFunction.of(expectedParent);
        }
    }


}
