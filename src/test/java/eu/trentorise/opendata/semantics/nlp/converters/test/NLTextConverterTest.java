package eu.trentorise.opendata.semantics.nlp.converters.test;

import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.semantics.nlp.converters.NLTextConverter;
import eu.trentorise.opendata.semantics.nlp.converters.UrlMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import it.unitn.disi.sweb.core.nlp.model.NLMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLSenseMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLSentence;
import it.unitn.disi.sweb.core.nlp.model.NLText;
import it.unitn.disi.sweb.core.nlp.model.NLTextUnit;
import it.unitn.disi.sweb.core.nlp.model.NLToken;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import eu.trentorise.opendata.semantics.nlp.model.MeaningKind;
import eu.trentorise.opendata.semantics.nlp.model.Meaning;
import eu.trentorise.opendata.semantics.nlp.model.SemText;
import eu.trentorise.opendata.semantics.nlp.model.Term;
import it.unitn.disi.sweb.core.nlp.model.NLMultiWord;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

/**
 *
 * @author David Leoni
 */
public class NLTextConverterTest {

    static final long TEST_CONCEPT_1_ID = 1L;
    static final long TEST_CONCEPT_2_ID = 2L;
    static final long TEST_CONCEPT_3_ID = 3L;

    private NLTextConverter conv;    
    
    @BeforeClass
    public static void beforeClass(){
        OdtConfig.of(NLTextConverterTest.class).loadLogConfig();        
    }
    
    @Before
    public void beforeMethod(){
        conv = NLTextConverter.of(UrlMapper.of("entities/", "concepts/"));
    }
    
    @After
    public void afterMethod(){
        conv = null;
    }
    
    /**
     * tests one token
     */
    @Test
    public void testNLTextToSemText_1() {
        NLText nlText = new NLText("ciao");

        SemText st = conv.semText(nlText);
        assertEquals(st.getText(), nlText.getText());
        assertEquals(Locale.ROOT, st.getLocale());
    }

    /**
     * one token in sentence, multiple meanings
     */
    @Test
    public void testNLTextToSemText_2() {

        String text = "hello dear Trento";

        NLText nltext = new NLText(text);
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, "startOffset", 0);
        sentence.setProp(NLTextUnit.PFX, "endOffset", text.length());

        String dearTerm = "dear";

        Set<NLMeaning> meanings = new HashSet<NLMeaning>();

        NLSenseMeaning sm1 = new NLSenseMeaning("testLemma1", 5L, "NOUN", TEST_CONCEPT_1_ID, 4, 1, "test description");
        // score must be set manually here, although on server will be computed from senseRank and senseFrequency
        sm1.setScore(1);
        sm1.setProbability((float) (1.0 / 6.0));
        meanings.add(sm1);

        NLSenseMeaning sm2 = new NLSenseMeaning("testLemma2", 6L, "NOUN", TEST_CONCEPT_2_ID, 4, 1, "test description");
        sm2.setScore(5);
        sm2.setProbability((float) (5.0 / 6.0));
        meanings.add(sm2);
                

