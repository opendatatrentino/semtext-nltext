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
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.SemText;
import eu.trentorise.opendata.semtext.Term;
import eu.trentorise.opendata.semtext.nltext.SemanticStringConverter;
import eu.trentorise.opendata.semtext.nltext.UrlMapper;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ComplexConcept;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ConceptTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.InstanceTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticString;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.StringTerm;
import java.util.ArrayList;
import java.util.List;
import org.junit.After;
import static org.junit.Assert.assertEquals;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 *
 * @author David Leoni
 */
public class SemanticStringToSemTextTest {
    
    static final long TEST_CONCEPT_1_ID = 1L;
    static final long TEST_CONCEPT_2_ID = 2L;

    private SemanticStringConverter conv;

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(SemanticStringToSemTextTest.class);
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
    public void test_0() {
        SemanticString ss = new SemanticString();
        SemText st = conv.semText(ss, true);
        assertEquals(ss.getText(), null);
        assertEquals(st.getText(), "");
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void test_1() {
        SemanticString ss = new SemanticString("ciao");
        SemText st = conv.semText(ss, true);
        assertEquals(st.getText(), ss.getText());
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
        assertEquals(st.getSentences().get(0).getTerms().size(), 0);
    }

    @Test
    public void test_2() {
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
    public void test_3() {
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
    public void test_4() {
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
    public void test_5() {
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
    public void test_6() {
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
    public void test_7() {
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
    public void test_complete(){
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
}