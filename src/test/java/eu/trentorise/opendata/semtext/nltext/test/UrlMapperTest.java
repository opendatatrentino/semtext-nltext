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
import eu.trentorise.opendata.semtext.nltext.UrlMapper;
import static org.junit.Assert.assertEquals;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class UrlMapperTest {
    
    @BeforeClass
    public static void beforeClass(){
        OdtConfig.of(UrlMapperTest.class).loadLogConfig();        
    }
    
    @Test
    public void testNullPrefixes(){
        try{
            UrlMapper.of(null, "");
        } catch(NullPointerException ex){
            
        }
        try{
            UrlMapper.of("", null);
        } catch(NullPointerException ex){
            
        }
                        
    }

    @Test
    public void testEmptyPrefixes(){
        UrlMapper um = UrlMapper.of("","");
        assertEquals("3",um.entityIdToUrl(3L));
        assertEquals(3,um.urlToEntityId("3"));
        assertEquals("3",um.conceptIdToUrl(3L));
        assertEquals(3,um.urlToConceptId("3"));        
    }    
    
    @Test
    public void testNonEmptyPrefixes(){
        UrlMapper um = UrlMapper.of("a","b");        
        assertEquals("a3",um.entityIdToUrl(3L));
        assertEquals(3,um.urlToEntityId("a3"));
        assertEquals("b3",um.conceptIdToUrl(3L));
        assertEquals(3,um.urlToConceptId("b3"));        
    }
}
