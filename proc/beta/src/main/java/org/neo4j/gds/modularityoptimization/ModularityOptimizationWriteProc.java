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
package org.neo4j.gds.modularityoptimization;

import org.neo4j.gds.GraphAlgorithmFactory;
import org.neo4j.gds.WriteProc;
import org.neo4j.gds.api.properties.nodes.NodePropertyValues;
import org.neo4j.gds.core.CypherMapWrapper;
import org.neo4j.gds.executor.ComputationResult;
import org.neo4j.gds.executor.ExecutionContext;
import org.neo4j.gds.executor.GdsCallable;
import org.neo4j.gds.result.AbstractResultBuilder;
import org.neo4j.gds.results.MemoryEstimateResult;
import org.neo4j.internal.kernel.api.procs.ProcedureCallContext;
import org.neo4j.procedure.Description;
import org.neo4j.procedure.Name;
import org.neo4j.procedure.Procedure;

import java.util.Map;
import java.util.stream.Stream;

import static org.neo4j.gds.executor.ExecutionMode.WRITE_NODE_PROPERTY;
import static org.neo4j.procedure.Mode.READ;
import static org.neo4j.procedure.Mode.WRITE;

@GdsCallable(name = "gds.beta.modularityOptimization.write", description = ModularityOptimizationProc.MODULARITY_OPTIMIZATION_DESCRIPTION, executionMode = WRITE_NODE_PROPERTY)
public class ModularityOptimizationWriteProc extends WriteProc<ModularityOptimization, ModularityOptimization, ModularityOptimizationWriteProc.WriteResult, ModularityOptimizationWriteConfig> {

    @Procedure(name = "gds.beta.modularityOptimization.write", mode = WRITE)
    @Description(ModularityOptimizationProc.MODULARITY_OPTIMIZATION_DESCRIPTION)
    public Stream<WriteResult> write(
        @Name(value = "graphName") String graphName,
        @Name(value = "configuration", defaultValue = "{}") Map<String, Object> configuration
    ) {
        return write(compute(graphName, configuration));
    }

    @Procedure(value = "gds.beta.modularityOptimization.write.estimate", mode = READ)
    @Description(ESTIMATE_DESCRIPTION)
    public Stream<MemoryEstimateResult> estimate(
        @Name(value = "graphNameOrConfiguration") Object graphNameOrConfiguration,
        @Name(value = "algoConfiguration") Map<String, Object> algoConfiguration
    ) {
        return computeEstimate(graphNameOrConfiguration, algoConfiguration);
    }

    @Override
    protected ModularityOptimizationWriteConfig newConfig(String username, CypherMapWrapper config) {
        return ModularityOptimizationWriteConfig.of(config);
    }

    @Override
    public GraphAlgorithmFactory<ModularityOptimization, ModularityOptimizationWriteConfig> algorithmFactory() {
        return new ModularityOptimizationFactory<>();
    }

    @Override
    protected NodePropertyValues nodeProperties(ComputationResult<ModularityOptimization, ModularityOptimization, ModularityOptimizationWriteConfig> computationResult) {
        return ModularityOptimizationProc.nodeProperties(computationResult);
    }

    @Override
    protected AbstractResultBuilder<WriteResult> resultBuilder(
        ComputationResult<ModularityOptimization, ModularityOptimization, ModularityOptimizationWriteConfig> computeResult,
        ExecutionContext executionContext
    ) {
        return ModularityOptimizationProc.resultBuilder(
            new WriteResult.Builder(callContext, computeResult.config().concurrency()),
            computeResult
        );
    }

    @SuppressWarnings("unused")
    public static class WriteResult {

        public final long preProcessingMillis;
        public final long computeMillis;
        public final long writeMillis;
        public final long postProcessingMillis;
        public final long nodes;
        public boolean didConverge;
        public long ranIterations;
        public double modularity;
        public final long communityCount;
        public final Map<String, Object> communityDistribution;
        public final Map<String, Object> configuration;

        WriteResult(
            long preProcessingMillis,
            long computeMillis,
            long postProcessingMillis,
            long writeMillis,
            long nodes,
            boolean didConverge,
            long ranIterations,
            double modularity,
            long communityCount,
            Map<String, Object> communityDistribution,
            Map<String, Object> configuration
        ) {
            this.preProcessingMillis = preProcessingMillis;
            this.computeMillis = computeMillis;
            this.writeMillis = writeMillis;
            this.postProcessingMillis = postProcessingMillis;
            this.nodes = nodes;
            this.didConverge = didConverge;
            this.ranIterations = ranIterations;
            this.modularity = modularity;
            this.communityCount = communityCount;
            this.communityDistribution = communityDistribution;
            this.configuration = configuration;
        }

        static class Builder extends ModularityOptimizationProc.ModularityOptimizationResultBuilder<WriteResult> {

            Builder(
                ProcedureCallContext context,
                int concurrency
            ) {
                super(context, concurrency);
            }

            @Override
            protected WriteResult buildResult() {
                return new WriteResult(
                    preProcessingMillis,
                    computeMillis,
                    postProcessingDuration,
                    writeMillis,
                    nodeCount,
                    didConverge,
                    ranIterations,
                    modularity,
                    maybeCommunityCount.orElse(0),
                    communityHistogramOrNull(),
                    config.toMap()
                );
            }
        }
    }
}
