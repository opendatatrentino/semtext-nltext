package eu.trentorise.opendata.semtext.nltext.test;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.trentorise.opendata.commons.Dict;
import eu.trentorise.opendata.commons.OdtConfig;
import eu.trentorise.opendata.semtext.nltext.NLTextConverter;
import eu.trentorise.opendata.semtext.nltext.UrlMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import it.unitn.disi.sweb.core.nlp.model.NLMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLSenseMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLSentence;
import it.unitn.disi.sweb.core.nlp.model.NLText;
import it.unitn.disi.sweb.core.nlp.model.NLTextUnit;
import it.unitn.disi.sweb.core.nlp.model.NLToken;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import org.junit.Test;

import eu.trentorise.opendata.semtext.MeaningKind;
import eu.trentorise.opendata.semtext.Meaning;
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.SemText;
import eu.trentorise.opendata.semtext.Term;
import eu.trentorise.opendata.semtext.nltext.NLMeaningMetadata;
import static eu.trentorise.opendata.semtext.nltext.NLTextConverter.END_OFFSET;
import static eu.trentorise.opendata.semtext.nltext.NLTextConverter.SENTENCE_END_OFFSET;
import static eu.trentorise.opendata.semtext.nltext.NLTextConverter.SENTENCE_START_OFFSET;
import static eu.trentorise.opendata.semtext.nltext.NLTextConverter.START_OFFSET;
import it.unitn.disi.sweb.core.nlp.model.NLEntityMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLMultiWord;
import it.unitn.disi.sweb.core.nlp.model.NLNamedEntity;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
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

    static final long TEST_ENTITY_1_ID = 11L;
    static final long TEST_ENTITY_2_ID = 22L;
    static final long TEST_ENTITY_3_ID = 33L;

    static final String TEST_LEMMA_1 = "testLemma1";
    static final String TEST_LEMMA_2 = "testLemma2";
    static final String TEST_LEMMA_3 = "testLemma3";

    static final String TEST_DESCRIPTION_1 = "testDescription1";
    static final String TEST_DESCRIPTION_2 = "testDescription2";
    static final String TEST_DESCRIPTION_3 = "testDescription3";

    private NLTextConverter conv;

    @BeforeClass
    public static void beforeClass() {
        OdtConfig.init(NLTextConverterTest.class);
    }

    @Before
    public void beforeMethod() {
        conv = NLTextConverter.of(UrlMapper.of("entities/", "concepts/"));
    }

    @After
    public void afterMethod() {
        conv = null;
    }

    /**
     * Just text and no tokens
     */
    @Test
    public void testJustText() {
        NLText nlText = new NLText("ciao");

        SemText st = conv.semText(nlText, true);
        assertEquals(st.getText(), nlText.getText());
        assertEquals(Locale.ROOT, st.getLocale());
    }

    private void setSynLemmas(NLMeaning meaning, String... lemmas) {
        meaning.setProp(NLTextUnit.PFX, NLTextConverter.SYNONYMOUS_LEMMAS, Arrays.asList(lemmas));
    }

    /**
     * one token in sentence, multiple meanings, one selected meaning
     *
     * <pre>
     * 0123
     * abc
     *  b
     * </pre>
     */
    @Test
    public void testSingleToken() {

        NLSenseMeaning sm1 = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 1.0f / 6.0f);
        setSynLemmas(sm1, "a", "b");

        NLSenseMeaning sm2 = nlSenseMeaning(TEST_LEMMA_2, TEST_DESCRIPTION_2, TEST_CONCEPT_2_ID, 5.0f / 6.0f);

        NLSenseMeaning nlSelectedMeaning = nlSenseMeaning(TEST_LEMMA_3, TEST_DESCRIPTION_3, TEST_CONCEPT_3_ID, 5.0f / 6.0f);

        NLText nltext = nlText("abc", nlToken(1, 2, nlSelectedMeaning, sm1, sm2));

        SemText st = conv.semText(nltext, true);

        assertEquals("abc", st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals(2, term.getMeanings().size());
        assertEquals(conv.getUrlMapper().conceptIdToUrl(TEST_CONCEPT_3_ID), term.getSelectedMeaning().getId());

        Meaning secondMeaning = term.getMeanings().get(1);
        assertEquals(conv.getUrlMapper().conceptIdToUrl(TEST_CONCEPT_1_ID),
                secondMeaning.getId());
        assertEquals(3, secondMeaning.getName().strings(Locale.ROOT).size());
        assertEquals(TEST_LEMMA_1, secondMeaning.getName().strings(Locale.ROOT).get(0));
        assertEquals("a", secondMeaning.getName().strings(Locale.ROOT).get(1));
        assertEquals("b", secondMeaning.getName().strings(Locale.ROOT).get(2));

    }

    /**
     * one token in sentence with null id for concept
     */
    @Test
    public void testNLTextToSemTextNoMeaning() {

        String text = "hello dear Trento";

        NLText nltext = new NLText(text);
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, START_OFFSET, 0);
        sentence.setProp(NLTextUnit.PFX, END_OFFSET, text.length());

        String dearTerm = "dear";

        Set<NLMeaning> meanings = new HashSet<NLMeaning>();

        NLSenseMeaning sm1 = new NLSenseMeaning(TEST_LEMMA_1, null, "NOUN", null, 4, 1, "test description");
        // score must be set manually here, although on server will be computed from senseRank and senseFrequency
        sm1.setScore(1);
        sm1.setProbability((float) (1.0 / 6.0));
        meanings.add(sm1);

        NLToken firstToken = new NLToken(dearTerm, meanings);
        firstToken.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET, 6);
        firstToken.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET, 10);

        sentence.addToken(firstToken);
        nltext.addSentence(sentence);

        SemText st = conv.semText(nltext, true);

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
        sentence.setProp(NLTextUnit.PFX, START_OFFSET, 0);
        sentence.setProp(NLTextUnit.PFX, END_OFFSET, text.length());

        String text_1 = "ab";
        String text_2 = "bc";

        Set<NLMeaning> meanings = new HashSet<NLMeaning>();

        NLSenseMeaning sm1 = new NLSenseMeaning(TEST_LEMMA_1, 5L, "NOUN", TEST_CONCEPT_1_ID, 4, 1, "test description");
        // score must be set manually here, although on server will be computed from senseRank and senseFrequency
        sm1.setScore(1);
        sm1.setProbability((float) (1.0 / 6.0));
        meanings.add(sm1);

        NLSenseMeaning sm2 = new NLSenseMeaning(TEST_LEMMA_2, 6L, "NOUN", TEST_CONCEPT_2_ID, 4, 1, "test description");
        sm2.setScore(5);
        sm2.setProbability((float) (5.0 / 6.0));
        meanings.add(sm2);

        NLToken firstToken = new NLToken(text_1, meanings);
        firstToken.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET, 0);
        firstToken.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET, 2);

        sentence.addToken(firstToken);

        NLToken secondToken = new NLToken(text_2, meanings);
        firstToken.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET, 1);
        firstToken.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET, 3);
        sentence.addToken(secondToken);

        nltext.addSentence(sentence);

        SemText st = conv.semText(nltext, true);

        assertEquals(text, st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);
        assertEquals(2, term.getMeanings().size());

        assertEquals(null, term.getSelectedMeaning());
    }

    /**
     * Return san NLToken with the given parameters.
     *
     * @param start sentenceStartOffset
     * @param end sentenceEndOffset
     * @param selMeaning
     * @param meanings an other meaning in the meaning lists.
     */
    private static NLToken nlToken(int start, int end, @Nullable NLMeaning selMeaning, NLMeaning... meanings) {
        NLToken ret = new NLToken();
        ret.setSelectedMeaning(selMeaning);
        ret.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET, start);
        ret.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET, end);
        ret.setMeanings(Sets.newHashSet(meanings));
        return ret;
    }

    /**
     * Returns an NLText with a sentence that spans the whole text.
     *
     * @param tokens will be changed by setting the correct text
     */
    private static NLText nlText(String text, NLToken... tokens) {

        NLText ret = new NLText(text);
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, START_OFFSET, 0);
        sentence.setProp(NLTextUnit.PFX, END_OFFSET, text.length());

        for (NLToken tok : tokens) {
            tok.setText(ret.getText()
                    .substring(
                            (Integer) tok.getProp(NLTextUnit.PFX, SENTENCE_START_OFFSET),
                            (Integer) tok.getProp(NLTextUnit.PFX, SENTENCE_END_OFFSET)));

        }
        sentence.setTokens(Arrays.asList(tokens));
        ret.addSentence(sentence);

        return ret;
    }

    private static NLEntityMeaning nlEntityMeaning(String lemma, String description, long entityId, float probability) {
        NLEntityMeaning ret = new NLEntityMeaning();
        ret.setLemma(lemma);
        ret.setProbability(probability);
        ret.setDescription(description);
        ret.setObjectID(entityId);
        return ret;
    }

    /**
     * Returns an nlsense meaning. 
     * 
     * @param gloss The gloss is put in {@link NLTextConverter#GLOSS_MAP} field with Italian locale. (description is left to null)
     */
    private static NLSenseMeaning nlSenseMeaning(String lemma, String gloss, long conceptId, float probability) {
        NLSenseMeaning ret = new NLSenseMeaning();
        ret.setLemma(lemma);
        ret.setProbability(probability);

        Map<String, String> glosses = new HashMap();
        glosses.put("it", gloss);
        ret.setProp(NLTextUnit.PFX, NLTextConverter.GLOSS_MAP, glosses);
        ret.setConceptId(conceptId);
        ret.setObjectID(conceptId);
        ret.setProbability(probability);
        return ret;
    }

    /**
     * one multiword with one token, no meanings, no selected meaning. Result
     * will still have one meaning with just the kind and no id.
     */
    @Test
    public void testSingleMultiWordNoMeanings() {
        NLToken tok = nlToken(0, 2, null);

        NLText nltext = nlText("abc", tok);
        nltext.getSentences().get(0).addMultiWord(multiword(null, tok));

        SemText st = conv.semText(nltext, true);
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals(1, term.getMeanings().size());
        assertEquals(MeaningKind.CONCEPT, term.getMeanings().get(0).getKind());
        assertEquals("", term.getMeanings().get(0).getId());
    }

    @Test
    public void testTesterMethods() {
        NLSenseMeaning sm1 = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 1.0f / 6.0f);

        NLSenseMeaning sm2 = nlSenseMeaning(TEST_LEMMA_2, TEST_DESCRIPTION_2, TEST_CONCEPT_2_ID, 5.0f / 6.0f);

        NLMultiWord mw = multiword(null, nlToken(0, 1, null));
        mw.addMeaning(sm1);
        mw.addMeaning(sm2);
        assertEquals(2, mw.getMeanings().size());

    }

    /**
     * Tests multiword with no selected meaning. Result should still have a
     * semtext Meaning of kind {@link MeaningKind#CONCEPT} and no meaning id.
     *
     * <pre>
     * 01234
     * abcd
     * ab       tok1
     *  bc      tok2
     * abc      multiword
     * </pre>
     */
    @Test
    public void testSingleMultiword() {
        String text = "abcd";

        NLSenseMeaning sm1 = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 1.0f / 6.0f);

        NLSenseMeaning sm2 = nlSenseMeaning(TEST_LEMMA_2, TEST_DESCRIPTION_2, TEST_CONCEPT_2_ID, 5.0f / 6.0f);

        NLToken tok1 = nlToken(0, 2, null);
        NLToken tok2 = nlToken(1, 3, null);

        NLText nltext = nlText(text, tok1, tok2);
        NLMultiWord mw = multiword(null, tok1, tok2);
        mw.setMeanings(Sets.newHashSet(sm1, sm2));
        nltext.getSentences().get(0).addMultiWord(mw);

        SemText st = conv.semText(nltext, true);

        assertEquals(text, st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals("abc", st.getText(term));

        assertEquals(2, term.getMeanings().size());

        Meaning meaning1 = term.getMeanings().get(0);

        NLMeaningMetadata metadata1 = (NLMeaningMetadata) meaning1.getMetadata(NLTextConverter.NLTEXT_NAMESPACE);
        assertEquals(TEST_LEMMA_2, metadata1.getLemma());
        assertEquals("", metadata1.getSummary());

        Dict name1 = meaning1.getName();
        assertEquals(sm2.getLemma(), name1.string(Locale.ROOT));

        assertEquals(TEST_DESCRIPTION_2, meaning1.getDescription().string(Locale.ITALIAN));

        Meaning meaning2 = term.getMeanings().get(1);
        Dict name2 = meaning2.getName();

        assertEquals(1, name2.strings(Locale.ROOT).size());
        assertEquals(sm1.getLemma(), name2.string(Locale.ROOT));

        assertEquals(null, term.getSelectedMeaning());

    }

    /**
     * Intersecting entity should win
     *
     * <pre>
     * 0123
     * abc
     * 11   tok1 selected sense
     * mm   multiword
     * nn   named entity
     * </pre>
     */
    @Test
    public void testEntityWins() {

        NLSenseMeaning senseM = nlSenseMeaning(TEST_LEMMA_1,
                TEST_DESCRIPTION_1,
                TEST_CONCEPT_1_ID,
                0.2f);
        NLEntityMeaning entityM = nlEntityMeaning(TEST_LEMMA_1,
                TEST_DESCRIPTION_1,
                TEST_ENTITY_1_ID,
                0.2f);

        NLToken token = nlToken(0, 2, senseM);

        NLText nltext = nlText("abc",
                token);
        nltext.getSentences().get(0).addMultiWord(multiword(null, token));
        nltext.getSentences().get(0).addNamedEntity(namedEntity(entityM, token));

        SemText semText = conv.semText(nltext, true);

        assertEquals(1, semText.terms().size());
        Term t = semText.terms().get(0);
        assertEquals(0, t.getMeanings().size());
        Meaning m = t.getSelectedMeaning();
        assertEquals(MeaningKind.ENTITY, m.getKind());
        assertEquals(conv.getUrlMapper().entityIdToUrl(TEST_ENTITY_1_ID),
                m.getId());
    }

    /**
     * Returns a new multiword. Provided tokens are modified with pointers to
     * the returned multiword.
     */
    private static NLMultiWord multiword(NLSenseMeaning selMeaning, NLToken... tokens) {
        NLMultiWord ret = new NLMultiWord();
        ret.setSelectedMeaning(selMeaning);

        if (tokens.length == 0) {
            throw new RuntimeException("Tried to create multi word with no tokens!");
        }
        ret.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET,
                tokens[0].getProp(SENTENCE_START_OFFSET));

        ret.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET,
                tokens[tokens.length - 1].getProp(SENTENCE_END_OFFSET));

        for (NLToken tok : tokens) {
            tok.addMultiWord(ret);
            tok.setUsedInMultiWord(true);
        }

        ret.setTokens(Arrays.asList(tokens));
        return ret;
    }

    /**
     * Returns a new named entity. Provided tokens are modified with pointers to
     * the returned named entity.
     */
    private static NLNamedEntity namedEntity(NLEntityMeaning selMeaning, NLToken... tokens) {
        NLNamedEntity ret = new NLNamedEntity();
        ret.setSelectedMeaning(selMeaning);

        if (tokens.length == 0) {
            throw new RuntimeException("Tried to create named entity with no tokens!");
        }
        ret.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET,
                tokens[0].getProp(SENTENCE_START_OFFSET));

        ret.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET,
                tokens[tokens.length - 1].getProp(SENTENCE_END_OFFSET));

        for (NLToken tok : tokens) {
            tok.addNamedEntity(ret);
            tok.setUsedInNamedEntity(true);
        }

        ret.setTokens(Arrays.asList(tokens));
        return ret;
    }

    /**
     * Multiword should win, because it has more tokens than named entity
     *
     * <pre>
     * 0123
     * abc
     * m m    multiword with selected sense
     * n      named entity with selected entity
     * 1      tok1 no selected meaning
     *   2    tok2 no selected meaning
     * </pre>
     */
    @Test
    public void multiwordWins() {

        NLSenseMeaning senseM = nlSenseMeaning(TEST_LEMMA_1,
                TEST_DESCRIPTION_1,
                TEST_CONCEPT_1_ID,
                0.2f);
        NLEntityMeaning entityM = nlEntityMeaning(TEST_LEMMA_1,
                TEST_DESCRIPTION_1,
                TEST_ENTITY_1_ID,
                0.2f);

        NLToken tok1 = nlToken(0, 1, null);
        NLToken tok2 = nlToken(2, 3, null);

        NLText nltext = nlText("abc",
                tok1,
                tok2);

        nltext.getSentences().get(0).addMultiWord(multiword(senseM, tok1, tok2));
        nltext.getSentences().get(0).addNamedEntity(namedEntity(entityM, tok1));

        SemText semText = conv.semText(nltext, true);

        assertEquals(1, semText.terms().size());
        Term t = semText.terms().get(0);
        assertEquals(0, t.getMeanings().size());
    }

    /**
     * Tests metadata from meaning is correctly generated
     * <pre>
     * 0123
     * abc
     * ab       tok1
     * </pre>
     */
    @Test
    public void testTokenMetadata() {
        String text = "abc";

        NLSenseMeaning sm = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 1.0f / 6.0f);

        NLToken tok1 = nlToken(0, 2, null, sm);

        NLText nltext = nlText(text, tok1);

        SemText st = conv.semText(nltext, true);

        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals("ab", st.getText(term));

        assertEquals(1, term.getMeanings().size());

        Meaning meaning1 = term.getMeanings().get(0);

        NLMeaningMetadata metadata = (NLMeaningMetadata) meaning1.getMetadata(NLTextConverter.NLTEXT_NAMESPACE);
        assertEquals(Strings.nullToEmpty(sm.getLemma()), metadata.getLemma());
        assertEquals(Strings.nullToEmpty(sm.getSummary()), metadata.getSummary());
    }

    /**
     * Tests two multiwords, no one has selected meaning. Second one has more
     * tokens, so should win.
     * <pre>
     *      01234
     *      abcd
     *      ab       tok1
     *       bc      tok2
     *      ab       multiword 1
     *      abc      multiword 2
     * </pre>
     */
    @Test
    public void testLongestMultiwordWins() {

        String text = "abcd";

        NLSenseMeaning sm1 = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 1.0f / 6.0f);

        NLSenseMeaning sm2 = nlSenseMeaning(TEST_LEMMA_2, TEST_DESCRIPTION_2, TEST_CONCEPT_2_ID, 5.0f / 6.0f);

        NLToken tok1 = nlToken(0, 2, null, sm1, sm2);
        NLToken tok2 = nlToken(1, 3, null, sm1, sm2);

        NLText nltext = nlText(text, tok1, tok2);
        nltext.getSentences().get(0).addMultiWord(multiword(null, tok1));
        nltext.getSentences().get(0).addMultiWord(multiword(null, tok1, tok2));

        SemText st = conv.semText(nltext, true);

        assertEquals(text, st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals("abc", st.getText(term));

    }

    /**
     * Tests two overlapping multiwords, no one has selected meaning so firs
     * should win (both have same number of tokens)
     * <pre>
     * 01234
     * abcd
     * ab       tok1
     *  bc      tok2
     *   cd     tok3
     * abc      multiword 1
     *  bcd     multiword 2
     * </pre>
     */
    @Test
    public void testOverlappingMultiWords() {

        String text = "abcd";

        NLSenseMeaning sm1 = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 1.0f / 6.0f);

        NLSenseMeaning sm2 = nlSenseMeaning(TEST_LEMMA_2, TEST_DESCRIPTION_2, TEST_CONCEPT_2_ID, 5.0f / 6.0f);

        NLToken tok1 = nlToken(0, 2, null, sm1, sm2);
        NLToken tok2 = nlToken(1, 3, null, sm1, sm2);
        NLToken tok3 = nlToken(2, 4, null, sm1, sm2);

        NLText nltext = nlText(text, tok1, tok2, tok3);
        nltext.getSentences().get(0).addMultiWord(multiword(null, tok1, tok2));
        nltext.getSentences().get(0).addMultiWord(multiword(null, tok2, tok3));

        SemText st = conv.semText(nltext, true);

        assertEquals(text, st.getText());
        assertEquals(1, st.getSentences().size());
        assertEquals(1, st.getSentences().get(0).getTerms().size());
        Term term = st.getSentences().get(0).getTerms().get(0);

        assertEquals("abc", st.getText(term));

    }

    /**
     * Example that constructs an nltext of one sentence with one nltoken inside
     * having one nlmeaning and then converts it to a SemText
     */
    @Test
    public void example() {

        NLTextConverter converter = NLTextConverter.of(
                UrlMapper.of("http://mysite.org/entities/",
                        "http://mysite.org/concepts/"));

        String text = "hello world";

        NLText nltext = new NLText(text);

        // NLText too has sentences:
        NLSentence sentence = new NLSentence(text);
        sentence.setProp(NLTextUnit.PFX, START_OFFSET, 0);
        sentence.setProp(NLTextUnit.PFX, END_OFFSET, text.length());

        // Let's create an NLMeaning:
        Set<NLMeaning> meanings = new HashSet<NLMeaning>();
        NLSenseMeaning nlSenseMeaning = new NLSenseMeaning("hello lemma", 1L, "NOUN", 2L, 3, 4, "hello description");
        // NLMeanings can have glosses:
        HashMap<String, String> glosses = new HashMap();
        glosses.put("en", "hello gloss"); 
        nlSenseMeaning.setProp(NLTextUnit.PFX, NLTextConverter.GLOSS_MAP, glosses);
        nlSenseMeaning.setSummary("hello summary");
        meanings.add(nlSenseMeaning);

        // An NLToken to be converted to SemText Term needs offsets:
        NLToken token = new NLToken("hello", meanings);
        token.setProp(NLTextUnit.PFX, SENTENCE_START_OFFSET, 0);
        token.setProp(NLTextUnit.PFX, SENTENCE_END_OFFSET, 5);

        sentence.addToken(token);
        nltext.addSentence(sentence);

        // in the converter with the second parameter we specify NLTest is supposed 
        // to have not been reviewed yet by a human:
        SemText semText = converter.semText(nltext, false);

        // Locale.ROOT is the default locale in SemText:
        assert semText.getLocale().equals(Locale.ROOT);
        // Locale.ROOT is represented by the empty string.
        assert semText.getLocale().toString().equals("");

        // SemText has sentences that contain terms:
        Term term = semText.getSentences().get(0).getTerms().get(0);

        // The meaning status of the term will be either TO_DISAMBIGUATE 
        // or SELECTED as we told the converter NLText was automatically tagged 
        // and has not been reviewed yet by a human
        assert term.getMeaningStatus().equals(MeaningStatus.TO_DISAMBIGUATE);

        // A semtext Term contains meanings:
        Meaning meaning = term.getMeanings().get(0);

        assert meaning.getName().string(Locale.ROOT).equals("hello lemma");
        assert meaning.getDescription().string(Locale.ENGLISH).equals("hello gloss");

        // NLTextConverter will include additional metadata in meanings under namespace "nltext":
        NLMeaningMetadata metadata = (NLMeaningMetadata) meaning.getMetadata("nltext");
        assert metadata.getLemma().equals("hello lemma");
        assert metadata.getSummary().equals("hello summary");

    }

    @Test
    public void testMeaning() {
        NLSenseMeaning meaning = nlSenseMeaning(TEST_LEMMA_1, TEST_DESCRIPTION_1, TEST_CONCEPT_1_ID, 0.3f);

    }

}
