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

package io.apicurio.umg;

import java.util.Collection;

import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.models.spec.SpecificationModel;
import io.apicurio.umg.pipe.GeneratorState;
import io.apicurio.umg.pipe.Pipeline;
import io.apicurio.umg.pipe.concept.CreateEntityModelsStage;
import io.apicurio.umg.pipe.concept.CreateImplicitUnionRulesStage;
import io.apicurio.umg.pipe.concept.CreateNamespaceModelsStage;
import io.apicurio.umg.pipe.concept.CreateParentTraitsStage;
import io.apicurio.umg.pipe.concept.CreatePropertyComparatorStage;
import io.apicurio.umg.pipe.concept.CreatePropertyModelsStage;
import io.apicurio.umg.pipe.concept.CreateTraitModelsStage;
import io.apicurio.umg.pipe.concept.CreateVisitorsStage;
import io.apicurio.umg.pipe.concept.ExpandPropertyOrderStage;
import io.apicurio.umg.pipe.concept.IndexSpecificationsStage;
import io.apicurio.umg.pipe.concept.NormalizeEntitiesStage;
import io.apicurio.umg.pipe.concept.NormalizePropertiesStage;
import io.apicurio.umg.pipe.concept.NormalizeTraitsStage;
import io.apicurio.umg.pipe.concept.NormalizeVisitorsStage;
import io.apicurio.umg.pipe.concept.RemoveTransparentTraitsStage;
import io.apicurio.umg.pipe.concept.ResolveVisitorEntityStage;
import io.apicurio.umg.pipe.concept.SpecificationValidationStage;
import io.apicurio.umg.pipe.java.ApplyUnionTypesStage;
import io.apicurio.umg.pipe.java.ConfigureInterfaceParentStage;
import io.apicurio.umg.pipe.java.ConfigureInterfaceTraitsStage;
import io.apicurio.umg.pipe.java.CreateAcceptMethodStage;
import io.apicurio.umg.pipe.java.CreateAllNodeVisitorStage;
import io.apicurio.umg.pipe.java.CreateCombinedVisitorInterfacesStage;
import io.apicurio.umg.pipe.java.CreateEmptyCloneMethodStage;
import io.apicurio.umg.pipe.java.CreateEntityImplementationsStage;
import io.apicurio.umg.pipe.java.CreateEntityInterfacesStage;
import io.apicurio.umg.pipe.java.CreateImplFieldsStage;
import io.apicurio.umg.pipe.java.CreateImplMethodsStage;
import io.apicurio.umg.pipe.java.CreateInterfaceMethodsStage;
import io.apicurio.umg.pipe.java.CreateModelTypeStage;
import io.apicurio.umg.pipe.java.CreateReaderDispatchersStage;
import io.apicurio.umg.pipe.java.CreateReaderFactoryStage;
import io.apicurio.umg.pipe.java.CreateReadersStage;
import io.apicurio.umg.pipe.java.CreateTestFixturesStage;
import io.apicurio.umg.pipe.java.CreateTraitInterfacesStage;
import io.apicurio.umg.pipe.java.CreateTraversersStage;
import io.apicurio.umg.pipe.java.CreateUnionTypeValuesStage;
import io.apicurio.umg.pipe.java.CreateUnionTypesStage;
import io.apicurio.umg.pipe.java.CreateUnionValueMethodsStage;
import io.apicurio.umg.pipe.java.CreateVisitorAdaptersStage;
import io.apicurio.umg.pipe.java.CreateVisitorInterfacesStage;
import io.apicurio.umg.pipe.java.CreateWriterDispatchersStage;
import io.apicurio.umg.pipe.java.CreateWriterFactoryStage;
import io.apicurio.umg.pipe.java.CreateWritersStage;
import io.apicurio.umg.pipe.java.JavaWriteStage;
import io.apicurio.umg.pipe.java.LoadBaseClassesStage;
import io.apicurio.umg.pipe.java.OrganizeImportsStage;
import io.apicurio.umg.pipe.java.RemoveUnusedImportsStage;

