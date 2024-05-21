/*
 * Copyright 2021 JBoss Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.apicurio.umg.index.concept;

import io.apicurio.umg.models.concept.*;
import io.apicurio.umg.models.concept.type.EntityType;
import io.apicurio.umg.models.concept.type.Type;
import io.apicurio.umg.models.concept.typelike.TraitTypeLike;
import io.apicurio.umg.models.concept.typelike.TypeLike;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static io.apicurio.umg.logging.Errors.assertion;

/**
 * @author eric.wittmann@gmail.com
 */
public class ConceptIndex {

    private Trie<String, NamespaceModel> namespaceIndex = new PatriciaTrie<>();
    private Trie<String, TraitModel> traitIndex = new PatriciaTrie<>();
    private Trie<String, EntityModel> entityIndex = new PatriciaTrie<>();
    private Trie<String, VisitorModel> visitorIndex = new PatriciaTrie<>();
    private Map<String, PropertyModelWithOriginComparator> propertyComparatorIndex = new HashMap<>();

    private final Map<String, TypeLike> typeIndex = new HashMap<>();

    // ========= Types

    public void index(Type model) {
        var key = model.getNamespace() + "." + model.getName();
        typeIndex.put(key, model);
    }

    public <T extends TypeLike> T lookupOrIndex(T newType) {
        // Optimize later
        var key = newType.getNamespace() + "." + newType.getName();
        TypeLike type = typeIndex.get(key);
        if (type == null) {
            type = newType;
            typeIndex.put(key, type);
        }
        return (T) type;
    }

    public <T extends TypeLike> T lookupOrIndex(String namespace, String name, Supplier<T> typeSupplier) {
        var key = namespace + "." + name;
        TypeLike type = typeIndex.get(key);
        if (type == null) {
            type = typeSupplier.get();
            typeIndex.put(key, type);
        }
        return (T) type;
    }

    public Type requireType(String namespace, String name) {
        var key = namespace + "." + name;
        var type = typeIndex.get(key);
        assertion(type != null, "Type %s.%s not found.", namespace, name);
        assertion(type instanceof Type, "This TypeLike is not a type: %s", type);
        return (Type) type;
    }

    public Collection<TypeLike> getTypes() {
        return typeIndex.values();
    }

    public Stream<EntityType> getEntityTypes() {
        return getTypes().stream().filter(TypeLike::isEntityType).map(t -> (EntityType) t);
    }

    public Stream<TraitTypeLike> getTraitTypeLikes() {
        return getTypes().stream().filter(TypeLike::isTraitTypeLike).map(t -> (TraitTypeLike) t);
    }

    // =========

    public void remove(TraitModel traitModel) {
        traitIndex.remove(traitModel.fullyQualifiedName());
    }

    public void remove(EntityModel entityModel) {
        entityIndex.remove(entityModel.fullyQualifiedName());
    }

    public void remove(NamespaceModel namespaceModel) {
        namespaceIndex.remove(namespaceModel.getName());
    }

    public void remove(VisitorModel visitorModel) {
        entityIndex.remove(visitorModel.getNamespace().fullName());
    }


    public boolean hasNamespace(String name) {
        return namespaceIndex.containsKey(name);
    }

    public boolean hasTrait(String fullyQualifiedTraitName) {
        return traitIndex.containsKey(fullyQualifiedTraitName);
    }

    public boolean hasEntity(String fullyQualifiedEntityName) {
        return entityIndex.containsKey(fullyQualifiedEntityName);
    }

    public boolean hasVisitor(String namespace) {
        return namespaceIndex.containsKey(namespace);
    }


    public void index(NamespaceModel model) {
        namespaceIndex.put(model.getName(), model);
    }

    public void index(TraitModel model) {
        traitIndex.put(model.fullyQualifiedName(), model);
    }

    public void index(EntityModel model) {
        entityIndex.put(model.fullyQualifiedName(), model);
    }

    public void index(VisitorModel model) {
        visitorIndex.put(model.getNamespace().fullName(), model);
    }

    public void index(EntityModel model, PropertyModelWithOriginComparator comparator) {
        propertyComparatorIndex.put(model.fullyQualifiedName(), comparator);
    }

