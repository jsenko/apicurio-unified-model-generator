package io.apicurio.umg;

import io.apicurio.umg.io.SpecificationLoader;
import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.models.spec.SpecificationModel;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class AbstractUMGTest {

    protected void run(List<File> specifications, String rootNamespace, File outputDir) {

        Logger.info("Generating unified models from: " + specifications);

        if (specifications == null || specifications.isEmpty()) {
            throw new RuntimeException("No input specifications found.");
        }

        if (!specifications.stream().map(File::isFile).reduce((a, b) -> a && b).orElse(false)) {
            throw new RuntimeException("At least one configured specification does not exist.");
        }

        if (outputDir.isFile()) {
            throw new RuntimeException("Output directory is unexpectedly a file (should be a directory or non-existent).");
        }

        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }

        Logger.info("Generating code into: " + outputDir.getAbsolutePath());

        UnifiedModelGeneratorConfig config = UnifiedModelGeneratorConfig.builder()
                .outputDirectory(outputDir)
                .testOutputDirectory(null)
                .generateTestFixtures(false)
                .rootNamespace(rootNamespace)
                .build();

        List<SpecificationModel> specs = specifications.stream().map(SpecificationLoader::loadSpec).collect(Collectors.toUnmodifiableList());

        var generator = new UnifiedModelGenerator(config, specs);

        try {
            generator.generate();
        } catch (Exception e) {
            throw new RuntimeException("Error generating unified model classes.", e);
        }

        Logger.info("Models successfully generated.");
    }
}
