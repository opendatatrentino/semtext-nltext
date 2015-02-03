package eu.trentorise.opendata.semantics.nlp.converters;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import eu.trentorise.opendata.commons.Dict;
import it.unitn.disi.sweb.core.nlp.model.NLComplexToken;
import it.unitn.disi.sweb.core.nlp.model.NLEntityMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLMultiWord;
import it.unitn.disi.sweb.core.nlp.model.NLNamedEntity;
import it.unitn.disi.sweb.core.nlp.model.NLSenseMeaning;
import it.unitn.disi.sweb.core.nlp.model.NLSentence;
import it.unitn.disi.sweb.core.nlp.model.NLText;
import it.unitn.disi.sweb.core.nlp.model.NLTextUnit;
import it.unitn.disi.sweb.core.nlp.model.NLToken;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.trentorise.opendata.semantics.nlp.model.MeaningKind;
import eu.trentorise.opendata.semantics.nlp.model.MeaningStatus;
import eu.trentorise.opendata.semantics.nlp.model.Meaning;
import eu.trentorise.opendata.semantics.nlp.model.SemText;
import eu.trentorise.opendata.semantics.nlp.model.SemTexts;
import eu.trentorise.opendata.semantics.nlp.model.Sentence;
import eu.trentorise.opendata.semantics.nlp.model.Term;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Class to convert SemText to/from NLText and SemanticString.
 *
 * @author David Leoni
 *
 */
@ParametersAreNonnullByDefault
@Immutable
public final class NLTextConverter {

    private static final Logger logger = LoggerFactory.getLogger(NLTextConverter.class);

    private static final NLTextConverter INSTANCE = new NLTextConverter(UrlMapper.of("", ""));

    private UrlMapper urlMapper;

    private NLTextConverter() {
    }

    private NLTextConverter(UrlMapper urlMapper) {
        checkNotNull(urlMapper);
        this.urlMapper = urlMapper;
    }

    public static NLTextConverter of() {
        return INSTANCE;
    }

    public static NLTextConverter of(UrlMapper urlMapper) {
        return new NLTextConverter(urlMapper);
    }

    /**
     * TODO - it always return the lemma in English!!!
     */
    public static Dict dict(NLMeaning meaning) {
        logger.warn("TODO - RETURNING MEANING LEMMA WITH UNKNOWN LOCALE!");
        return Dict.of(meaning.getLemma());
    }

    /**
     * We support NLMultiTerm and NLNamedEntity
     */
    private static boolean isUsedInComplexToken(NLToken token) {
        return token.isUsedInMultiWord() || token.isUsedInNamedEntity();
    }

    /**
     * We support NLMultiTerm and NLNamedEntity
     */
    private static List<NLComplexToken> getMultiTokens(NLToken token) {
        List<NLComplexToken> ret = new ArrayList();
        if (token.isUsedInMultiWord()) {
            ret.addAll(token.getMultiWords());
        }
        if (token.isUsedInNamedEntity()) {
            ret.addAll(token.getNamedEntities());
        }
        return ret;
    }

    private static MeaningKind getKind(NLComplexToken tok) {
        if (tok instanceof NLNamedEntity) {
            return MeaningKind.ENTITY;
        } else if (tok instanceof NLMultiWord) {
            return MeaningKind.CONCEPT;
        } else {
            throw new UnsupportedOperationException("NLComplexToken of class " + tok.getClass().getSimpleName() + " is not supported!");
        }
    }

