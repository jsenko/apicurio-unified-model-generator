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
import io.apicurio.umg.models.concept.type.TypeModel;
import org.apache.commons.collections4.Trie;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * @author eric.wittmann@gmail.com
 */
public class ConceptIndex {

    private Trie<String, NamespaceModel> namespaceIndex = new PatriciaTrie<>();
    private Trie<String, TraitModel> traitIndex = new PatriciaTrie<>();
    private Trie<String, EntityModel> entityIndex = new PatriciaTrie<>();
    private Trie<String, VisitorModel> visitorIndex = new PatriciaTrie<>();
    private Map<String, PropertyModelWithOriginComparator> propertyComparatorIndex = new HashMap<>();

    private Map<String, TypeModel> typeIndex = new HashMap<>();

    // ========= Namespaces

    public void index(NamespaceModel model) {
        namespaceIndex.put(model.getName(), model);
    }

    public NamespaceModel lookupNamespace(String namespace) {
        return namespaceIndex.get(namespace);
    }

    public NamespaceModel lookupNamespace(String namespace, Function<String, NamespaceModel> factory) {
        return namespaceIndex.computeIfAbsent(namespace, (key) -> factory.apply(key));
    }

    public Collection<NamespaceModel> findNamespaces(String prefix) {
        return namespaceIndex.prefixMap(prefix).values();
    }

    public void remove(NamespaceModel namespaceModel) {
        namespaceIndex.remove(namespaceModel.getName());
    }

    // ========= Entities

    public void index(EntityModel model) {
        entityIndex.put(model.getNn().fullyQualifiedName(), model);
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

    public Collection<EntityModel> findEntities(String prefix) {
        return entityIndex.prefixMap(prefix).values();
    }

    public void remove(EntityModel entityModel) {
        entityIndex.remove(entityModel.getNn().fullyQualifiedName());
    }

    // ========= Traits

    public void index(TraitModel model) {
        traitIndex.put(model.getNn().fullyQualifiedName(), model);
    }

    public TraitModel lookupTrait(String fullyQualifiedTraitName) {
        return traitIndex.get(fullyQualifiedTraitName);
    }

    public Collection<TraitModel> findTraits(String prefix) {
        return traitIndex.prefixMap(prefix).values();
    }

    public void remove(TraitModel traitModel) {
        traitIndex.remove(traitModel.getNn().fullyQualifiedName());
    }

    // ========= Types

    public TypeModel lookupOrIndex(String name, Supplier<TypeModel> modelSupplier) {
        return typeIndex.computeIfAbsent(name, _name -> modelSupplier.get());
    }


    public void index(TypeModel model) {
        typeIndex.put(model.getName(), model);
    }

    // ========= Visitors

    public void index(VisitorModel model) {
        visitorIndex.put(model.getNamespace().fullName(), model);
    }

    public VisitorModel lookupVisitor(String namespace) {
        return visitorIndex.get(namespace);
    }

    public Collection<VisitorModel> findVisitors(String prefix) {
        return visitorIndex.prefixMap(prefix).values();
    }

    public void remove(VisitorModel visitorModel) {
        entityIndex.remove(visitorModel.getNamespace().fullName());
    }

    // ========= Property Comparators

    public void index(EntityModel model, PropertyModelWithOriginComparator comparator) {
        propertyComparatorIndex.put(model.getNn().fullyQualifiedName(), comparator);
    }

    public PropertyModelWithOriginComparator lookupPropertyComparator(EntityModel entity) {
        return lookupPropertyComparator(entity.getNn().getNamespace(), entity.getNn().getName());
    }

    public PropertyModelWithOriginComparator lookupPropertyComparator(NamespaceModel namespace, String entityName) {
        return lookupPropertyComparator(namespace.fullName(), entityName);
    }

    public PropertyModelWithOriginComparator lookupPropertyComparator(String namespace, String entityName) {
        String fqn = namespace + "." + entityName;
        return this.propertyComparatorIndex.get(fqn);
    }

    // ========= Other

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


}
