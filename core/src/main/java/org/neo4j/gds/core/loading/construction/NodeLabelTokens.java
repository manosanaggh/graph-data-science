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
package org.neo4j.gds.core.loading.construction;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.neo4j.gds.NodeLabel;
import org.neo4j.graphdb.Label;
import org.neo4j.values.SequenceValue;
import org.neo4j.values.storable.TextValue;

import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.function.Function;

import static org.neo4j.gds.utils.StringFormatting.formatWithLocale;

public final class NodeLabelTokens {
    public static @NotNull NodeLabelToken of(@Nullable Object nodeLabels) {
        var nodeLabelToken = ofNullable(nodeLabels);
        if (nodeLabelToken == null) {
            throw new IllegalArgumentException(formatWithLocale(
                "Could not represent a value of type[%s] as nodeLabels: %s",
                nodeLabels.getClass(),
                nodeLabels
            ));
        }
        return nodeLabelToken;
    }

    public static @NotNull NodeLabelToken of(SequenceValue nodeLabels) {
        return new NodeLabelToken() {
            @Override
            public boolean isEmpty() {
                return nodeLabels.isEmpty();
            }

            @Override
            public int size() {
                return nodeLabels.length();
            }

            @Override
            public @NotNull NodeLabel get(int index) {
                return new NodeLabel(((TextValue) nodeLabels.value(index)).stringValue());
            }

            @Override
            public String[] getStrings() {
                var result = new String[nodeLabels.length()];
                for (int i = 0; i < nodeLabels.length(); i++) {
                    result[i] = ( (TextValue) nodeLabels.value(i)).stringValue();
                }
                return result;
            }
        };
    }

    @Contract("null -> !null")
    public static @Nullable NodeLabelToken ofNullable(@Nullable Object nodeLabels) {
        if (nodeLabels == null) {
            return empty();
        }

        if (nodeLabels instanceof NodeLabel[]) {
            return ofNodeLabels((NodeLabel[]) nodeLabels);
        }

        if (nodeLabels instanceof String[]) {
            return new Array<>((String[]) nodeLabels, NodeLabelTokens::labelOf);
        }

        if (nodeLabels instanceof Label[]) {
            return new Array<>((Label[]) nodeLabels, NodeLabelTokens::labelOf);
        }

        if (nodeLabels instanceof List) {
            List<?> labels = (List<?>) nodeLabels;
            return ofList(labels);
        }

        return ofSingleton(nodeLabels);
    }

    public static @NotNull NodeLabelToken empty() {
        return Null.INSTANCE;
    }

    public static @NotNull NodeLabelToken ofNodeLabels(NodeLabel... nodeLabels) {
        return new Array<>(nodeLabels, Function.identity());
    }

    public static @NotNull NodeLabelToken ofStrings(String... nodeLabelStrings) {
        return new Array<>(nodeLabelStrings, NodeLabelTokens::labelOf);
    }

    static @NotNull NodeLabelToken ofNodeLabel(NodeLabel nodeLabel) {
        return new Singleton<>(nodeLabel, Function.identity());
    }

    private static @Nullable NodeLabelToken ofList(List<?> nodeLabels) {
        if (nodeLabels.isEmpty()) {
            return empty();
        }

        Object label = nodeLabels.get(0);

        if (nodeLabels.size() == 1) {
            return ofSingleton(label);
        }

        if (label instanceof NodeLabel) {
            //noinspection unchecked
            return new JavaList<>((List<NodeLabel>) nodeLabels, Function.identity());
        }
        if (label instanceof String) {
            //noinspection unchecked
            return new JavaList<>((List<String>) nodeLabels, NodeLabelTokens::labelOf);
        }
        if (label instanceof Label) {
            //noinspection unchecked
            return new JavaList<>((List<Label>) nodeLabels, NodeLabelTokens::labelOf);
        }

        return null;
    }

    private static @Nullable NodeLabelToken ofSingleton(Object nodeLabel) {
        if (nodeLabel instanceof NodeLabel) {
            return new Singleton<>((NodeLabel) nodeLabel, Function.identity());
        }

        if (nodeLabel instanceof String) {
            return new Singleton<>((String) nodeLabel, NodeLabelTokens::labelOf);
        }

        if (nodeLabel instanceof Label) {
            return new Singleton<>((Label) nodeLabel, NodeLabelTokens::labelOf);
        }

        return null;
    }

    private static NodeLabel labelOf(String label) {
        return new NodeLabel(label);
    }

    private static NodeLabel labelOf(Label label) {
        return new NodeLabel(label.name());
    }

    private enum Null implements NodeLabelToken {
        INSTANCE;

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public int size() {
            return 0;
        }

        @Override
        public @NotNull NodeLabel get(int index) {
            throw new NoSuchElementException();
        }

        @Override
        public String[] getStrings() {
            return new String[0];
        }
    }

    private static final class Array<T> implements NodeLabelToken {
        private final @NotNull T[] nodeLabels;

        private final Function<T, NodeLabel> toNodeLabel;

        private Array(T[] nodeLabels, Function<T, NodeLabel> toNodeLabel) {
            this.nodeLabels = nodeLabels;
            this.toNodeLabel = toNodeLabel;
        }

        @Override
        public boolean isEmpty() {
            return nodeLabels.length == 0;
        }

        @Override
        public int size() {
            return nodeLabels.length;
        }

        @Override
        public @NotNull NodeLabel get(int index) {
            return toNodeLabel.apply(nodeLabels[index]);
        }

        @Override
        public String[] getStrings() {
            return Arrays.stream(nodeLabels).map(toNodeLabel).map(NodeLabel::name).toArray(String[]::new);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Array<?> array = (Array<?>) o;
            return Arrays.equals(nodeLabels, array.nodeLabels);
        }

        @Override
        public int hashCode() {
            return Arrays.hashCode(nodeLabels);
        }
    }

    private static final class JavaList<T> implements NodeLabelToken {
        private final @NotNull List<T> nodeLabels;

        private final Function<T, NodeLabel> toNodeLabel;

        private JavaList(@NotNull List<T> nodeLabels, Function<T, NodeLabel> toNodeLabel) {
            this.nodeLabels = nodeLabels;
            this.toNodeLabel = toNodeLabel;
        }

        @Override
        public boolean isEmpty() {
            return nodeLabels.isEmpty();
        }

        @Override
        public int size() {
            return nodeLabels.size();
        }

        @Override
        public @NotNull NodeLabel get(int index) {
            return toNodeLabel.apply(nodeLabels.get(index));
        }

        @Override
        public String[] getStrings() {
            return nodeLabels.stream().map(toNodeLabel).map(NodeLabel::name).toArray(String[]::new);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            JavaList<?> javaList = (JavaList<?>) o;
            return nodeLabels.equals(javaList.nodeLabels);
        }

        @Override
        public int hashCode() {
            return Objects.hash(nodeLabels);
        }
    }

    private static final class Singleton<T> implements NodeLabelToken {

        private final NodeLabel item;

        private Singleton(T item, Function<T, NodeLabel> toNodeLabel) {
            this.item = toNodeLabel.apply(item);
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public int size() {
            return 1;
        }

        @Override
        public @NotNull NodeLabel get(int index) {
            assert index == 0;
            return item;
        }

        @Override
        public String[] getStrings() {
            return new String[]{item.name};
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Singleton<?> singleton = (Singleton<?>) o;
            return item.equals(singleton.item);
        }

        @Override
        public int hashCode() {
            return Objects.hash(item);
        }
    }

    private NodeLabelTokens() {}
}