    /**
     * Transforms multiwords and named entities into tokens and only includes
     * tokens for which startOffset and endOffset are defined.
     *
     * @param sentence
     */
    private Sentence semTextSentence(NLSentence sentence) {

        if (sentence == null) {
            throw new IllegalArgumentException("Cannot convert a null sentence!");
        }

        int startOffset = -1;
        int endOffset = -1;
        List<Term> words = new ArrayList<Term>();

        Integer so = (Integer) sentence.getProp(NLTextUnit.PFX, "startOffset");
        Integer eo = (Integer) sentence.getProp(NLTextUnit.PFX, "endOffset");

        if (so == null || eo == null) {
            throw new IllegalArgumentException("Offsets are null! startOffset = " + so + " endOffset = " + eo);
        }
        startOffset = so;
        endOffset = eo;

        int i = 0;

        List<NLToken> tokens = sentence.getTokens();
        if (tokens == null) {
            logger.warn("Found NLSentence with null tokens, returning Sentence with no tokens");
            return Sentence.of(startOffset, endOffset);
        }

        while (i < sentence.getTokens().size()) {
            NLToken t = null;
            try {
                t = tokens.get(i);
            } catch (Exception ex) {
            }

            if (t == null) {
                logger.warn("Couldn't find token at position " + i + ", skipping it.");
                i += 1;
                continue;
            }

            try {

                if (t == null) {
                    throw new IllegalArgumentException("Found null NLToken, skipping it");
                }

                if (isUsedInComplexToken(t)) {
                    if (getMultiTokens(t).size() > 1) {
                        logger.warn("Found a token belonging to multiple words and/or named entities. Taking only the first one.");
                    }
                    if (getMultiTokens(t).isEmpty()) {
                        throw new IllegalArgumentException("Token should be used in multitokens, but none found. Skipping token.");
                    }

                    NLComplexToken multiThing = getMultiTokens(t).get(0); // t.getMultiTerms().get(0);

                    int tokensSize = 1;
                    for (int j = i + 1; j < sentence.getTokens().size(); j++) {
                        NLToken q = sentence.getTokens().get(j);
                        if (!(isUsedInComplexToken(q) && getMultiTokens(q).get(0).getId() == multiThing.getId())) {
                            break;
                        } else {
                            tokensSize += 1;
                        }
                    }

                    Integer mtso = (Integer) t.getProp(NLTextUnit.PFX, "sentenceStartOffset");
                    Integer mteo = (Integer) sentence.getTokens().get(i + tokensSize - 1).getProp(NLTextUnit.PFX, "sentenceEndOffset");

                    if (mtso == null || mteo == null) {
                        i += tokensSize;
                        continue;
                    } else {
                        Set<NLMeaning> ms = new HashSet(multiThing.getMeanings());

                        if (multiThing.getMeanings().isEmpty() && multiThing.getSelectedMeaning() != null) {
                            ms.add(multiThing.getSelectedMeaning());
                        }

                        TreeSet<Meaning> sortedMeanings;
                        MeaningStatus meaningStatus;
                        Meaning selectedMeaning;

                        if (ms.size() > 0) {
                            sortedMeanings = makeSortedMeanings(ms);

                            if (sortedMeanings.first().getId().isEmpty()) {
                                meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
                                selectedMeaning = null;
                            } else {
                                meaningStatus = MeaningStatus.SELECTED;
                                selectedMeaning = sortedMeanings.first();
                            }

                        } else { // no meanings, but we know the kind                        
                            sortedMeanings = new TreeSet<Meaning>();
                            sortedMeanings.add(Meaning.of("", getKind(multiThing), 1.0));
                            meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
                            selectedMeaning = null;
                        }
                        words.add(Term.of(startOffset + mtso,
                                startOffset + mteo,
                                meaningStatus, selectedMeaning, ImmutableList.copyOf(sortedMeanings)));

                        i += tokensSize;
                    }

                } else {
                    if (t.getProp(NLTextUnit.PFX, "sentenceStartOffset") != null
                            && t.getProp(NLTextUnit.PFX, "sentenceEndOffset") != null
                            && t.getMeanings().size() > 0) {
                        words.add(semTextTerm(t, startOffset));
                    }
                    i += 1;
                }
            } catch (Exception ex) {
                logger.warn("Error while processing token at position " + i + " with text " + t.getText() + ", skipping it.", ex);
                i += 1;
            }

        }

        return Sentence.of(startOffset, endOffset, words);
    }

