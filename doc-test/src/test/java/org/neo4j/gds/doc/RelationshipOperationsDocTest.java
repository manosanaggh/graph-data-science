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
package org.neo4j.gds.doc;

import org.neo4j.gds.catalog.GraphDropRelationshipProc;
import org.neo4j.gds.catalog.GraphProjectProc;
import org.neo4j.gds.catalog.GraphStreamRelationshipPropertiesProc;
import org.neo4j.gds.catalog.GraphStreamRelationshipsProc;
import org.neo4j.gds.catalog.GraphWriteRelationshipProc;
import org.neo4j.gds.functions.AsNodeFunc;
import org.neo4j.gds.similarity.nodesim.NodeSimilarityMutateProc;

import java.util.List;

final class RelationshipOperationsDocTest extends SingleFileDocTestBase {

    @Override
    protected List<Class<?>> functions() {
        return List.of(AsNodeFunc.class);
    }

    @Override
    protected List<Class<?>> procedures() {
        return List.of(
            GraphProjectProc.class,
            NodeSimilarityMutateProc.class,
            GraphStreamRelationshipsProc.class,
            GraphStreamRelationshipPropertiesProc.class,
            GraphWriteRelationshipProc.class,
            GraphDropRelationshipProc.class
        );
    }

    @Override
    protected String adocFile() {
        return "pages/graph-catalog-relationship-ops.adoc";
    }

}
