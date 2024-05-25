package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.models.concept.type.RawType;
import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.concept.type.UnionType;
import io.apicurio.umg.models.concept.typelike.TypeLike;
import io.apicurio.umg.pipe.AbstractStage;

import java.util.*;
import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;

public class NormalizeUnionsStage extends AbstractStage {

    @Override
    protected void doProcess() {

        var queue = new ArrayDeque<UnionType>();

        getState().getConceptIndex().getTypes().stream()
                .filter(TypeLike::isUnionType)
                .forEach(t -> queue.add((UnionType) t));

        while (!queue.isEmpty()) {
            var type = queue.remove();

            var siblings = getState().getConceptIndex().getTypes().stream()
                    .filter(TypeLike::isUnionType)
                    .map(t -> (UnionType) t)
                    .filter(t -> hasSameNameAndNamespaceLevel(type, t))
                    .collect(Collectors.toSet());

            if (siblings.size() > 1) {
                @SuppressWarnings("unchecked")
                var intersection = (Set<Type>) (Set<?>) intersection(siblings.stream().map(UnionType::getTypes).toArray(List[]::new));
                if (!intersection.isEmpty()) {
                    // Parent's raw type
                    // TODO: Name vs. raw type?
                    var rt = RawType.parse(intersection.stream().map(t -> t.getName()).collect(Collectors.joining("|")));
                    // Parent's union rules
                    // TODO: Do we need union rules her?
                    // Root type: We require all sibling be roots.
                    if (type.isRoot()) {
                        assertion(siblings.stream().allMatch(t -> t.isRoot()));
                    } else {
                        assertion(siblings.stream().noneMatch(t -> t.isRoot()));
                    }
                    // Create a new parent
                    var parent = UnionType.builder()
                            .name(type.getName())
                            .namespace(parentNamespace(type.getNamespace()))
                            .rawType(rt)
                            .leaf(false)
                            .root(type.isRoot())
                            .build();
                    assertion(!getState().getConceptIndex().getTypes().contains(parent));
                    getState().getConceptIndex().index(parent);
                    // Process children
                    siblings.forEach(t -> {
                        assertion(t.getParent() == null);
                        t.setParent(parent);
                    });
                }
            }
            queue.removeAll(siblings);
        }
        System.err.println();
    }


    // TODO: Make namespace a separate class.
    private static boolean hasSameNameAndNamespaceLevel(TypeLike left, TypeLike right) {
        return left.getName().equals(right.getName()) && parentNamespace(left.getNamespace()).equals(right.getNamespace());
    }

    private static String parentNamespace(String ns) {
        if (ns.length() != 0) {
            var parts = ns.split("\\.");
            if (parts.length > 1) {
                return String.join(".", Arrays.copyOf(parts, parts.length - 1));
            }
        }
        return "";
    }

    @SafeVarargs
    private static <T> Set<T> intersection(Collection<T>... collections) {
        if (collections.length == 0) {
            return Set.of();
        }
        var res = new HashSet<T>();
        if (collections.length == 1) {
            res.addAll(collections[0]);
            return res;
        }
        var first = collections[0];
        for (T t : first) {
            boolean ok = true;
            for (int i = 1; i < collections.length; i++) {
                ok = ok && collections[i].contains(t);
            }
            if (ok) {
                res.add(t);
            }
        }
        return res;
    }
}
