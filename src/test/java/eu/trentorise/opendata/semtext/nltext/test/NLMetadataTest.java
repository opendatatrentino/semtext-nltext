/*
 * Copyright 2015 Trento Rise.
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
package eu.trentorise.opendata.semtext.nltext.test;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.NullNode;
import eu.trentorise.opendata.commons.OdtConfig;

import static eu.trentorise.opendata.commons.test.jackson.OdtJacksonTester.changeField;
import static eu.trentorise.opendata.commons.test.jackson.OdtJacksonTester.testJsonConv;
import eu.trentorise.opendata.semtext.nltext.NLMeaningMetadata;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import eu.trentorise.opendata.semtext.jackson.SemTextModule;
import it.unitn.disi.sweb.core.nlp.model.NLSenseMeaning;
import java.io.IOException;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class NLMetadataTest {

    private static final Logger LOG = Logger.getLogger(NLMetadataTest.class.getName());

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(NLMetadataTest.class);
    }

    ObjectMapper objectMapper;

    @Before
    public void before() {
        objectMapper = new ObjectMapper();

        SemTextModule.registerModulesInto(objectMapper);

        SemTextModule.clearMetadata();
    }

    @After
    public void after() {
        objectMapper = null;
        SemTextModule.clearMetadata();
    }

    @Test
    public void testNLMetadataJackson() throws IOException {
        testJsonConv(objectMapper, LOG, NLMeaningMetadata.of("a", "b"));

        String json = changeField(objectMapper, LOG, NLMeaningMetadata.of("a", "b"), "lemma", NullNode.instance);

        try {
            objectMapper.readValue(json, NLMeaningMetadata.class);
            Assert.fail("Should have failed before!");
        }
        catch (JsonMappingException ex) {

        }

    }

    @Test
    public void testMetadata() {
        assertEquals("a", NLMeaningMetadata.of("a", "b").getLemma());
        assertEquals("b", NLMeaningMetadata.of("a", "b").getSummary());

        try {
            NLMeaningMetadata.of(null, "");
            Assert.fail();
        }
        catch (NullPointerException ex) {

        }

        try {
            NLMeaningMetadata.of("", null);
            Assert.fail();
        }
        catch (NullPointerException ex) {

        }
    }

    @Test
    @SuppressWarnings({"ObjectEqualsNull", "IncompatibleEquals"})
    public void testEquality() {
        assertEquals(NLMeaningMetadata.of("a", "b"), NLMeaningMetadata.of("a", "b"));
        
        assertEquals(NLMeaningMetadata.of("a", "b").hashCode(), NLMeaningMetadata.of("a", "b").hashCode());
        assertNotEquals(NLMeaningMetadata.of("a", "b"), NLMeaningMetadata.of("a", "c"));
        assertFalse(NLMeaningMetadata.of("a", "b").equals(null));
        assertFalse(NLMeaningMetadata.of("a", "b").equals("c"));
        
    }

}