    /**
     * Converts provided nlText to a semantic text. Not all NLText features are
     * supported.
     *
     */
    public SemText semText(@Nullable NLText nltext) {

        if (nltext == null) {
            return SemText.of();
        }

        List<Sentence> sentences = new ArrayList<Sentence>();

        List<NLSentence> nlSentences = nltext.getSentences();
        if (nlSentences != null) {
            for (NLSentence nls : nlSentences) {
                Integer so = (Integer) nls.getProp(NLTextUnit.PFX, "startOffset");
                Integer eo = (Integer) nls.getProp(NLTextUnit.PFX, "endOffset");

                if (so != null && eo != null) {
                    try {
                        Sentence s = semTextSentence(nls);
                        sentences.add(s);
                    } catch (Exception ex) {
                        logger.warn("Error while converting NLSentence, skipping it.", ex);
                    }
                }
            }
        }
        Locale locale;
        String lang = nltext.getLanguage();
        if (lang == null) {
            logger.warn("Found null language in nltext " + nltext.getText() + ", setting Locale.ROOT");
            locale = Locale.ROOT;
        } else {
            locale = new Locale(lang);
        }

        return SemText.ofSentences(nltext.getText(), locale, sentences);
    }

    /**
     * Converts provided NLMeaning to a semtext Meaning.
     */
    public Meaning semTextMeaning(NLMeaning nlMeaning) {
        checkNotNull(nlMeaning);
        MeaningKind kind = null;
        String url = "";
        Long id;
        if (nlMeaning instanceof NLSenseMeaning) {
            kind = MeaningKind.CONCEPT;
            id = ((NLSenseMeaning) nlMeaning).getConceptId();
            if (id != null) {
                url = urlMapper.conceptIdToUrl(id);
            }
        } else if (nlMeaning instanceof NLEntityMeaning) {
            kind = MeaningKind.ENTITY;
            id = ((NLEntityMeaning) nlMeaning).getObjectID();
            if (id != null) {
                url = urlMapper.entityIdToUrl(id);
            }
        } else {
            throw new IllegalArgumentException("Found an unsupported meaning type: " + nlMeaning.getClass().getName());
        }
        return Meaning.of(url, kind, nlMeaning.getProbability(), dict(nlMeaning));
    }

    /**
     * Returns a sorted set according to the probabilcopyOfy copyOf pcopyOfvided
     * meanings. First element has the highest probability.
     *
     */
    private TreeSet<Meaning> makeSortedMeanings(Set<? extends NLMeaning> meanings) {
        TreeSet<Meaning> ts = new TreeSet<Meaning>(Collections.reverseOrder());
        for (NLMeaning m : meanings) {
            ts.add(semTextMeaning(m));
        }
        return ts;
    }

    /**
     * @param nlToken must have startOffset and endOffset a throws
     * RuntimeException
     */
    private Term semTextTerm(NLToken nlToken, int sentenceStartOffset) {

        Integer so = (Integer) nlToken.getProp(NLTextUnit.PFX, "sentenceStartOffset");
        Integer eo = (Integer) nlToken.getProp(NLTextUnit.PFX, "sentenceEndOffset");

        if (so == null || eo == null) {
            throw new IllegalArgumentException("Offsets within the sentence are null! sentenceStartOffset = " + so + " sentenceEndOffset = " + eo);
        }

        int startOffset = sentenceStartOffset + so;
        int endOffset = sentenceStartOffset + eo;
        TreeSet<Meaning> meanings = makeSortedMeanings(nlToken.getMeanings());
        
        Meaning selectedMeaning;
        if (nlToken.getSelectedMeaning() == null) {
            selectedMeaning = null;
        } else {
            selectedMeaning = semTextMeaning(nlToken.getSelectedMeaning());
        }
        
        MeaningStatus meaningStatus;

        if (selectedMeaning == null) {
            meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
        } else {
            meaningStatus = MeaningStatus.SELECTED;
        }

        return Term.of(startOffset, endOffset, meaningStatus, selectedMeaning, ImmutableList.copyOf(meanings));
    }

    public UrlMapper getUrlMapper() {
        return urlMapper;
    }

}
