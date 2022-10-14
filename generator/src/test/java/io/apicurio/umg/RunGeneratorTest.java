package io.apicurio.umg;

import io.apicurio.umg.beans.Specification;
import io.apicurio.umg.io.SpecificationLoader;
import io.apicurio.umg.logging.Logger;
import io.apicurio.umg.main.Main;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class RunGeneratorTest {

    @Test
    public void run() throws Exception {

        var specs = loadSpecs();
        var generator = UnifiedModelGenerator.create(specs);
        generator.generateInto(getTargetDir());
        Logger.info("Model generated successfully!");
    }

    public File getTargetDir() {
        String relPath = getClass().getProtectionDomain().getCodeSource().getLocation().getFile();
        System.out.println(relPath);
        File targetDir = new File(relPath + "../../target/generated-models");
        if (targetDir.exists()) {
            var d = targetDir.delete();
            System.err.println("Dir deleted " + d);
        } else {
            System.err.println("Dir does not exists " + targetDir);
        }
        targetDir.mkdir();
        return targetDir;
    }

    private static List<Specification> loadSpecs() {
        Logger.info("Loading specifications.");
        List<Specification> specs = new ArrayList<>();
        specs.add(SpecificationLoader.loadSpec("specifications/openapi/openapi-2.0.x.yaml", Main.class.getClassLoader()));
        specs.add(SpecificationLoader.loadSpec("specifications/openapi/openapi-3.0.x.yaml", Main.class.getClassLoader()));
        specs.add(SpecificationLoader.loadSpec("specifications/openapi/openapi-3.1.x.yaml", Main.class.getClassLoader()));
        specs.add(SpecificationLoader.loadSpec("specifications/asyncapi/asyncapi-2.0.x.yaml", Main.class.getClassLoader()));
        specs.add(SpecificationLoader.loadSpec("specifications/json-schema/json-schema-2020-12.yaml", Main.class.getClassLoader()));
        return specs;
    }
}
