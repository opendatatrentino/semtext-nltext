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

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.semtext.nltext.SemanticStringConverter;
import eu.trentorise.opendata.semtext.nltext.UrlMapper;
import eu.trentorise.opendata.semtext.SemText;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticString;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class SemanticStringConverterTest {

    static final long TEST_CONCEPT_1_ID = 1L;
    static final long TEST_CONCEPT_2_ID = 2L;
    

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(SemanticStringConverterTest.class);
    }


    @Test
    public void example() {
        SemanticStringConverter conv = SemanticStringConverter.of(
                UrlMapper.of("http://mysite.org/entities/",
                        "http://mysite.org/concepts/"));
                // when creating semtext, string ids will have these prefixes 
        // followed by the numerical ids of found in semantic strings

        // by setting true as second parameter we are instructing the converter 
        // that we suppose the SemanticString meanings have been reviewed by a human. 
        SemText semtext = conv.semText(new SemanticString("ciao"), true);
        
        SemanticString semstring = conv.semanticString(SemText.of("ciao"));
    }

    
    
    
}
