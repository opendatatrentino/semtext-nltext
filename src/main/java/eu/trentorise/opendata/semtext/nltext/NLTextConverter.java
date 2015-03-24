package eu.trentorise.opendata.semtext.nltext;

import static com.google.common.base.Preconditions.checkArgument;
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

import eu.trentorise.opendata.semtext.MeaningKind;
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.Meaning;
import eu.trentorise.opendata.semtext.SemText;
import eu.trentorise.opendata.semtext.Sentence;
import eu.trentorise.opendata.semtext.Term;
import java.util.logging.Level;
import java.util.logging.Logger;
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

    private static final Logger LOG = Logger.getLogger(NLTextConverter.class.getName());

    private static final NLTextConverter INSTANCE = new NLTextConverter();

    private UrlMapper urlMapper;

    private NLTextConverter() {
        urlMapper = UrlMapper.of();
    }

    private NLTextConverter(UrlMapper urlMapper) {
        this();
        checkNotNull(urlMapper);
        this.urlMapper = urlMapper;
    }

    /**
     * Returns a converter which stores numerical ids as strings with no
     * prefixes like "12345".
     */
    public static NLTextConverter of() {
        return INSTANCE;
    }

    /**
     * Returns a converter that will use the provided url mapper for converting entity/concept ids to urls.
     */
    public static NLTextConverter of(UrlMapper urlMapper) {
        return new NLTextConverter(urlMapper);
    }

    /**
     * Returns a Dict made with the synonims preent inthe meaning. If they are
     * absent, NLMeaning.getLemma is used instead. The method never throws, on
     * error it just returns a Dict will less information.
     */
    public static Dict dict(@Nullable NLMeaning meaning) {
        try {
            if (meaning == null) {
                LOG.warning("found null NLMeaning while extracting dict, returning empty Dict");
                return Dict.of();
            }

            LOG.warning("TODO - RETURNING MEANING LEMMA(S) WITH UNKNOWN LOCALE!");

            Object lemmasProp = meaning.getProp(NLTextUnit.PFX, "synonymousLemmas");
            if (lemmasProp != null) {
                List<String> lemmas = (List<String>) meaning.getProp(NLTextUnit.PFX, "synonymousLemmas");

                if (lemmas != null) {
                    List<String> sanitizedLemmas = new ArrayList();
                    for (String lemma : lemmas) {
                        if (lemma == null) {
                            LOG.warning("Found null synonym in NLMeaing!");
                        } else {
                            sanitizedLemmas.add(lemma);
                        }
                    }
                    return Dict.of(sanitizedLemmas);
                }

            }
            if (meaning.getLemma() == null) {
                LOG.warning("Found NLMeaning.getLemma() = null !");
                return Dict.of();
            } else {
                return Dict.of(meaning.getLemma());
            }
        }
        catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error while creating Dict from NLMeaning, returning empty Dict", ex);
            return Dict.of();
        }

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
            LOG.log(Level.WARNING, "Found token with unhandled class {0},  setting meaning kind to UNKNWON.", tok.getClass());
            return MeaningKind.UNKNOWN;
        }
    }

    /**
     * Converter from NLSentence to SemText Sentence.
     *
     * Transforms multiwords and named entities into non-overlapping terms and
     * only includes terms for which startOffset and endOffset are defined.
     *
     */
    private Sentence semTextSentence(NLSentence sentence) {

        if (sentence == null) {
            throw new IllegalArgumentException("Cannot convert a null sentence!");
        }

        int startOffset;
        int endOffset;
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
            LOG.warning("Found NLSentence with null tokens, returning Sentence with no tokens");
            return Sentence.of(startOffset, endOffset);
        }

        while (i < sentence.getTokens().size()) {
            NLToken t = null;
            try {
                t = tokens.get(i);
            }
            catch (Exception ex) {
            }

            if (t == null) {
                LOG.log(Level.WARNING, "Couldn''t find token at position {0}, skipping it.", i);
                i += 1;
                continue;
            }

            try {

                if (t == null) {
                    throw new IllegalArgumentException("Found null NLToken, skipping it");
                }

                if (isUsedInComplexToken(t)) {
                    if (getMultiTokens(t).size() > 1) {
                        LOG.warning("Found a token belonging to multiple words and/or named entities. Taking only the first one.");
                    }
                    if (getMultiTokens(t).isEmpty()) {
                        throw new IllegalArgumentException("Token should be used in multitokens, but none found. ");
                    }

                    NLComplexToken multiThing = getMultiTokens(t).get(0); 

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
                    } else {                        
                        words.add(semTextTerm(startOffset + mtso,
                                                 startOffset + mteo, 
                                                 multiThing));
                        i += tokensSize;
                    }

                } else { // not used in complex token
                    if (t.getProp(NLTextUnit.PFX, "sentenceStartOffset") != null
                            && t.getProp(NLTextUnit.PFX, "sentenceEndOffset") != null
                            && t.getMeanings().size() > 0) {
                        words.add(semTextTerm(t, startOffset));
                    }
                    i += 1;
                }
            }
            catch (Exception ex) {
                LOG.log(Level.WARNING, "Error while processing token at position " + i + " with text " + t.getText() + ", skipping it.", ex);
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
            LOG.warning("Found null NLText while converting to SemText, returning empty semtext");
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
                    }
                    catch (Exception ex) {
                        LOG.log(Level.WARNING, "Error while converting NLSentence, skipping it.", ex);
                    }
                }
            }
        }
        Locale locale;
        String lang = nltext.getLanguage();
        if (lang == null) {
            LOG.log(Level.WARNING, "Found null language in nltext {0}, setting Locale.ROOT", nltext.getText());
            locale = Locale.ROOT;
        } else {
            locale = new Locale(lang);
        }

        return SemText.ofSentences(locale, nltext.getText(), sentences);
    }

    /**
     * Converts provided NLMeaning to a semtext Meaning. This function never
     * throws, on error it simply returns a less meaningful... meaning and logs
     * a warning.
     */
    public Meaning semTextMeaning(@Nullable NLMeaning nlMeaning) {
        try {
            if (nlMeaning == null) {
                LOG.warning("Found null nlMeaning during conversion to SemText meaning, returning empty Meaning.of()");
                return Meaning.of();
            }

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
        catch (Exception ex) {
            LOG.log(Level.SEVERE, "Error while converting NLMeaning to SemText meaning, returning empty Meaning.of()", ex);
            return Meaning.of();
        }
    }

    /**
     * Returns a sorted set according to the probability of provided meanings.
     * First element has the highest probability.
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
     * @param nlToken must have startOffset and endOffset otherwise throws
     * IllegalArgumentException
     */
    private Term semTextTerm(NLToken nlToken, int sentenceStartOffset) {

        checkArgument(sentenceStartOffset >= 0, "Sentence start offset can't be negative! Offset found: %s", sentenceStartOffset);

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

    /**
     * Returns the UrlMapper used by the converter.
     */
    public UrlMapper getUrlMapper() {
        return urlMapper;
    }
    
    private Term semTextTerm(int startOffset, int endOffset,  NLComplexToken multiThing) {
        
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
            MeaningKind kind = getKind(multiThing);
            if (MeaningKind.UNKNOWN != kind) {
                sortedMeanings.add(Meaning.of("", kind, 1.0));
            }
            meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
            selectedMeaning = null;
        }
        return Term.of(startOffset,
                         endOffset,
                         meaningStatus, selectedMeaning, sortedMeanings);
    }

}