    public NamespaceModel lookupNamespace(String namespace) {
        return namespaceIndex.get(namespace);
    }

    public NamespaceModel lookupNamespace(String namespace, Function<String, NamespaceModel> factory) {
        return namespaceIndex.computeIfAbsent(namespace, (key) -> factory.apply(key));
    }

    public TraitModel lookupTrait(String fullyQualifiedTraitName) {
        return traitIndex.get(fullyQualifiedTraitName);
    }

    public EntityModel lookupEntity(String fullyQualifiedEntityName) {
        return entityIndex.get(fullyQualifiedEntityName);
    }

    public EntityModel lookupEntity(String namespace, String entityName) {
        return lookupEntity(namespace + "." + entityName);
    }

    public EntityModel lookupEntity(NamespaceModel namespace, String entityName) {
        return lookupEntity(namespace.fullName(), entityName);
    }

    public VisitorModel lookupVisitor(String namespace) {
        return visitorIndex.get(namespace);
    }

    public PropertyModelWithOriginComparator lookupPropertyComparator(EntityModel entity) {
        return lookupPropertyComparator(entity.getNamespace(), entity.getName());
    }

    public PropertyModelWithOriginComparator lookupPropertyComparator(NamespaceModel namespace, String entityName) {
        return lookupPropertyComparator(namespace.fullName(), entityName);
    }

    public PropertyModelWithOriginComparator lookupPropertyComparator(String namespace, String entityName) {
        String fqn = namespace + "." + entityName;
        return this.propertyComparatorIndex.get(fqn);
    }


    public Collection<NamespaceModel> findNamespaces(String prefix) {
        return namespaceIndex.prefixMap(prefix).values();
    }

    public Collection<TraitModel> findTraits(String prefix) {
        return traitIndex.prefixMap(prefix).values();
    }

    public Collection<EntityModel> findEntities(String prefix) {
        return entityIndex.prefixMap(prefix).values();
    }

    public Collection<VisitorModel> findVisitors(String prefix) {
        return visitorIndex.prefixMap(prefix).values();
    }

    public Collection<EntityModel> getAllEntitiesWithCopy() {
        return new HashSet<>(findEntities(""));
    }

    public Collection<TraitModel> getAllTraitsWithCopy() {
        return new HashSet<>(findTraits(""));
    }

    /**
     * Gets a list of all properties for the given entity.  This includes any inherited properties and
     * any properties from Traits.
     *
     * @param entityModel
     */
    public Collection<PropertyModelWithOrigin> getAllEntityProperties(EntityModel entityModel) {
        EntityModel model = entityModel;
        PropertyModelWithOriginComparator propertyComparator = lookupPropertyComparator(entityModel);
        final Set<PropertyModelWithOrigin> models = propertyComparator == null ? new HashSet<>() : new TreeSet<>(propertyComparator);
        while (model != null) {
            final EntityModel _entity = model;
            models.addAll(model.getProperties().values().stream().map(property -> PropertyModelWithOrigin.builder().property(property).origin(_entity).build()).collect(Collectors.toList()));

            // Also include properties from all traits.
            model.getTraits().forEach(trait -> {
                TraitModel t = trait;
                while (t != null) {
                    final TraitModel _trait = t;
                    models.addAll(t.getProperties().values().stream().map(property -> PropertyModelWithOrigin.builder().property(property).origin(_trait).build()).collect(Collectors.toList()));
                    t = t.getParent();
                }
            });

            model = (EntityModel) model.getParent();
        }
        return models;
    }

    /**
     * Given a starting namespace and an entity name, search up the entity hierarchy for
     * entities matching the name.  Returns the common-most one.
     *
     * @param namespace
     * @param entityName
     */
    public EntityModel lookupCommonEntity(String namespace, String entityName) {
        NamespaceModel nsModel = lookupNamespace(namespace);
        EntityModel entity;
        do {
            String entityFQN = nsModel.fullName() + "." + entityName;
            entity = lookupEntity(entityFQN);
            nsModel = nsModel.getParent();
        } while (lookupEntity(nsModel, entityName) != null);
        return entity;
    }

}
