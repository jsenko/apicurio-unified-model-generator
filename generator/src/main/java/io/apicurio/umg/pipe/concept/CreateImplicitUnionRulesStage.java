package io.apicurio.umg.pipe.concept;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import io.apicurio.umg.beans.UnionRule;
import io.apicurio.umg.beans.UnionRuleType;
import io.apicurio.umg.models.concept.EntityModel;
import io.apicurio.umg.models.concept.NamespaceModel;
import io.apicurio.umg.models.concept.PropertyModel;
import io.apicurio.umg.models.concept.PropertyModelWithOrigin;
import io.apicurio.umg.models.concept.RawType;
import io.apicurio.umg.models.concept.type.EntityTypeModel;
import io.apicurio.umg.models.concept.type.TypeModel;
import io.apicurio.umg.models.concept.type.UnionTypeModel;
import io.apicurio.umg.pipe.AbstractStage;

/**
 * Stage responsible for creating implicit union rules.  A union rule is needed
 * when a union type is ambiguous.  This can happen when a union type contains
 * (for example) multiple Entity types.  Something like this:
 *
 * 'Widget|Doodad'
 *
 * In this case, the generated reader needs to know how to decide whether the
 * content it's reading is a Widget or a Doodad, since it might be either.  The
 * spec YAML file can optionally explicitly configure the rules, or we can
 * automatically figure them out in some cases.  This stage tries to figure them
 * out when they are missing.
 *
 * @author eric.wittmann@gmail.com
 */
public class CreateImplicitUnionRulesStage extends AbstractStage {

    @Override
    protected void doProcess() {
        getState().getConceptIndex().getTypes().stream()
                .filter(TypeModel::isUnionType)
                .map(type -> (UnionTypeModel) type)
                .filter(type -> type.getUnionRules() == null || type.getUnionRules().isEmpty())
                .filter(type -> isUnionAmbiguous(type))
                .forEach(union -> createImplicitUnionRules(union));
    }


    private boolean isUnionAmbiguous(UnionTypeModel union) {
        int entityCount = 0;
        int arrayCount = 0;
        int mapCount = 0;
        for (RawType nestedType : union.getRawType().getNested()) {
            if (nestedType.isMap()) {
                mapCount++;
            } else if (nestedType.isList()) {
                arrayCount++;
            } else if (nestedType.isEntityType()) {
                entityCount++;
            }
        }
        if (arrayCount > 1) {
            throw new RuntimeException("No union rules exist to dis-ambiguate multiple list types.");
        }
        if (mapCount > 0 && entityCount > 0) {
            throw new RuntimeException("No union rules exist to dis-ambiguate map types from entity types.");
        }
        return entityCount > 1;
    }

    /**
     * Create implicit union rules for the union type.  This is only called if we actually need it.
     */
    private void createImplicitUnionRules(UnionTypeModel union) {
        final int minimumRulesRequired = union.getTypes().size() - 1;
        int rulesCreated = 0;
        for (TypeModel nestedType : union.getTypes()) {
            // We only support entity types
            EntityTypeModel nestedEntityType = (EntityTypeModel) nestedType;
            UnionRule rule = createImplicitRuleForEntity(nestedEntityType.getEntity().getNn().getNamespace(), nestedType.getRawType().getSimpleType());
            if (rule != null) {
                List<UnionRule> unionRules = union.getUnionRules();
                if (unionRules == null) {
                    unionRules = new ArrayList<>();
                    union.setUnionRules(unionRules);
                }
                unionRules.add(rule);
                rulesCreated++;
            }
        }

        if (rulesCreated < minimumRulesRequired) {
            fail("Failed to create appropriate implicit union rules for union: %s", union);
        }
    }

    /**
     * Creates an implicit union rule for the given entity type.  An implicit rule can be determined in
     * one of the following ways:
     * <p>
     * 1) The entity definition has a "discriminator" property associated with it.  This tells us the
     * property (and optional property value) to use for discrimination.
     * 2) The entity definition has only one property.  The existence of that property is used for
     * discrimination.
     */
    private UnionRule createImplicitRuleForEntity(NamespaceModel nsContext, String entityName) {
        EntityModel entity = getState().getConceptIndex().lookupEntity(nsContext, entityName);
        Collection<PropertyModelWithOrigin> entityProperties = getState().getConceptIndex().getAllEntityProperties(entity);

        // #1 : look for discriminator
        List<PropertyModel> discriminators = entityProperties.stream()
                .filter(property -> property.getProperty().getDiscriminator() != null)
                .map(property -> property.getProperty()).collect(Collectors.toList());
        if (discriminators.size() == 1) {
            UnionRule rule = new UnionRule();
            PropertyModel discriminatorProperty = discriminators.get(0);
            String discriminatorPropertyName = discriminatorProperty.getName();
            String discriminatorPropertyValue = discriminatorProperty.getDiscriminator();
            rule.setUnionType(entityName);
            rule.setPropertyName(discriminatorPropertyName);
            if ("*".equals(discriminatorPropertyValue)) {
                rule.setRuleType(UnionRuleType.IsJsonObjectWithPropertyName);
            } else {
                rule.setRuleType(UnionRuleType.IsJsonObjectWithPropertyValue);
                rule.setPropertyJsonValue(discriminatorPropertyValue);
            }
            return rule;
        } else if (discriminators.size() > 1) {
            throw new RuntimeException("Found multiple union type discriminators for entity: " + nsContext.fullName() + "." + entityName);
        }

        // #2 : Entity has a single property.
        if (entityProperties.size() == 1) {
            PropertyModel singleProperty = entityProperties.iterator().next().getProperty();
            UnionRule rule = new UnionRule();
            rule.setUnionType(entityName);
            rule.setPropertyName(singleProperty.getName());
            rule.setRuleType(UnionRuleType.IsJsonObjectWithPropertyName);
            return rule;
        }

        // Return null if we couldn't create an implicit rule.
        return null;
    }
}
