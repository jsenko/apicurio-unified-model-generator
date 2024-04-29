package io.apicurio.umg.pipe.concept;

import io.apicurio.umg.beans.UnionRule;
import io.apicurio.umg.beans.UnionRuleType;
import io.apicurio.umg.models.concept.ConceptUtils;
import io.apicurio.umg.models.concept.type.PrimitiveType;
import io.apicurio.umg.models.concept.type.RawType;
import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.concept.type.UnionType;
import io.apicurio.umg.pipe.AbstractStage;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static io.apicurio.umg.logging.Errors.assertion;
import static io.apicurio.umg.logging.Errors.fail;

/**
 * Check that all union types have a complete list of associated union rules.
 * If we find some rule missing, try to check if the sets of types for each union variant are disjoint.
 * In that case we can differentiate between e.g. boolean|string.
 * There can be some more complex cases, e.g.:
 * Schema|[Schema]
 * where
 * Schema = Entity|boolean
 * In the above case we can still determine a union rule based on types.
 * However, for the following case:
 * Schema|{Schema}
 * we can't since both an entity and a map appears as an ObjectNode,
 * and manually specified rule is needed.
 */
public class CreateTypeBasedImplicitUnionRulesStage extends AbstractStage {

    @Override
    protected void doProcess() {
        getState().getConceptIndex().getTypes().stream()
                .filter(t -> t.isUnionType())
                .map(t -> (UnionType) t)
                .filter(t -> t.getTypes().stream().anyMatch(nt -> t.getRuleFor(nt.getRawType().asRawType()) == null))
                .forEach(t -> handleRulesStage(t));
    }


    private void handleRulesStage(UnionType type) {

        // For each union type, collect a list of kinds. We'll use raw type in case we need to print an error.
        // We'll skip nested types where a rule is already defined, but warning the user about a potential issue.
        var rulesDefined = new ArrayList<RawType>();
        var rulesUndefined = new ArrayList<RawType>();

        var kinds = new HashMap<String, Set<TypeKind>>();
        type.getTypes().forEach(nt -> {
            var s = ConceptUtils.collectNestedTypesShallow(nt).stream()
                    .filter(t -> !t.isUnionType())
                    .map(t -> (Type) t)
                    .filter(t -> {
                        if (type.getRuleFor(t.getRawType().asRawType()) != null) {
                            rulesDefined.add(t.getRawType());
                            return false;
                        } else {
                            rulesUndefined.add(t.getRawType());
                            return true;
                        }
                    })
                    .map(t -> map(t))
                    .collect(Collectors.toSet());
            kinds.put(nt.getName(), s);
        });
        // Now determine if their intersection is empty. We'll just merge the sets and count them
        var merged = new HashSet<TypeKind>();
        kinds.values().stream().forEach(s -> merged.addAll(s));
        assertion(merged.size() == kinds.values().stream().map(s -> s.size()).reduce(0, Integer::sum),
                "Could not create implicit union rules for type " + type.getName() + ", " +
                        "because the nested types cannot be disambiguated by their JSON type: " + kinds + ". " +
                        "Manual union rules must be added for all union variants.");

        // Issue a warning if we have a mix of manual and implicit rules.
        if (rulesDefined.size() > 0) {
            warn("We have found an union with variants that have both manually defined (%s) and implicitly generated (%s) rules. " +
                            "The implicit rules are based only on the JSON type of the value. Make sure this behavior is correct.",
                    rulesDefined, rulesUndefined);
        }

        // Generate rules
        type.getTypes().forEach(rt -> {
            var r = new UnionRule();
            r.setUnionType(rt.getName());
            r.setRuleType(UnionRuleType.IsJsonTypes);
            r.setJsonTypes(kinds.get(rt.getName()).stream().map(k -> k.getKind()).collect(Collectors.toSet()));
            type.getUnionRules().add(r);
        });
    }

    private TypeKind map(Type t) {
        if (t.isEntityType() || t.isMapType()) {
            return TypeKind.OBJECT_NODE;
        } else if (t.isListType()) {
            return TypeKind.ARRAY_NODE;
        } else if (t.isPrimitiveType() && (t == PrimitiveType.ANY || t == PrimitiveType.OBJECT)) {
            return TypeKind.OBJECT_NODE;
        } else if (t.isPrimitiveType() && t == PrimitiveType.STRING) {
            return TypeKind.STRING_NODE;
        } else if (t.isPrimitiveType() && t == PrimitiveType.BOOLEAN) {
            return TypeKind.BOOLEAN_NODE;
        } else if (t.isPrimitiveType() && (t == PrimitiveType.NUMBER || t == PrimitiveType.INTEGER)) {
            return TypeKind.NUMBER_NODE;
        } else {
            fail("TODO");
            return null; // Unreachable
        }
    }

    private enum TypeKind {
        OBJECT_NODE("object"),
        ARRAY_NODE("array"),
        BOOLEAN_NODE("boolean"),
        STRING_NODE("string"),
        NUMBER_NODE("number");

        @Getter
        private final String kind;

        TypeKind(String kind) {
            this.kind = kind;
        }
    }
}
