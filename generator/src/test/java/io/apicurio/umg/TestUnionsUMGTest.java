package io.apicurio.umg;

import org.junit.Test;

import java.io.File;
import java.util.List;

public class TestUnionsUMGTest extends AbstractUMGTest {

    @Test
    public void run() {

        ClassLoader classLoader = getClass().getClassLoader();
        var specifications = List.of(
                new File(classLoader.getResource("specs/json-schema.yaml").getFile())
        );

        var rootNamespace = "io.apicurio.datamodels.models";

        var outputDir = new File("/home/jsenko/projects/work/repos/github.com/Apicurio/apicurio-unified-model-generator.git/generator/src/gen/test");

        run(specifications, rootNamespace, outputDir);
    }
}
