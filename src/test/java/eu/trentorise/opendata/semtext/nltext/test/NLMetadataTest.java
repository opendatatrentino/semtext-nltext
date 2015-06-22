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
import com.google.common.collect.ImmutableList;
import eu.trentorise.opendata.commons.OdtConfig;

import static eu.trentorise.opendata.commons.test.jackson.OdtJacksonTester.changeField;
import static eu.trentorise.opendata.commons.test.jackson.OdtJacksonTester.testJsonConv;
import eu.trentorise.opendata.semtext.nltext.NLMeaningMetadata;
import java.util.logging.Logger;
import static org.junit.Assert.assertEquals;
import eu.trentorise.opendata.semtext.jackson.SemTextModule;
import eu.trentorise.opendata.semtext.nltext.NLTermMetadata;
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
    public void testNLTokenJackson() throws IOException {
        testJsonConv(objectMapper, LOG, NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")));

        String json = changeField(  objectMapper, 
                                     LOG, 
                                     NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")),
                                    "stems", 
                                    NullNode.instance);

        try {
            objectMapper.readValue(json, NLTermMetadata.class);
            Assert.fail("Should have failed before!");
        }
        catch (JsonMappingException ex) {

        }

    }
    

    @Test
    public void testNLTokenMetadata() {
        assertEquals(ImmutableList.of("S"), NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")).getStems());
        assertEquals(ImmutableList.of("L"), NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")).getDerivedLemmas());        

        try {
            NLTermMetadata.of(null, ImmutableList.<String>of());
            Assert.fail();
        }
        catch (NullPointerException ex) {

        }

        try {
            NLTermMetadata.of(ImmutableList.<String>of(), null);
            Assert.fail();
        }
        catch (NullPointerException ex) {

        }
        
    }
    
    @Test
    @SuppressWarnings({"ObjectEqualsNull", "IncompatibleEquals"})
    public void testNLMeaningEquality() {
        assertEquals(NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")),                
                    NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")));
        
        assertEquals(NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")).hashCode(), 
                NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")).hashCode());
        
        assertNotEquals(NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")), 
                NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("G")));
        assertFalse(NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")).equals(null));
        assertFalse(NLTermMetadata.of(ImmutableList.of("S"), ImmutableList.of("L")).equals("c"));
        
    }    
    
    @Test
    public void testNLMeaningMetadata() {
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
    public void testNLTokenEquality() {
        assertEquals(NLMeaningMetadata.of("a", "b"), NLMeaningMetadata.of("a", "b"));
        
        assertEquals(NLMeaningMetadata.of("a", "b").hashCode(), NLMeaningMetadata.of("a", "b").hashCode());
        assertNotEquals(NLMeaningMetadata.of("a", "b"), NLMeaningMetadata.of("a", "c"));
        assertFalse(NLMeaningMetadata.of("a", "b").equals(null));
        assertFalse(NLMeaningMetadata.of("a", "b").equals("c"));
        
    }

}