        NLToken firstToken = new NLToken(dearTerm, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 6);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 10);

        
        NLSenseMeaning nlSelectedMeaning = new NLSenseMeaning("testLemma3", 6L, "NOUN", TEST_CONCEPT_3_ID, 4, 1, "test description");
        nlSelectedMeaning.setScore(5);
        nlSelectedMeaning.setProbability((float) (5.0 / 6.0));        
        
        firstToken.setSelectedMeaning(nlSelectedMeaning);
        
        sentence.addToken(firstToken);
        nltext.addSentence(sentence);

        SemText st = conv.semText(nltext);
        

        assertEquals(text, st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);
        assertEquals(2, term.getMeanings().size());
        assertEquals(conv.getUrlMapper().conceptIdToUrl(TEST_CONCEPT_3_ID), term.getSelectedMeaning().getId());

    }

    /**
     * one token in sentence with null id for concept
     */
    @Test
    public void testNLTextToSemTextNoMeaning() {

        String text = "hello dear Trento";

        NLText nltext = new NLText(text);
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, "startOffset", 0);
        sentence.setProp(NLTextUnit.PFX, "endOffset", text.length());

        String dearTerm = "dear";

        Set<NLMeaning> meanings = new HashSet<NLMeaning>();

        NLSenseMeaning sm1 = new NLSenseMeaning("testLemma1", null, "NOUN", null, 4, 1, "test description");
        // score must be set manually here, although on server will be computed from senseRank and senseFrequency
        sm1.setScore(1);
        sm1.setProbability((float) (1.0 / 6.0));
        meanings.add(sm1);

        NLToken firstToken = new NLToken(dearTerm, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 6);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 10);

        sentence.addToken(firstToken);
        nltext.addSentence(sentence);

        SemText st = conv.semText(nltext);

        assertEquals(st.getText(), text);
        assertEquals(st.getSentences().size(), 1);
        assertEquals(st.getSentences().get(0).getTerms().size(), 1);
        Term word = st.getSentences().get(0).getTerms().get(0);
        assertEquals(1, word.getMeanings().size());        
        assertNull(word.getSelectedMeaning());
        Meaning m = word.getMeanings().get(0);
        assertEquals("", m.getId());
        assertEquals(MeaningKind.CONCEPT, m.getKind());
    }

    /**
     * tests two overlapping tokens
     */
    @Test
    public void testNLTextToSemText_3() {

        String text = "abc";

        NLText nltext = new NLText(text);
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, "startOffset", 0);
        sentence.setProp(NLTextUnit.PFX, "endOffset", text.length());

        String text_1 = "ab";
        String text_2 = "bc";

        Set<NLMeaning> meanings = new HashSet<NLMeaning>();

        NLSenseMeaning sm1 = new NLSenseMeaning("testLemma1", 5L, "NOUN", TEST_CONCEPT_1_ID, 4, 1, "test description");
        // score must be set manually here, although on server will be computed from senseRank and senseFrequency
        sm1.setScore(1);
        sm1.setProbability((float) (1.0 / 6.0));
        meanings.add(sm1);

        NLSenseMeaning sm2 = new NLSenseMeaning("testLemma2", 6L, "NOUN", TEST_CONCEPT_2_ID, 4, 1, "test description");
        sm2.setScore(5);
        sm2.setProbability((float) (5.0 / 6.0));
        meanings.add(sm2);

        NLToken firstToken = new NLToken(text_1, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 0);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 2);

        sentence.addToken(firstToken);

        NLToken secondToken = new NLToken(text_2, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 1);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 3);
        sentence.addToken(secondToken);

        nltext.addSentence(sentence);

        SemText st = conv.semText(nltext);        

        assertEquals(text, st.getText() );
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);
        assertEquals(2, term.getMeanings().size());
        
        assertEquals(null, term.getSelectedMeaning());        
    }

    /**
     * tests two overlapping multiwords
     */
    @Test
    public void testNLTextToSemText_4() {

        String text = "abcd";

        NLText nltext = new NLText(text);
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, "startOffset", 0);
        sentence.setProp(NLTextUnit.PFX, "endOffset", text.length());

        // mw 1 = "abc"
        // mw 2 = "bcd"
        String text_1 = "ab";
        String text_2 = "bc";
        String text_3 = "cd";

        Set<NLMeaning> meanings = new HashSet<NLMeaning>();

        NLSenseMeaning sm1 = new NLSenseMeaning("testLemma1", 5L, "NOUN", TEST_CONCEPT_1_ID, 4, 1, "test description");
        // score must be set manually here, although on server will be computed from senseRank and senseFrequency
        sm1.setScore(1);
        sm1.setProbability((float) (1.0 / 6.0));
        meanings.add(sm1);

        NLSenseMeaning sm2 = new NLSenseMeaning("testLemma2", 6L, "NOUN", TEST_CONCEPT_2_ID, 4, 1, "test description");
        sm2.setScore(5);
        sm2.setProbability((float) (5.0 / 6.0));
        meanings.add(sm2);

        NLToken firstToken = new NLToken(text_1, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 0);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 2);

        sentence.addToken(firstToken);

        NLToken secondToken = new NLToken(text_2, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 1);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 3);
        sentence.addToken(secondToken);

        List<NLToken> toksMw_1 = new ArrayList();
        toksMw_1.add(firstToken);
        toksMw_1.add(secondToken);

        List<String> toksMwString_1 = new ArrayList();
        toksMwString_1.add(text_1);
        toksMwString_1.add(text_2);

        sentence.addMultiWord(new NLMultiWord("abc", toksMw_1, toksMwString_1));

        NLToken thirdToken = new NLToken(text_3, meanings);
        firstToken.setProp(NLTextUnit.PFX, "sentenceStartOffset", 2);
        firstToken.setProp(NLTextUnit.PFX, "sentenceEndOffset", 4);

        List<NLToken> toksMw_2 = new ArrayList();
        toksMw_1.add(secondToken);
        toksMw_1.add(thirdToken);

        List<String> toksMwString_2 = new ArrayList();
        toksMwString_1.add(text_2);
        toksMwString_1.add(text_3);

        sentence.addMultiWord(new NLMultiWord("bcd", toksMw_2, toksMwString_2));
        
                
        // todo use NLNamedEntity, discuss with Gabor, Simon why NLNamedEntity 
        // constructor is different than ones for NLMultiWord

        sentence.addToken(thirdToken);

        nltext.addSentence(sentence);

        SemText st = conv.semText(nltext);        

        assertEquals(text, st.getText());
        assertEquals(1, st.getSentences().size() );
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals(2, term.getMeanings().size());
        assertEquals(null, term.getSelectedMeaning());
    }

    

}
