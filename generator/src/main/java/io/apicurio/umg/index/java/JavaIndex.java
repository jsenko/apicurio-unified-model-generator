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

package io.apicurio.umg.index.java;

import io.apicurio.umg.models.concept.type.TypeModel;
import io.apicurio.umg.pipe.java.type.IJavaType;
import lombok.Getter;
import org.jboss.forge.roaster.model.source.JavaClassSource;
import org.jboss.forge.roaster.model.source.JavaEnumSource;
import org.jboss.forge.roaster.model.source.JavaInterfaceSource;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import static io.apicurio.umg.models.concept.NamespacedName.nn;

/**
 * @author eric.wittmann@gmail.com
 */
public class JavaIndex {

    @Getter
    private Map<String, JavaInterfaceSource> interfaces = new HashMap<>();

    @Getter
    private Map<String, JavaClassSource> classes = new HashMap<>();

    @Getter
    private Map<String, JavaEnumSource> enums = new HashMap<>();

    @Getter
    private Map<TypeModel, IJavaType> typeIndex = new HashMap<>();

    public JavaInterfaceSource lookupInterface(String fullyQualifiedName) {
        return this.interfaces.get(fullyQualifiedName);
    }

    public JavaClassSource lookupClass(String fullyQualifiedName) {
        return this.classes.get(fullyQualifiedName);
    }

    public JavaEnumSource lookupEnum(String fullyQualifiedName) {
        return this.enums.get(fullyQualifiedName);
    }

    public void index(JavaClassSource _class) {
        String fqcn = _class.getCanonicalName();
        this.classes.put(fqcn, _class);
    }

    public void index(JavaInterfaceSource _iface) {
        String fqcn = _iface.getCanonicalName();
        this.interfaces.put(fqcn, _iface);
    }

    public void index(JavaEnumSource _enum) {
        String fqcn = _enum.getCanonicalName();
        this.enums.put(fqcn, _enum);
    }


    public IJavaType lookupType(TypeModel typeModel) {
        return typeIndex.get(typeModel);
    }

    public IJavaType lookupOrIndex(TypeModel typeModel, Supplier<IJavaType> supplier) {
        // Cannot use computeIfAbsent for recursively
        var key = typeModel;
        IJavaType res = typeIndex.get(key);
        if (res == null) {
            res = supplier.get();
            typeIndex.put(key, res);
        }
        return res;
    }


    public void index(IJavaType javaType) {
        typeIndex.put(javaType.getTypeModel(), javaType);
    }
}