/**
 * @author eric.wittmann@gmail.com
 */
public class UnifiedModelGenerator {

    private final UnifiedModelGeneratorConfig config;
    private final Collection<SpecificationModel> specifications;

    /**
     * Constructor.
     *
     * @param config
     * @param specifications
     */
    public UnifiedModelGenerator(UnifiedModelGeneratorConfig config, Collection<SpecificationModel> specifications) {
        this.config = config;
        this.specifications = specifications;
    }

    /**
     * Generates the output from the given list of specifications.
     */
    public void generate() throws Exception {
        Logger.info("Output directory: %s", config.getOutputDirectory().getAbsolutePath());

        GeneratorState state = new GeneratorState();
        state.setSpecifications(specifications);
        state.setConfig(config);
        Pipeline pipe = new Pipeline();

        // Index phase
        pipe.addStage(new IndexSpecificationsStage());
        pipe.addStage(new ExpandPropertyOrderStage());
        pipe.addStage(new SpecificationValidationStage());

        // Model creation phase
        pipe.addStage(new CreateNamespaceModelsStage());
        pipe.addStage(new CreateTraitModelsStage());
        pipe.addStage(new CreateEntityModelsStage());
        pipe.addStage(new CreatePropertyModelsStage());
        pipe.addStage(new CreateParentTraitsStage());
        pipe.addStage(new CreateVisitorsStage());

        // Implicit model creation phase
        pipe.addStage(new CreateImplicitUnionRulesStage());

        // Model optimization phase
        pipe.addStage(new RemoveTransparentTraitsStage());
        pipe.addStage(new NormalizeTraitsStage());
        pipe.addStage(new NormalizeEntitiesStage());
        pipe.addStage(new NormalizePropertiesStage());
        pipe.addStage(new NormalizeVisitorsStage());
        pipe.addStage(new ResolveVisitorEntityStage());
        pipe.addStage(new CreatePropertyComparatorStage());

        // Debug the models
        //pipe.addStage(new DebugStage());

        // Generate java code
        pipe.addStage(new LoadBaseClassesStage());
        pipe.addStage(new CreateModelTypeStage());

        pipe.addStage(new CreateTraitInterfacesStage());
        pipe.addStage(new CreateEntityInterfacesStage());
        pipe.addStage(new ConfigureInterfaceParentStage());
        pipe.addStage(new ConfigureInterfaceTraitsStage());

        pipe.addStage(new CreateUnionTypeValuesStage());
        pipe.addStage(new CreateUnionTypesStage());
        pipe.addStage(new ApplyUnionTypesStage());

        pipe.addStage(new CreateInterfaceMethodsStage());
        pipe.addStage(new CreateEntityImplementationsStage());
        pipe.addStage(new CreateImplFieldsStage());
        pipe.addStage(new CreateImplMethodsStage());
        pipe.addStage(new CreateUnionValueMethodsStage());

        pipe.addStage(new CreateReadersStage());
        pipe.addStage(new CreateWritersStage());
        pipe.addStage(new CreateVisitorInterfacesStage());
        pipe.addStage(new CreateAcceptMethodStage());
        pipe.addStage(new CreateEmptyCloneMethodStage());
        pipe.addStage(new CreateCombinedVisitorInterfacesStage());
        pipe.addStage(new CreateVisitorAdaptersStage());
        pipe.addStage(new CreateAllNodeVisitorStage());
        pipe.addStage(new CreateReaderDispatchersStage());
        pipe.addStage(new CreateWriterDispatchersStage());
        pipe.addStage(new CreateTraversersStage());
        pipe.addStage(new CreateReaderFactoryStage());
        pipe.addStage(new CreateWriterFactoryStage());

        pipe.addStage(new RemoveUnusedImportsStage());
        pipe.addStage(new OrganizeImportsStage());
        pipe.addStage(new JavaWriteStage());

        // Generate tests
        pipe.addStage(new CreateTestFixturesStage());

        pipe.run(state);
    }
}
