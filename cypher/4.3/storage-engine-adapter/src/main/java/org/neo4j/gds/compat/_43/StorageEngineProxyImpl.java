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
package org.neo4j.gds.compat._43;

import org.neo4j.common.Edition;
import org.neo4j.configuration.Config;
import org.neo4j.configuration.GraphDatabaseInternalSettings;
import org.neo4j.counts.CountsAccessor;
import org.neo4j.counts.CountsStore;
import org.neo4j.dbms.api.DatabaseManagementService;
import org.neo4j.gds.api.GraphStore;
import org.neo4j.gds.compat.AbstractInMemoryNodeCursor;
import org.neo4j.gds.compat.AbstractInMemoryNodePropertyCursor;
import org.neo4j.gds.compat.AbstractInMemoryRelationshipPropertyCursor;
import org.neo4j.gds.compat.AbstractInMemoryRelationshipTraversalCursor;
import org.neo4j.gds.compat.GdsDatabaseManagementServiceBuilder;
import org.neo4j.gds.compat.GraphDatabaseApiProxy;
import org.neo4j.gds.compat.StorageEngineProxyApi;
import org.neo4j.gds.core.cypher.CypherGraphStore;
import org.neo4j.graphdb.Direction;
import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.internal.recordstorage.AbstractInMemoryRelationshipScanCursor;
import org.neo4j.internal.recordstorage.InMemoryStorageReader43;
import org.neo4j.io.layout.DatabaseLayout;
import org.neo4j.storageengine.api.CommandCreationContext;
import org.neo4j.storageengine.api.RelationshipSelection;
import org.neo4j.storageengine.api.StorageEntityCursor;
import org.neo4j.storageengine.api.StoragePropertyCursor;
import org.neo4j.storageengine.api.StorageReader;
import org.neo4j.storageengine.api.StorageRelationshipTraversalCursor;
import org.neo4j.token.TokenHolders;

import static org.neo4j.configuration.GraphDatabaseInternalSettings.storage_engine;

public class StorageEngineProxyImpl implements StorageEngineProxyApi {

    @Override
    public InMemoryStorageEngineImpl.Builder inMemoryStorageEngineBuilder(
        DatabaseLayout databaseLayout,
        TokenHolders tokenHolders
    ) {
        return new InMemoryStorageEngineImpl.Builder(
            databaseLayout,
            tokenHolders,
            new InMemoryMetaDataProviderImpl()
        );
    }

    @Override
    public CountsStore inMemoryCountsStore(
        GraphStore graphStore, TokenHolders tokenHolders
    ) {
        return new InMemoryCountsStoreImpl(graphStore, tokenHolders);
    }

    @Override
    public CommandCreationContext inMemoryCommandCreationContext() {
        return new InMemoryCommandCreationContextImpl();
    }

    @Override
    public void initRelationshipTraversalCursorForRelType(
        StorageRelationshipTraversalCursor cursor,
        long sourceNodeId,
        int relTypeToken
    ) {
        var relationshipSelection = RelationshipSelection.selection(
            relTypeToken,
            Direction.OUTGOING
        );
        cursor.init(sourceNodeId, -1, relationshipSelection);
    }

    @Override
    public StorageReader inMemoryStorageReader(
        CypherGraphStore graphStore, TokenHolders tokenHolders, CountsAccessor counts
    ) {
        return new InMemoryStorageReader43(graphStore, tokenHolders, counts);
    }

    @Override
    public void createInMemoryDatabase(
        DatabaseManagementService dbms,
        String dbName,
        Config config
    ) {
        config.set(storage_engine, InMemoryStorageEngineFactory43.IN_MEMORY_STORAGE_ENGINE_NAME_43);
        dbms.createDatabase(dbName, config);
    }

    @Override
    public GraphDatabaseService startAndGetInMemoryDatabase(DatabaseManagementService dbms, String dbName) {
        dbms.startDatabase(dbName);
        return dbms.database(dbName);
    }

    @Override
    public GdsDatabaseManagementServiceBuilder setSkipDefaultIndexesOnCreationSetting(GdsDatabaseManagementServiceBuilder dbmsBuilder) {
        return dbmsBuilder.setConfig(GraphDatabaseInternalSettings.skip_default_indexes_on_creation, true);
    }

    @Override
    public AbstractInMemoryNodeCursor inMemoryNodeCursor(CypherGraphStore graphStore, TokenHolders tokenHolders) {
        return new InMemoryNodeCursor(graphStore, tokenHolders);
    }

    @Override
    public AbstractInMemoryNodePropertyCursor inMemoryNodePropertyCursor(
        CypherGraphStore graphStore, TokenHolders tokenHolders
    ) {
        return new InMemoryNodePropertyCursor(graphStore, tokenHolders);
    }

    @Override
    public AbstractInMemoryRelationshipTraversalCursor inMemoryRelationshipTraversalCursor(
        CypherGraphStore graphStore, TokenHolders tokenHolders
    ) {
        return new InMemoryRelationshipTraversalCursor(graphStore, tokenHolders);
    }

    @Override
    public AbstractInMemoryRelationshipScanCursor inMemoryRelationshipScanCursor(
        CypherGraphStore graphStore, TokenHolders tokenHolders
    ) {
        return new InMemoryRelationshipScanCursor(graphStore, tokenHolders);
    }

    @Override
    public AbstractInMemoryRelationshipPropertyCursor inMemoryRelationshipPropertyCursor(
        CypherGraphStore graphStore, TokenHolders tokenHolders
    ) {
        return new InMemoryRelationshipPropertyCursor(graphStore, tokenHolders);
    }

    @Override
    public void properties(
        StorageEntityCursor storageCursor, StoragePropertyCursor propertyCursor, int[] propertySelection
    ) {
        storageCursor.properties(propertyCursor);
    }

    @Override
    public Edition dbmsEdition(GraphDatabaseService databaseService) {
        return GraphDatabaseApiProxy.dbmsInfo(databaseService).edition;
    }
}
