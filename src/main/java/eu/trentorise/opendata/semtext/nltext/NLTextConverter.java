package eu.trentorise.opendata.semtext.nltext;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import eu.trentorise.opendata.commons.Dict;
import eu.trentorise.opendata.commons.LocalizedString;
import eu.trentorise.opendata.commons.OdtUtils;
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
    
    /**
     * Metadata in semtext objects converted from nltext will have this namespace 
     */
    public static final String NLTEXT_NAMESPACE = "nltext";
    
    /**
     * Field name of synonimous lemmas in {@link NLMeaning}
     */
    public static String SYNONYMOUS_LEMMAS = "synonymousLemmas";
    
    /**
     * Field name of token start offset in {@link NLTerm}
     */
    public static String SENTENCE_START_OFFSET = "sentenceStartOffset";

    /**
     * Field name of token end offset in {@link NLTerm}
     */
    public static String SENTENCE_END_OFFSET = "sentenceEndOffset";
    
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
     * Returns a converter that will use the provided url mapper for converting
     * entity/concept ids to urls.
     */
    public static NLTextConverter of(UrlMapper urlMapper) {
        return new NLTextConverter(urlMapper);
    }

    /**
     * Returns a string as a sanitized String. On error logs a warning and
     * returns the empty string.
     *
     * @param prependedLogMsg message to prepend to the warn
     */
    private static String stringToString(@Nullable String lemma, @Nullable String prependedLogMsg) {
        if (lemma == null) {
            LOG.log(Level.WARNING, "{0} -- Found null string, returning empty string", prependedLogMsg);
            return "";
        } else {
            if (lemma.isEmpty()) {
                LOG.log(Level.WARNING, "{0} -- Found empty lemma, returning empty string", prependedLogMsg);
                return "";
            }
        }
        return lemma;
    }

    /**
     * Returns a string as a sanitized Dict. On error logs a warning and returns
     * the empty dict.
     *
     * @param locale if unknown use {@link Locale#ROOT}
     * @param prependedLogMsg message to prepend to the warn
     */
    private static Dict stringToDict(@Nullable String lemma, Locale locale, @Nullable String prependedLogMsg) {
        checkNotNull(locale);
        String sanitizedLemma = stringToString(lemma, prependedLogMsg);
        if (sanitizedLemma.isEmpty()) {
            return Dict.of();
        } else {
            return Dict.of(locale, sanitizedLemma);
        }
    }

    /**
     * Extracts a list of sanitized lemmas from the given nl sense meaning
     *
     * @param locale the locale of the lemmas. If unknown, use
     * {@link Locale#ROOT}.
     */
    public static List<String> lemmas(NLSenseMeaning meaning, Locale locale) {
        checkNotNull(meaning);

        List<String> ret = new ArrayList();

        String lemma = stringToString(meaning.getLemma(), "Error while extracting lemma from NLSenseMeaning");
        if (!lemma.isEmpty()) {
            ret.add(lemma);
        }
        Object lemmasProp = meaning.getProp(NLTextUnit.PFX, SYNONYMOUS_LEMMAS);
        if (lemmasProp != null) {
            List<String> synLemmas = (List<String>) meaning.getProp(NLTextUnit.PFX, SYNONYMOUS_LEMMAS);

            if (synLemmas != null) {
                for (String synLemma : synLemmas) {
                    if (synLemma == null) {
                        LOG.warning("Found null synonym in NLMeaning!");
                    } else {
                        if (!synLemma.equals(lemma)) {
                            ret.add(synLemma);
                        }
                    }
                }
            }

        }
        return ret;
    }

    /**
     * Returns a Dict made with the lemmaToString present in the meaning and
     * eventual synonyms appended afterwords. The method never throws, on error
     * it just returns a Dict will less information.
     *
     * @param locale the locale of the lemmas. If unknown, use
     * {@link Locale#ROOT}.
     */
    public static Dict dictName(@Nullable NLSenseMeaning meaning, Locale locale) {
        try {
            if (meaning == null) {
                LOG.warning("found null NLMeaning while extracting dict, returning empty Dict");
                return Dict.of();
            }

            List<String> sanitizedLemmas = new ArrayList();

            String lemma = meaning.getLemma();
            if (lemma == null) {
                LOG.warning("found null lemma in NLMeaning while extracting dict");
            } else {
                if (lemma.isEmpty()) {
                    LOG.warning("found empty lemma in NLMeaning while extracting dict");
                } else {
                    sanitizedLemmas.add(lemma);
                }
            }

            Object lemmasProp = meaning.getProp(NLTextUnit.PFX, "synonymousLemmas");
            if (lemmasProp != null) {
                List<String> synLemmas = (List<String>) meaning.getProp(NLTextUnit.PFX, "synonymousLemmas");

                if (synLemmas != null) {
                    for (String synLemma : synLemmas) {
                        if (synLemma == null) {
                            LOG.warning("Found null synonym in NLMeaing!");
                        } else {
                            if (!synLemma.equals(lemma)) {
                                sanitizedLemmas.add(synLemma);
                            }
                        }
                    }
                }

            }

            if (sanitizedLemmas.isEmpty()) {
                LOG.warning("Found no valid lemmas to use, returning empty dict!");
                return Dict.of();
            } else {
                return Dict.of(locale, sanitizedLemmas);
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
     * Warning: conversion may be lossy.
     *
     * @param checkedByUser see {@link #semText(it.unitn.disi.sweb.core.nlp.model.NLText, boolean)
     * @param locale If unknown, use {@link Locale#ROOT}
     *
     */
    private Sentence semTextSentence(NLSentence sentence, Locale locale, boolean checkedByUser) {

        if (sentence == null) {
            throw new IllegalArgumentException("Cannot convert a null sentence!");
        }

        int startOffset;
        int endOffset;
        List<Term> terms = new ArrayList<Term>();

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

                    Integer mtso = (Integer) t.getProp(NLTextUnit.PFX, SENTENCE_START_OFFSET);
                    Integer mteo = (Integer) sentence.getTokens().get(i + tokensSize - 1).getProp(NLTextUnit.PFX, SENTENCE_END_OFFSET);

                    if (mtso == null || mteo == null) {
                        i += tokensSize;
                    } else {
                        terms.add(semTextTerm(startOffset + mtso,
                                startOffset + mteo,
                                multiThing,
                                locale,
                                checkedByUser));
                        i += tokensSize;
                    }

                } else { // not used in complex token
                    if (t.getProp(NLTextUnit.PFX, "sentenceStartOffset") != null
                            && t.getProp(NLTextUnit.PFX, "sentenceEndOffset") != null
                            && t.getMeanings().size() > 0) {
                        terms.add(semTextTerm(t, locale, startOffset, checkedByUser));
                    }
                    i += 1;
                }
            }
            catch (Exception ex) {
                LOG.log(Level.WARNING, "Error while processing token at position " + i + " with text " + t.getText() + ", skipping it.", ex);
                i += 1;
            }

        }

        return Sentence.of(startOffset, endOffset, terms);
    }

    /**
     * Converts provided {@code nltext} to a semantic text. <br/>
     * <br/>
     * Warning: conversion may be lossy.
     *
     * @param checkedByUser if true, the {@code nltext} is supposed to have been
     * reviewed entirely by a human and meaning statuses in returned
     * {@code SemText} will be either {@link MeaningStatus#REVIEWED REVIEWED} or
     * {@link MeaningStatus#NOT_SURE NOT_SURE}, otherwise the semantic string is
     * supposed to have been enriched automatically by some nlp service and
     * meaning statuses will be either {@link MeaningStatus#SELECTED SELECTED}
     * or {@link MeaningStatus#TO_DISAMBIGUATE TO_DISAMBIGUATE}.
     *
     */
    public SemText semText(@Nullable NLText nltext, boolean checkedByUser) {

        if (nltext == null) {
            LOG.warning("Found null NLText while converting to SemText, returning empty semtext");
            return SemText.of();
        }
        
        Locale locale;
        String lang = nltext.getLanguage();
        if (lang == null) {
            LOG.log(Level.WARNING, "Found null language in nltext {0}, setting Locale.ROOT", nltext.getText());
            locale = Locale.ROOT;
        } else {
            locale = OdtUtils.languageTagToLocale(lang);
        }

        List<Sentence> sentences = new ArrayList<Sentence>();

        List<NLSentence> nlSentences = nltext.getSentences();
        if (nlSentences != null) {
            for (NLSentence nls : nlSentences) {
                Integer so = (Integer) nls.getProp(NLTextUnit.PFX, "startOffset");
                Integer eo = (Integer) nls.getProp(NLTextUnit.PFX, "endOffset");

                if (so != null && eo != null) {
                    try {
                        Sentence s = semTextSentence(nls, locale, checkedByUser);
                        sentences.add(s);
                    }
                    catch (Exception ex) {
                        LOG.log(Level.WARNING, "Error while converting NLSentence, skipping it.", ex);
                    }
                }
            }
        }                

        return SemText.ofSentences(locale, nltext.getText(), sentences);
    }

    /**
     * Converts provided NLMeaning to a semtext Meaning. This function never
     * throws, on error it simply returns a less meaningful... meaning and logs
     * a warning.
     *
     * @param locale If unknown, use {@link Locale#ROOT}
     *
     * Warning: conversion may be lossy.
     */
    public Meaning semTextMeaning(@Nullable NLMeaning nlMeaning, Locale locale) {
        try {
            if (nlMeaning == null) {
                LOG.warning("Found null nlMeaning during conversion to SemText meaning, returning empty Meaning.of()");
                return Meaning.of();
            }

            MeaningKind kind = null;
            String url = "";
            Long id;
            Dict name;
            Dict description;
            

            if (nlMeaning instanceof NLSenseMeaning) {
                NLSenseMeaning senseMeaning = ((NLSenseMeaning) nlMeaning);
                kind = MeaningKind.CONCEPT;
                id = senseMeaning.getConceptId();
                if (id != null) {
                    url = urlMapper.conceptIdToUrl(id);
                }
                name = dictName(senseMeaning, locale);
                description = stringToDict(senseMeaning.getDescription(), locale, "Error while extracting description from NLSenseMeaning");
                
            } else if (nlMeaning instanceof NLEntityMeaning) {
                NLEntityMeaning entityMeaning = ((NLEntityMeaning) nlMeaning);
                kind = MeaningKind.ENTITY;
                id = entityMeaning.getObjectID();
                if (id != null) {
                    url = urlMapper.entityIdToUrl(id);
                }
                name = stringToDict(url, locale, "Error while extracting description from NLEntityMeaning");
                description = stringToDict(entityMeaning.getDescription(), locale, "Error while extracting description from NLEntityMeaning");
            } else {
                throw new IllegalArgumentException("Found an unsupported meaning type: " + nlMeaning.getClass().getName());
            }
            
            NLMeaningMetadata metadata = NLMeaningMetadata.of(
                    stringToString(nlMeaning.getLemma(), "Error while extracting lemma from NLMeaning"),
                    stringToString(nlMeaning.getSummary(), "Error while extracting summary from NLMeaning"));
            
            return Meaning.of(url, kind, nlMeaning.getProbability(), name, description, ImmutableMap.of(NLTEXT_NAMESPACE, metadata));
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
    private TreeSet<Meaning> makeSortedMeanings(Set<? extends NLMeaning> meanings, Locale locale) {
        TreeSet<Meaning> ts = new TreeSet<Meaning>(Collections.reverseOrder());
        for (NLMeaning m : meanings) {
            ts.add(semTextMeaning(m, locale));
        }
        return ts;
    }

    /**
     * @param nlToken must have startOffset and endOffset otherwise throws
     * IllegalArgumentException
     * @param locale if unknown use {@link Locale#ROOT}
     * @param checkedByUser see {@link #semText(it.unitn.disi.sweb.core.nlp.model.NLText, boolean)
     * }
     */
    private Term semTextTerm(NLToken nlToken, Locale locale, int sentenceStartOffset, boolean checkedByUser) {

        checkNotNull(nlToken);
        checkNotNull(locale);

        checkArgument(sentenceStartOffset >= 0, "Sentence start offset can't be negative! Offset found: %s", sentenceStartOffset);

        Integer so = (Integer) nlToken.getProp(NLTextUnit.PFX, "sentenceStartOffset");
        Integer eo = (Integer) nlToken.getProp(NLTextUnit.PFX, "sentenceEndOffset");

        if (so == null || eo == null) {
            throw new IllegalArgumentException("Offsets within the sentence are null! sentenceStartOffset = " + so + " sentenceEndOffset = " + eo);
        }

        int startOffset = sentenceStartOffset + so;
        int endOffset = sentenceStartOffset + eo;
        TreeSet<Meaning> meanings = makeSortedMeanings(nlToken.getMeanings(), locale);

        Meaning selectedMeaning;
        if (nlToken.getSelectedMeaning() == null) {
            selectedMeaning = null;
        } else {
            selectedMeaning = semTextMeaning(nlToken.getSelectedMeaning(), locale);
        }

        MeaningStatus meaningStatus;

        if (selectedMeaning == null) {
            if (checkedByUser) {
                meaningStatus = MeaningStatus.NOT_SURE;
            } else {
                meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
            }
        } else {
            if (checkedByUser) {
                meaningStatus = MeaningStatus.REVIEWED;
            } else {
                meaningStatus = MeaningStatus.SELECTED;
            }

        }

        return Term.of(startOffset, endOffset, meaningStatus, selectedMeaning, ImmutableList.copyOf(meanings));
    }

    /**
     * Returns the UrlMapper used by the converter.
     */
    public UrlMapper getUrlMapper() {
        return urlMapper;
    }

    /**
     *
     * @param checkedByUser see {@link #semText(it.unitn.disi.sweb.core.nlp.model.NLText, boolean)
     * @param locale If unknown, use {@link Locale#ROOT}
     *
     */
    private Term semTextTerm(int startOffset, int endOffset, NLComplexToken multiThing, Locale locale, boolean checkedByUser) {

        Set<NLMeaning> ms = new HashSet(multiThing.getMeanings());

        if (multiThing.getMeanings().isEmpty() && multiThing.getSelectedMeaning() != null) {
            ms.add(multiThing.getSelectedMeaning());
        }

        TreeSet<Meaning> sortedMeanings;
        MeaningStatus meaningStatus;
        Meaning selectedMeaning;

        if (ms.size() > 0) {
            sortedMeanings = makeSortedMeanings(ms, locale);

            if (sortedMeanings.first().getId().isEmpty()) {
                if (checkedByUser) {
                    meaningStatus = MeaningStatus.NOT_SURE;
                } else {
                    meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
                }

                selectedMeaning = null;
            } else {
                if (checkedByUser) {
                    meaningStatus = MeaningStatus.REVIEWED;
                } else {
                    meaningStatus = MeaningStatus.SELECTED;
                }
                selectedMeaning = sortedMeanings.first();
            }

        } else { // no meanings, but we know the kind                        
            sortedMeanings = new TreeSet<Meaning>();
            MeaningKind kind = getKind(multiThing);
            if (MeaningKind.UNKNOWN != kind) {
                sortedMeanings.add(Meaning.of("", kind, 1.0));
            }
            if (checkedByUser) {
                meaningStatus = MeaningStatus.NOT_SURE;
            } else {
                meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
            }

            selectedMeaning = null;
        }
        return Term.of(startOffset,
                endOffset,
                meaningStatus, selectedMeaning, sortedMeanings);
    }
    
}
