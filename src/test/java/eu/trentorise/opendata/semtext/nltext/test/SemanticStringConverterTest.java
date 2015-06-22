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
import eu.trentorise.opendata.semtext.nltext.SemanticStringConverter;
import eu.trentorise.opendata.semtext.nltext.UrlMapper;
import eu.trentorise.opendata.semtext.Meaning;
import eu.trentorise.opendata.semtext.MeaningKind;
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.SemText;
import eu.trentorise.opendata.semtext.Sentence;
import eu.trentorise.opendata.semtext.Term;
import eu.trentorise.opendata.semtext.nltext.NLTermMetadata;
import eu.trentorise.opendata.semtext.nltext.NLTextConverter;
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
        OdtConfig.init(SemanticStringConverterTest.class);
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

    @Test
    public void testSemanticStringToSemText_0() {
        SemanticString ss = new SemanticString();
        SemText st = conv.semText(ss, true);
        assertEquals(ss.getText(), null);
        assertEquals(st.getText(), "");
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_1() {
        SemanticString ss = new SemanticString("ciao");
        SemText st = conv.semText(ss, true);
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
        SemText st = conv.semText(ss, true);
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
        SemText st = conv.semText(ss, true);
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
        SemText st = conv.semText(ss, true);
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
        SemanticString ss = new SemanticString("hello dear world", ccs);
        SemText st = conv.semText(ss, true);
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
        SemanticString ss = new SemanticString("hello dear world", ccs);
        SemText st = conv.semText(ss, true);
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
        SemanticString ss = new SemanticString("hello dear world", ccs);
        SemText st = conv.semText(ss, true);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void testSemanticStringToSemText_complete(){
        semanticStringToSemText_complete(true);
        semanticStringToSemText_complete(false);
    }

    public void semanticStringToSemText_complete(boolean checkedByUser) {
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
        it.setWeight(5.0);        
        entityTerms.add(it);

        List<StringTerm> stringTerms = new ArrayList<StringTerm>();

        sts.add(new SemanticTerm("dear", 6, concTerms, stringTerms, entityTerms));
        ccs.add(new ComplexConcept(sts));
        SemanticString ss = new SemanticString("hello dear world", ccs);
        SemText st = conv.semText(ss, checkedByUser);

        assertEquals(ss.getText(), st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term t = st.getSentences().get(0).getTerms().get(0);
        assertEquals(6, t.getStart());
        assertEquals(10, t.getEnd());
        assertEquals(2, t.getMeanings().size());
        if (checkedByUser){
            assertEquals(MeaningStatus.REVIEWED, t.getMeaningStatus());
        } else {
            assertEquals(MeaningStatus.SELECTED, t.getMeaningStatus());
        }
        
        assertEquals(conv.getUrlMapper().entityIdToUrl(2L), t.getSelectedMeaning().getId());
       
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
    public void testSemTextToSemanticString_4() {
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
}
