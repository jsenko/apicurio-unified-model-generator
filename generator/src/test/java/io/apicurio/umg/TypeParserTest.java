package io.apicurio.umg;

import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;

import io.apicurio.umg.models.concept.RawType;

public class TypeParserTest {

    @Test
    public void run() throws Exception {

        RawType.parse("foo");
        RawType.parse("foo|bar");
        RawType.parse("foo|bar|baz");
        RawType.parse("{foo|bar}");
        RawType.parse("{{foo|bar}}");
        RawType.parse("{[foo]|{bar}|baz}|quox");

        Assert.assertEquals(
                RawType.builder().simple(true).simpleType("foo").build(),
                RawType.parse("foo"));

        Assert.assertEquals(
                RawType.builder().union(true).nested(List.of(
                        RawType.builder().simple(true).simpleType("foo").build(),
                        RawType.builder().simple(true).simpleType("bar").build()
                        )).build(),
                RawType.parse("foo|bar"));

        Assert.assertEquals(
                RawType.builder().union(true).nested(List.of(
                        RawType.builder().simple(true).simpleType("foo").build(),
                        RawType.builder().simple(true).simpleType("bar").build(),
                        RawType.builder().simple(true).simpleType("baz").build()
                        )).build(),
                RawType.parse("foo|bar|baz"));

        Assert.assertEquals(
                RawType.builder().map(true).nested(List.of(
                        RawType.builder().union(true).nested(List.of(
                                RawType.builder().simple(true).simpleType("foo").build(),
                                RawType.builder().simple(true).simpleType("bar").build()
                                )).build()
                        )).build(),
                RawType.parse("{foo|bar}"));

        Assert.assertEquals(
                RawType.builder().map(true).nested(List.of(
                        RawType.builder().map(true).nested(List.of(
                                RawType.builder().union(true).nested(List.of(
                                        RawType.builder().simple(true).simpleType("foo").build(),
                                        RawType.builder().simple(true).simpleType("bar").build()
                                        )).build()
                                )).build()
                        )).build(),
                RawType.parse("{{foo|bar}}"));

        Assert.assertEquals(
                RawType.builder().union(true).nested(List.of(
                        RawType.builder().map(true).nested(List.of(
                                RawType.builder().union(true).nested(List.of(
                                        RawType.builder().list(true).nested(List.of(
                                                RawType.builder().simple(true).simpleType("foo").build()
                                                )).build(),
                                        RawType.builder().map(true).nested(List.of(
                                                RawType.builder().simple(true).simpleType("bar").build()
                                                )).build(),
                                        RawType.builder().simple(true).simpleType("baz").build()
                                        )).build()
                                )).build(),
                        RawType.builder().simple(true).simpleType("quox").build()
                        )).build(),
                RawType.parse("{[foo]|{bar}|baz}|quox"));
    }
}
