package eu.trentorise.opendata.semtext.nltext;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import eu.trentorise.opendata.commons.Dict;
import eu.trentorise.opendata.commons.NotFoundException;
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
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Class to convert SemText to/from NLText
 *
 * Conversion will attach {@link NLMeaningMetadata} to meanings and
 * {@link NLTermMetadata} to terms.
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
     * Metadata in semtext objects converted from nltext will have this
     * namespace
     */
    public static final String NLTEXT_NAMESPACE = "nltext";

    /**
     * Field name of synonimous lemmas in {@link NLMeaning}
     */
    public static final String SYNONYMOUS_LEMMAS = "synonymousLemmas";

    /**
     * Field name of sentence start offset in {@link NLSentence}
     */
    public static final String START_OFFSET = "startOffset";

    /**
     * Field name of sentence end offset in {@link NLSentence}
     */
    public static final String END_OFFSET = "endOffset";

    /**
     * Field name of token start offset in {@link NLToken}
     */
    public static final String SENTENCE_START_OFFSET = "sentenceStartOffset";

    /**
     * Field name of token end offset in {@link NLToken}
     */
    public static final String SENTENCE_END_OFFSET = "sentenceEndOffset";

    /**
     * Gloss map field name for {@link NLSenseMeaning}
     */
    public static final String GLOSS_MAP = "glossMap";

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
    private static String stringToString(@Nullable String string, @Nullable String prependedLogMsg) {
        if (string == null) {
            LOG.log(Level.WARNING, "{0} -- Found null string", prependedLogMsg);
            return "";
        } else {
            if (string.isEmpty()) {
                LOG.log(Level.WARNING, "{0} -- Found empty string", prependedLogMsg);
                return "";
            }
        }
        return string;
    }

    /**
     * Returns a gloss hashmap as a sanitized Dict. On error logs a warning and
     * returns the empty dict.
     *
     * @param prependedLogMsg message to prepend to the warn
     */
    private static Dict glossToDict(NLSenseMeaning senseMeaning, @Nullable String prependedLogMsg) {
        try {
            Map<String, String> glosses = (Map<String, String>) senseMeaning.getProp(NLTextUnit.PFX, GLOSS_MAP);
            if (glosses == null) {
                LOG.log(Level.WARNING, "{0}" + " -- Found null " + GLOSS_MAP + ", returning empty dict", prependedLogMsg);
                return Dict.of();
            }
            Dict.Builder dictb = Dict.builder();

            for (String key : glosses.keySet()) {
                Locale loc = OdtUtils.languageTagToLocale(key);
                String string = glosses.get(key);
                if (string != null) {
                    dictb.put(loc, string);
                }
            }
            return dictb.build();

        }
        catch (Exception ex) {
            LOG.log(Level.WARNING, String.valueOf(prependedLogMsg) + " -- Error while converting gloss map, returning empty dict", ex);
            return Dict.of();
        }
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

        String lemma = stringToString(meaning.getLemma(), "Found invalid lemma in NLSenseMeaning");
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
     * Returns a Dict made with the lemma present in the meaning and eventual
     * synonyms appended afterwords.
     *
     * The method never throws, on error it just returns a Dict will less
     * information.
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
     * Returns true if {@code multiTokens} contain {@code token}
     */
    private static boolean contain(List<NLComplexToken> multiTokens, NLComplexToken token) {
        for (NLComplexToken tok : multiTokens) {
            if (tok.getId() == token.getId()) {
                return true;
            }
        }
        return false;
    }

    /**
     *
     * @throws NotFoundException if the sentence start offset is missing
     */
    private static int sentenceStartOffset(NLToken token) {
        Integer so = (Integer) token.getProp(NLTextUnit.PFX, SENTENCE_START_OFFSET);
        if (so == null) {
            throw new NotFoundException(SENTENCE_START_OFFSET + " is null in NLToken " + token);
        }

        return so;
    }

    /**
     *
     * @throws NotFoundException if the sentence end offset is missing
     */
    private static int getSentenceEndOffset(NLToken token) {
        Integer so = (Integer) token.getProp(NLTextUnit.PFX, SENTENCE_END_OFFSET);
        if (so == null) {
            throw new NotFoundException(SENTENCE_END_OFFSET + " is null in NLToken " + token);
        }

        return so;
    }

    /**
     *
     * @throws NotFoundException if the start offset is missing
     */
    private static int getStartOffset(NLSentence sentence) {
        Integer so = (Integer) sentence.getProp(NLTextUnit.PFX, START_OFFSET);
        if (so == null) {
            throw new NotFoundException(START_OFFSET + " is null in NLSentence " + sentence);
        }
        return so;
    }

    /**
     *
     * @throws NotFoundException if the start offset is missing
     */
    private static int getEndOffset(NLSentence sentence) {
        Integer so = (Integer) sentence.getProp(NLTextUnit.PFX, END_OFFSET);
        if (so == null) {
            throw new NotFoundException(END_OFFSET + " is null in NLSentence " + sentence);
        }
        return so;
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

        Integer so = getStartOffset(sentence);
        Integer eo = getEndOffset(sentence);

        startOffset = so;
        endOffset = eo;

        int tokIndex = 0;

        List<NLToken> tokens = sentence.getTokens();
        if (tokens == null) {
            LOG.warning("Found NLSentence with null tokens, returning Sentence with no tokens");
            return Sentence.of(startOffset, endOffset);
        }

        while (tokIndex < tokens.size()) {
            NLToken tok = null;
            try {
                tok = tokens.get(tokIndex);
            }
            catch (Exception ex) {
            }

            if (tok == null) {
                LOG.log(Level.WARNING, "Couldn''t find token at position {0}, skipping it.", tokIndex);
                tokIndex += 1;
                continue;
            }

            try {

                if (terms.size() > 0) {
                    if (Iterables.getLast(terms).getEnd() > sentenceStartOffset(tok) + startOffset) {
                        tokIndex += 1;
                        continue;
                    }
                }

                if (isUsedInComplexToken(tok)) {
                    List<NLComplexToken> multiTokens = getMultiTokens(tok);

                    if (multiTokens.isEmpty()) {
                        throw new IllegalArgumentException("Token should be used in multitokens, but none found. ");
                    }

                    NLComplexToken multiToken = null;

                    for (NLComplexToken candidateMultiToken : multiTokens) {
                        if (multiToken == null) {
                            multiToken = candidateMultiToken;
                        } else {
                            int candidateMultiTokenSize = candidateMultiToken.getTokens().size();
                            int multiTokenSize = multiToken.getTokens().size();

                            if (candidateMultiTokenSize > multiTokenSize) {
                                multiToken = candidateMultiToken;
                            }

                            if (candidateMultiTokenSize == multiTokenSize) {

                                if (multiToken.getSelectedMeaning() == null) {
                                    multiToken = candidateMultiToken;
                                } else {
                                    if (candidateMultiToken instanceof NLNamedEntity
                                            && candidateMultiToken.getSelectedMeaning() != null
                                            && multiToken instanceof NLMultiWord) {
                                        multiToken = candidateMultiToken;
                                    }
                                }
                            }
                        }
                    }

                    int tokensSize = 1;
                    for (int j = tokIndex + 1; j < sentence.getTokens().size(); j++) {
                        NLToken q = sentence.getTokens().get(j);
                        if (!(isUsedInComplexToken(q)
                                && contain(getMultiTokens(q), multiToken))) {
                            break;
                        } else {
                            tokensSize += 1;
                        }
                    }

                    Integer mtso = (Integer) tok.getProp(NLTextUnit.PFX, SENTENCE_START_OFFSET);
                    Integer mteo = (Integer) sentence.getTokens().get(tokIndex + tokensSize - 1).getProp(NLTextUnit.PFX, SENTENCE_END_OFFSET);

                    if (mtso == null || mteo == null) {
                        tokIndex += tokensSize;
                    } else {
                        terms.add(semTextTerm(startOffset + mtso,
                                startOffset + mteo,
                                multiToken,
                                locale,
                                checkedByUser));
                        tokIndex += tokensSize;
                    }

                } else { // not used in complex token
                    if (tok.getProp(NLTextUnit.PFX, SENTENCE_START_OFFSET) != null
                            && tok.getProp(NLTextUnit.PFX, SENTENCE_END_OFFSET) != null
                            && (tok.getSelectedMeaning() != null
                            || tok.getMeanings().size() > 0)) {
                        terms.add(semTextTerm(tok, locale, startOffset, checkedByUser));
                    }
                    tokIndex += 1;
                }
            }
            catch (Exception ex) {
                LOG.log(Level.WARNING, "Error while processing token at position " + tokIndex + " with text " + tok.getText() + ", skipping it.", ex);
                tokIndex += 1;
            }

        }

        return Sentence.of(startOffset, endOffset, terms);
    }

    /**
     * Converts provided {@code nltext} to a semantic text. Conversion will
     * attach {@link NLMeaningMetadata} to meanings and {@link NLTermMetadata}
     * to terms. <br/>
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

    private List<String> stringsToStrings(@Nullable Iterable<String> strings, @Nullable String prependedLogMsg) {
        if (strings == null) {
            LOG.log(Level.WARNING, "{0} -- Found null strings", prependedLogMsg);
            return new ArrayList();
        }

        List<String> ret = new ArrayList();
        for (String string : strings) {
            if (string == null) {
                LOG.log(Level.WARNING,
                        "{0} -- Found null string, skipping it", prependedLogMsg);
            } else {
                ret.add(string);
            }
        }
        return ret;

    }

    /**
     * Converts provided NLMeaning to a semtext Meaning.
     *
     *
     * This function never throws, on error it simply returns a less
     * meaningful... meaning and logs a warning.
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
                description = glossToDict(senseMeaning, "Error while extracting description from NLSenseMeaning");

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
                    stringToString(nlMeaning.getLemma(), "invalid lemma in NLMeaning"),
                    stringToString(nlMeaning.getSummary(), "invalid summary in NLMeaning"));

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
    private TreeSet<Meaning> makeSortedMeanings(
            Set<? extends NLMeaning> meanings,
            Locale locale) {

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
    private Term semTextTerm(NLToken nlToken,
            Locale locale,
            int sentenceStartOffset,
            boolean checkedByUser) {

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
        TreeSet<Meaning> meanings = makeSortedMeanings(
                nlToken.getMeanings(),
                locale
        );

        Meaning selectedMeaning;
        MeaningStatus meaningStatus;

        if (nlToken.getSelectedMeaning() == null
                || semTextMeaning(nlToken.getSelectedMeaning(), locale)
                .getId().isEmpty()) {
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

            selectedMeaning = semTextMeaning(nlToken.getSelectedMeaning(), locale);
        }

        List<String> sanitizedStems = new ArrayList();
        String sanitizedStem = stringToString(nlToken.getDerivedStem(), "Found invalid stem in NLToken!");
        if (!sanitizedStem.isEmpty()) {
            sanitizedStems.add(sanitizedStem);
        }
        String sanitizedText = stringToString(nlToken.getText(), "Found invalid text in NLToken");
        if (!sanitizedText.isEmpty()) {
            sanitizedStems.add(sanitizedText);
        }
        List<String> sanitizedDerivedLemmas = stringsToStrings(nlToken.getDerivedLemmas(), "Found invalid derived lemma in nltoken!");

        return Term.of(
                startOffset,
                endOffset,
                meaningStatus,
                selectedMeaning,
                ImmutableList.copyOf(meanings),
                ImmutableMap.of(NLTEXT_NAMESPACE,
                        NLTermMetadata.of(sanitizedStems, sanitizedDerivedLemmas)
                ));
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
    private Term semTextTerm(int startOffset,
            int endOffset,
            NLComplexToken multiThing,
            Locale locale,
            boolean checkedByUser) {

        Set<NLMeaning> ms = new HashSet(multiThing.getMeanings());

        TreeSet<Meaning> sortedMeanings;
        MeaningStatus meaningStatus;
        @Nullable
        Meaning selectedMeaning;

        if (multiThing.getSelectedMeaning() == null
                || semTextMeaning(multiThing.getSelectedMeaning(), locale)
                .getId().isEmpty()) {
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

            selectedMeaning = semTextMeaning(
                    multiThing.getSelectedMeaning(),
                    locale);
        }

        if (ms.size() > 0) {
            sortedMeanings = makeSortedMeanings(ms, locale);
        } else { // no meanings, but we know the kind                        
            sortedMeanings = new TreeSet<Meaning>();
            MeaningKind kind = getKind(multiThing);
            if (selectedMeaning == null
                    && MeaningKind.UNKNOWN != kind) {
                NLMeaningMetadata metadata = NLMeaningMetadata.of("", "");
                sortedMeanings.add(Meaning.of(
                        "",
                        kind,
                        1.0,
                        Dict.of(),
                        Dict.of(),
                        ImmutableMap.of(NLTEXT_NAMESPACE, metadata)));
            }
        }

        List<String> sanitizedDerivedLemmas = stringsToStrings(multiThing.getDerivedLemmas(), "Found invalid derived lemma in NLComplexToken!");

        return Term.of(startOffset,
                endOffset,
                meaningStatus,
                selectedMeaning,
                sortedMeanings,
                ImmutableMap.of(NLTEXT_NAMESPACE, NLTermMetadata.of(new ArrayList(),
                                sanitizedDerivedLemmas)));
    }

}
