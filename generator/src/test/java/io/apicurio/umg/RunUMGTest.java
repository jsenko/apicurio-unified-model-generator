package io.apicurio.umg;

import io.apicurio.umg.io.SpecificationLoader;
import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.models.spec.SpecificationModel;
import org.junit.Test;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class RunUMGTest {

    private List<File> specifications;

    File outputDir;

    String rootNamespace;

    @Test
    public void run() throws Exception {

        rootNamespace = "io.apicurio.datamodels.models";

        ClassLoader classLoader = getClass().getClassLoader();
        specifications = List.of(
                //new File(classLoader.getResource("specs/openapi.yaml").getFile()),
                //new File(classLoader.getResource("specs/asyncapi.yaml").getFile())
                //new File(classLoader.getResource("specs/json-schema.yaml").getFile())
                new File(classLoader.getResource("specs/test.yaml").getFile())
        );

        outputDir = new File("/home/jsenko/projects/work/repos/github.com/Apicurio/apicurio-unified-model-generator.git/generator/src/gen");

        Logger.info("Generating unified models from: " + specifications);
        if (specifications == null || specifications.isEmpty()) {
            throw new RuntimeException("No input specifications found.");
        }
        if (!specifications.stream().map(specFile -> specFile.isFile()).reduce((a, b) -> a && b)
                .orElse(false)) {
            throw new RuntimeException("At least one configured specification does not exist.");
        }

        if (outputDir.isFile()) {
            throw new RuntimeException(
                    "Output directory is unexpectedly a file (should be a directory or non-existent).");
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        // Add the output directory as a compile source.
        Logger.info("Generating code into: " + outputDir.getAbsolutePath());

        // Create config
        UnifiedModelGeneratorConfig config = UnifiedModelGeneratorConfig.builder()
                .outputDirectory(outputDir)
                .testOutputDirectory(null)
                .generateTestFixtures(false)
                .rootNamespace(rootNamespace).build();
        // Load the specs
        List<SpecificationModel> specs = loadSpecifications();
        // Create a unified model generator
        UnifiedModelGenerator generator = new UnifiedModelGenerator(config, specs);
        // Generate the source code into the target output directory.
        try {
            generator.generate();
        } catch (Exception e) {
            throw new RuntimeException("Error generating unified model classes.", e);
        }

        Logger.info("Models successfully generated.");
    }

    /**
     * Loads the configured specifications.
     */
    private List<SpecificationModel> loadSpecifications() {
        return this.specifications.stream().map(file -> SpecificationLoader.loadSpec(file))
                .collect(Collectors.toUnmodifiableList());
    }
}
