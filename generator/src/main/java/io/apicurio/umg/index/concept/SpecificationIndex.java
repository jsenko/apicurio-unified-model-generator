/*
 * Copyright 2020 JBoss Inc
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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import io.apicurio.umg.beans.Entity;
import io.apicurio.umg.beans.Property;
import io.apicurio.umg.beans.SpecificationVersion;
import io.apicurio.umg.beans.Trait;
import io.apicurio.umg.models.concept.PropertyType;
import io.apicurio.umg.models.concept.SpecificationVersionId;
import io.apicurio.umg.models.spec.SpecificationModel;
import lombok.Getter;

/**
 * Maintains an index of all specifications.
 *
 * @author eric.wittmann@gmail.com
 */
public class SpecificationIndex {

    private Collection<SpecificationModel> specifications = new HashSet<>();

    private Collection<SpecificationVersion> specificationVersions = new HashSet<>();

    @Getter
    private Map<SpecificationVersionId, SpecificationModel> specIndex = new HashMap<>();

    @Getter
    private Map<String, Trait> traitIndex = new HashMap<>();

    @Getter
    private Map<String, Entity> entityIndex = new HashMap<>();

    @Getter
    private Map<String, Property> typeAliasIndex = new HashMap<>();

    @Getter
    private Map<String, String> prefixToNS = new HashMap<>();

    @Getter
    private Map<String, String> nsToPrefix = new HashMap<>();

    public void index(SpecificationModel spec) {
        specifications.add(spec);
        prefixToNS.put(spec.getPrefix(), spec.getNamespace());
        nsToPrefix.put(spec.getNamespace(), spec.getPrefix());
        spec.getVersions().forEach(specVer -> {
            specificationVersions.add(specVer);
            specIndex.put(SpecificationVersionId.create(specVer), spec);
            prefixToNS.put(specVer.getPrefix(), specVer.getNamespace());
            nsToPrefix.put(specVer.getNamespace(), specVer.getPrefix());
            specVer.getTraits().forEach(trait -> indexTrait(specVer, trait));
            specVer.getEntities().forEach(entity -> indexEntity(specVer, entity));
            specVer.getTypeAliases().forEach(alias -> indexTypeAlias(specVer, alias));
        });
    }

    public void indexTypeAlias(SpecificationVersion specVersion, Property model) {
        if(!PropertyType.parse(model.getName()).isSimple()) {
            throw new IllegalStateException("Property alias name must be a simple property type");
        }
        String key = specVersion.getNamespace() + "." + model.getName();
        typeAliasIndex.put(key, model);
    }

    public void indexEntity(SpecificationVersion specVersion, Entity model) {
        String key = specVersion.getNamespace() + "." + model.getName();
        entityIndex.put(key, model);
    }

    public void indexTrait(SpecificationVersion specVersion, Trait model) {
        String key = specVersion.getNamespace() + "." + model.getName();
        traitIndex.put(key, model);
    }

    public Collection<SpecificationModel> getAllSpecifications() {
        return Collections.unmodifiableCollection(specifications);
    }

    public Collection<SpecificationVersion> getAllSpecificationVersions() {
        return Collections.unmodifiableCollection(specificationVersions);
    }

    public String prefixForNS(String namespace) {
        return nsToPrefix.get(namespace);
    }

}
