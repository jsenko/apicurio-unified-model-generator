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

import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.models.spec.SpecificationModel;
import io.apicurio.umg.pipe.GeneratorState;
import io.apicurio.umg.pipe.Pipeline;
import io.apicurio.umg.pipe.concept.*;
import io.apicurio.umg.pipe.java.*;

import java.util.Collection;

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
        pipe.addStage(new CreatePropertyAndTypeModelsStage());
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

        pipe.addStage(new CreateTypeBasedImplicitUnionRulesStage());

        // Debug the models
        //pipe.addStage(new DebugStage());

        // Generate java code
        /*
         * Wrap (concept) types in their Java equivalents.
         */
        pipe.addStage(new CreateJavaTypesStage());

        pipe.addStage(new LoadBaseClassesStage());
        pipe.addStage(new CreateModelTypeStage());

        pipe.addStage(new CreateTraitInterfacesStage());
        pipe.addStage(new CreateTraitOrEntityInterfacesStage());
        pipe.addStage(new ConfigureInterfaceParentStage());
        pipe.addStage(new ConfigureInterfaceTraitsStage());

        pipe.addStage(new CreateUnionInterfacesStage());
        pipe.addStage(new CreatePrimitiveUnionValuesStage());
        pipe.addStage(new CreateCollectionUnionValuesStage());
        pipe.addStage(new CreateUnionInterfaceMethodsStage());
        pipe.addStage(new ApplyUnionInterfacesToTypesStage());

        pipe.addStage(new CreateEntityClassesStage());



        pipe.addStage(new CreateMethodsStage());
        pipe.addStage(new CreateFieldsStage());

        // TODO We need entity classes, move that around?
        pipe.addStage(new CreateUnionClassMethodsStage());

        pipe.addStage(new ApplyNodeOrMappedNodeStage());
        pipe.addStage(new ApplyRootNodeStage());

        // Generate Java IO code

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
