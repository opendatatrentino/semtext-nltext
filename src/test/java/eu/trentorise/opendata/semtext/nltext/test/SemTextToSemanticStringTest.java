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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.semtext.Meaning;
import eu.trentorise.opendata.semtext.MeaningKind;
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.SemText;
import eu.trentorise.opendata.semtext.Sentence;
import eu.trentorise.opendata.semtext.Term;
import eu.trentorise.opendata.semtext.nltext.NLTermMetadata;
import eu.trentorise.opendata.semtext.nltext.NLTextConverter;
import eu.trentorise.opendata.semtext.nltext.SemanticStringConverter;
import eu.trentorise.opendata.disiclient.UrlMapper;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ComplexConcept;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticString;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticTerm;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class SemTextToSemanticStringTest {

    static final long TEST_CONCEPT_ID_1 = 1L;
    static final long TEST_CONCEPT_ID_2 = 2L;
    static final long TEST_ENTITY_ID_1 = 1L;
    static final long TEST_ENTITY_ID_2 = 2L;

    private SemanticStringConverter conv;

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(SemTextToSemanticStringTest.class);
    }

    @Before
    public void beforeMethod() {
        conv = SemanticStringConverter.of(UrlMapper.of());
    }

    @After
    public void afterMethod() {
        conv = null;
    }

    @Test
    public void test1() {
        SemText st = SemText.of();
        SemanticString ss = conv.semanticString(st);
        assertEquals(ss.getText(), "");
        assertEquals(ss.getComplexConcepts().size(), 0);
    }

    @Test
    public void test2() {
        SemText st = SemText.of("ciao");
        SemanticString ss = conv.semanticString(st);
        assertEquals(ss.getText(), "ciao");
        assertEquals(ss.getComplexConcepts().size(), 0);
    }

    @Test
    public void test3() {
        List<Sentence> sentences = new ArrayList();
        sentences.add(Sentence.of(0, 4));
        SemText st = SemText.ofSentences(Locale.ITALIAN, "ciao", sentences);
        SemanticString ss = conv.semanticString(st);
        assertEquals(ss.getText(), "ciao");
        assertEquals(ss.getComplexConcepts().size(), 0);
    }

    /**
     * <pre>
     *      hello dear world
     *      11111               term1 (with NLTermMetadata)
     *            2222          term2 (without metadata)
     * </pre>
     */
    @Test
    public void test4() {
        long concId = 4;
        long entId = 5;
        String text = "hello dear world";
        List<Sentence> sentences = new ArrayList<Sentence>();

        sentences.add(Sentence.of(0, text.length(),
                Term.of(0,
                        5,
                        MeaningStatus.SELECTED,
                        Meaning.of(conv.getUrlMapper().conceptIdToUrl(concId),
                                MeaningKind.CONCEPT,
                                0.3),
                        ImmutableList.<Meaning>of(),
                        ImmutableMap.of(NLTextConverter.NLTEXT_NAMESPACE,
                                NLTermMetadata.of(ImmutableList.of("S"),
                                        ImmutableList.of("L")))),
                Term.of(6,
                        10,
                        MeaningStatus.SELECTED,
                        Meaning.of(conv.getUrlMapper().entityIdToUrl(entId),
                                MeaningKind.ENTITY,
                                0.3))));

        SemText st = SemText.ofSentences(Locale.ITALIAN, text, sentences);

        SemanticString ss = conv.semanticString(st);

        assertEquals(text, ss.getText());
        assertEquals(2, ss.getComplexConcepts().size());

        ComplexConcept cc_1 = ss.getComplexConcepts().get(0);
        SemanticTerm t1 = cc_1.getTerms().get(0);
        assertEquals(1, cc_1.getTerms().size());
        assertEquals(1, t1.getConceptTerms().size());
        assertEquals((long) t1.getConceptTerms().get(0).getValue(),
                concId);
        assertEquals("L", t1.getStringTerms().get(0).getValue());

        ComplexConcept cc_2 = ss.getComplexConcepts().get(1);
        SemanticTerm t2 = cc_2.getTerms().get(0);

        assertEquals(1, cc_2.getTerms().size());
        assertEquals(1, t2.getInstanceTerms().size());
        assertEquals((long) t2.getInstanceTerms().get(0).getValue(),
                entId);
        assertEquals("dear", t2.getStringTerms().get(0).getValue());

    }

    /**
     * One Term, one selected meaning and one other meaning with empty id.
     */
    @Test
    public void testSelectedPlusEmptyMeaningId() {

        Meaning selMeaning = Meaning.of(conv.getUrlMapper().entityIdToUrl(TEST_ENTITY_ID_1), MeaningKind.ENTITY, 1);

        Meaning otherMeaning = Meaning.of("", MeaningKind.ENTITY, 1);

        SemText semText = SemText.of(
                Locale.ITALIAN,
                "c",
                Term.of(
                        0,
                        1,
                        MeaningStatus.SELECTED,
                        selMeaning,
                        ImmutableList.of(otherMeaning)));
        SemanticString semanticString = conv.semanticString(semText);
        assertEquals(1, semanticString.getComplexConcepts().size());
        assertEquals(1, semanticString.getComplexConcepts().get(0).getTerms().size());
        assertEquals(1, semanticString.getComplexConcepts().get(0).getTerms().get(0).getInstanceTerms().size());
        assertEquals(TEST_ENTITY_ID_1, (long) semanticString.getComplexConcepts().get(0).getTerms().get(0).getInstanceTerms().get(0).getValue());

    }
    
    /**
     * One Term, no selected meaning and one other meaning with empty id.
     */
    @Test
    public void testNonSelectedPlusEmptyMeaningId() {
        
        Meaning otherMeaning = Meaning.of("", MeaningKind.ENTITY, 1);

        SemText semText = SemText.of(
                Locale.ITALIAN,
                "c",
                Term.of(
                        0,
                        1,
                        MeaningStatus.TO_DISAMBIGUATE,
                        null,
                        ImmutableList.of(otherMeaning)));
        SemanticString semanticString = conv.semanticString(semText);
        assertEquals(1, semanticString.getComplexConcepts().size());
        assertEquals(1, semanticString.getComplexConcepts().get(0).getTerms().size());
        assertEquals(0, semanticString.getComplexConcepts().get(0).getTerms().get(0).getInstanceTerms().size());
        assertEquals(1, semanticString.getComplexConcepts().get(0).getTerms().get(0).getStringTerms().size());        
    }
    

}
