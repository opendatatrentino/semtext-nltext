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
package eu.trentorise.opendata.semantics.nlp.converters.test;

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.semantics.nlp.converters.SemanticStringConverter;
import eu.trentorise.opendata.semantics.nlp.converters.UrlMapper;
import eu.trentorise.opendata.semantics.nlp.model.Meaning;
import eu.trentorise.opendata.semantics.nlp.model.MeaningKind;
import eu.trentorise.opendata.semantics.nlp.model.MeaningStatus;
import eu.trentorise.opendata.semantics.nlp.model.SemText;
import eu.trentorise.opendata.semantics.nlp.model.Sentence;
import eu.trentorise.opendata.semantics.nlp.model.Term;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ComplexConcept;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ConceptTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.InstanceTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticString;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.StringTerm;
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
public class SemanticStringConverterTest {

    static final long TEST_CONCEPT_1_ID = 1L;
    static final long TEST_CONCEPT_2_ID = 2L;

    private SemanticStringConverter conv;

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.of(SemanticStringConverterTest.class).loadLogConfig();
    }

    @Before
    public void beforeMethod() {
        conv = SemanticStringConverter.of(UrlMapper.of("entities/", "concepts/"));
    }

    @After
    public void afterMethod() {
        conv = null;
    }

    @Test
    public void testSemanticStringToSemText_0() {
        SemanticString ss = new SemanticString();
        SemText st = conv.semText(ss);
        assertEquals(ss.getText(), null);
        assertEquals(st.getText(), "");
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_1() {
        SemanticString ss = new SemanticString("ciao");
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_2() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        ccs.add(new ComplexConcept());
        SemanticString ss = new SemanticString("ciao", ccs);
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_3() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        List<SemanticTerm> sts = new ArrayList<SemanticTerm>();
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("ciao", ccs);
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_4() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        List<SemanticTerm> sts = new ArrayList<SemanticTerm>();
        sts.add(new SemanticTerm());
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("ciao", ccs);
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_5() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        List<SemanticTerm> sts = new ArrayList<SemanticTerm>();
        sts.add(new SemanticTerm("dear", 6));
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("hello dear Refine", ccs);
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_6() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        List<SemanticTerm> sts = new ArrayList<SemanticTerm>();

        List<ConceptTerm> concTerms = new ArrayList<ConceptTerm>();
        List<InstanceTerm> entityTerms = new ArrayList<InstanceTerm>();
        List<StringTerm> stringTerms = new ArrayList<StringTerm>();

        sts.add(new SemanticTerm("dear", 6, concTerms, stringTerms, entityTerms));
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("hello dear Refine", ccs);
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_7() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        List<SemanticTerm> sts = new ArrayList<SemanticTerm>();

        List<ConceptTerm> concTerms = new ArrayList<ConceptTerm>();
        concTerms.add(new ConceptTerm());

        List<InstanceTerm> entityTerms = new ArrayList<InstanceTerm>();
        entityTerms.add(new InstanceTerm());

        List<StringTerm> stringTerms = new ArrayList<StringTerm>();

        sts.add(new SemanticTerm("dear", 6, concTerms, stringTerms, entityTerms));
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("hello dear Refine", ccs);
        SemText st = conv.semText(ss);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_8() {
        List<ComplexConcept> ccs = new ArrayList<ComplexConcept>();
        List<SemanticTerm> sts = new ArrayList<SemanticTerm>();

        List<ConceptTerm> concTerms = new ArrayList<ConceptTerm>();
        ConceptTerm ct = new ConceptTerm();
        ct.setValue(1L);
        ct.setWeight(0.1);
        concTerms.add(ct);

        List<InstanceTerm> entityTerms = new ArrayList<InstanceTerm>();
        InstanceTerm it = new InstanceTerm();
        it.setValue(2L);
        ct.setWeight(5.0);
        entityTerms.add(it);

        List<StringTerm> stringTerms = new ArrayList<StringTerm>();

        sts.add(new SemanticTerm("dear", 6, concTerms, stringTerms, entityTerms));
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("hello dear Refine", ccs);
        SemText st = conv.semText(ss);

        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 1);
        Term t = st.getSentences().get(0).getTerms().get(0);
        assertEquals(t.getStart(), 6);
        assertEquals(t.getEnd(), 10);
        assertEquals(t.getMeanings().size(), 2);
        // assertNotEquals(w.getSelectedMeaning(), null);

    }

    @Test
    public void testSemTextToSemanticString_1() {
        SemText st = SemText.of();
        SemanticString ss = conv.semanticString(st);
        assertEquals(ss.getText(), "");
        assertEquals(ss.getComplexConcepts().size(), 0);
    }

    @Test
    public void testSemTextToSemanticString_2() {
        SemText st = SemText.of("ciao");
        SemanticString ss = conv.semanticString(st);
        assertEquals(ss.getText(), "ciao");
        assertEquals(ss.getComplexConcepts().size(), 0);
    }

    @Test
    public void testSemTextToSemanticString_3() {
        List<Sentence> sentences = new ArrayList<Sentence>();
        sentences.add(Sentence.of(0, 4));
        SemText st = SemText.ofSentences("ciao", Locale.ITALIAN, sentences);
        SemanticString ss = conv.semanticString(st);
        assertEquals(ss.getText(), "ciao");
        assertEquals(ss.getComplexConcepts().size(), 0);
    }

    @Test
    public void testSemTextToSemanticString_4() {
        long concID = 4;
        String text = "hello dear Refine";
        List<Sentence> sentences = new ArrayList<Sentence>();

        sentences.add(Sentence.of(0, text.length(),
                Term.of(6,
                        10,
                        MeaningStatus.SELECTED,
                        Meaning.of(conv.getUrlMapper().conceptIdToUrl(concID),
                                MeaningKind.CONCEPT,
                                0.3))));

        SemText st = SemText.ofSentences(text, Locale.ITALIAN, sentences);

        SemanticString ss = conv.semanticString(st);

        assertEquals(text, ss.getText());
        assertEquals(1, ss.getComplexConcepts().size());
        assertEquals( 1, ss.getComplexConcepts().get(0).getTerms().size());
        assertEquals(1, ss.getComplexConcepts().get(0).getTerms().get(0).getConceptTerms().size());
        assertEquals((long) ss.getComplexConcepts().get(0).getTerms().get(0).getConceptTerms().get(0).getValue(),
                concID);
    }
}
