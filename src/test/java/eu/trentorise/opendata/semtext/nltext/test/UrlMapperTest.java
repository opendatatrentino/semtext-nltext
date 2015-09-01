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
import eu.trentorise.opendata.disiclient.UrlMapper;
import org.junit.After;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Leoni
 */
public class UrlMapperTest {

    private static final Logger LOG = LoggerFactory.getLogger(UrlMapperTest.class);

    public static String BASE = "http://entitybase.org";
    private UrlMapper m;

    @Before
    public void before() {
        m = UrlMapper.of(BASE);
    }

    @After
    public void after() {
        m = null;
    }

    @Test
    public void testAttrDef() {
        assertEquals(1L, m.attrDefUrlToId(m.attrDefIdToUrl(1L, 2L)));
        assertEquals(2L, m.attrDefUrlToConceptId(m.attrDefIdToUrl(1L, 2L)));
        assertEquals(-1L, m.attrDefUrlToId(m.attrDefIdToUrl(-1L, -1L)));
        try {
            m.attrDefIdToUrl(-1L, 5L);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }

        try {
            m.attrDefIdToUrl(null, 5L);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }

        try {
            m.attrDefIdToUrl(1L, null);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }
    }

    @Test
    public void testConcept() {
        assertEquals(1L, m.conceptUrlToId(m.conceptIdToUrl(1L)));        
        assertEquals(-1L, m.conceptUrlToId(m.conceptIdToUrl(-1L)));

        try {
            m.conceptIdToUrl(null);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }

    }

    @Test
    public void testEtype() {
        assertEquals(1L, m.etypeUrlToId(m.etypeIdToUrl(1L)));        
        assertEquals(-1L, m.etypeUrlToId(m.etypeIdToUrl(-1L)));
        try {
            m.etypeIdToUrl(null);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }

       
    }

    @Test
    public void testEntity() {
        assertEquals(1L, m.entityUrlToId(m.entityIdToUrl(1L)));        
        assertEquals(-1L, m.entityUrlToId(m.entityIdToUrl(-1L)));
        assertEquals(1L, m.entityNewUrlToId(m.entityNewIdToUrl(1L)));
        assertEquals(-1L, m.entityNewUrlToId(m.entityNewIdToUrl(-1L)));

        try {
            m.entityIdToUrl(null);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }
        
        try {
            m.entityNewIdToUrl(null);
            Assert.fail();
        }
        catch (IllegalArgumentException ex) {

        }
        

    }

}
