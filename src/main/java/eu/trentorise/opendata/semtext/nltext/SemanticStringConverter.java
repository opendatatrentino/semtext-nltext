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
package eu.trentorise.opendata.semtext.nltext;

import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.semtext.Meaning;
import eu.trentorise.opendata.semtext.MeaningKind;
import eu.trentorise.opendata.semtext.MeaningStatus;
import eu.trentorise.opendata.semtext.SemText;
import eu.trentorise.opendata.semtext.SemTexts;
import eu.trentorise.opendata.semtext.Sentence;
import eu.trentorise.opendata.semtext.Term;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ComplexConcept;
import it.unitn.disi.sweb.webapi.model.eb.sstring.ConceptTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.InstanceTerm;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticString;
import it.unitn.disi.sweb.webapi.model.eb.sstring.SemanticTerm;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author David Leoni
 */
@ParametersAreNonnullByDefault
@Immutable
public final class SemanticStringConverter {

    private static final Logger logger = LoggerFactory.getLogger(SemanticStringConverter.class);

    private static final SemanticStringConverter INSTANCE = new SemanticStringConverter(UrlMapper.of());

    private UrlMapper urlMapper;

    private SemanticStringConverter() {
    }

    private SemanticStringConverter(UrlMapper urlMapper) {
        checkNotNull(urlMapper);
        this.urlMapper = urlMapper;
    }

    /**
     * Returns a converter which stores numerical ids as strings with no
     * prefixes like "12345".
     */
    public static SemanticStringConverter of() {
        return INSTANCE;
    }

    public static SemanticStringConverter of(UrlMapper urlMapper) {
        return new SemanticStringConverter(urlMapper);
    }

    private void addMeaning(Meaning m, double probability, List<ConceptTerm> concTerms, List<InstanceTerm> entityTerms) {
        checkNotNull(m);
        checkNotNull(concTerms);
        checkNotNull(entityTerms);

        if (MeaningKind.CONCEPT.equals(m.getKind())) {
            ConceptTerm concTerm = new ConceptTerm();
            concTerm.setValue(urlMapper.urlToConceptId(m.getId()));

            concTerm.setWeight(probability);
            concTerms.add(concTerm);
            return;
        }
        if (MeaningKind.ENTITY.equals(m.getKind())) {
            InstanceTerm entityTerm = new InstanceTerm();
            entityTerm.setValue(urlMapper.urlToEntityId(m.getId()));
            entityTerm.setWeight(probability);
            entityTerms.add(entityTerm);
            return;
        }
        if (MeaningKind.UNKNOWN.equals(m.getKind())) {
            if (m.getId().length() > 0) {
                logger.warn("Found meaning of kind UNKNOWN with non-empty id: {0}, skipping it !", m.getId());
                return;
            }
        }
        throw new IllegalArgumentException("Found not supported MeaningKind: " + m.getKind());
    }

    /**
     * Converts input semantic text into a semantic string. For each Term of
     * input semantic text a ComplexConcept holding one semantic term is
     * created.
     *
     * @param st the semantic string to convert
     * @return a semantic string representation of input semantic text
     */
    public SemanticString semanticString(SemText st) {
        List<ComplexConcept> complexConcepts = new ArrayList<ComplexConcept>();

        for (Sentence sentence : st.getSentences()) {
            for (Term stTerm : sentence.getTerms()) {
                List<SemanticTerm> semTerms = new ArrayList<SemanticTerm>();
                List<ConceptTerm> concTerms = new ArrayList<ConceptTerm>();
                List<InstanceTerm> entityTerms = new ArrayList<InstanceTerm>();

                SemanticTerm semTerm = new SemanticTerm();

                if (MeaningStatus.SELECTED.equals(stTerm.getMeaningStatus())
                        || MeaningStatus.REVIEWED.equals(stTerm.getMeaningStatus())) {
                    // super high prob so we're sure selected meaning gets the highest weight
                    addMeaning(stTerm.getSelectedMeaning(), 5.0, concTerms, entityTerms);
                }

                for (Meaning m : stTerm.getMeanings()) {
                    Meaning selMeaning = stTerm.getSelectedMeaning();
                    if (selMeaning != null
                            && !m.getId().equals(selMeaning.getId())) {
                        addMeaning(m, m.getProbability(), concTerms, entityTerms);
                    }
                }

                semTerm.setOffset(stTerm.getStart());
                semTerm.setText(st.getText(stTerm));
                semTerm.setConceptTerms(concTerms);
                semTerm.setInstanceTerms(entityTerms);

                semTerms.add(semTerm);
                ComplexConcept cc = new ComplexConcept(semTerms);
                complexConcepts.add(cc);
            }
        }

        return new SemanticString(st.getText(), complexConcepts);
    }

    /**
     * Converts provided semantic string to a semantic text. Semantic text terms
     * meaning status will be deducted using the {@link SemTexts#disambiguate(java.lang.Iterable)
     * } function.
     *
     * Given that semantic string is underspecified, it is not possible to know
     * if we can faithfully convert all the semantic strings out there.
     *
     */
    public SemText semText(@Nullable SemanticString ss) {
        if (ss == null) {
            return SemText.of();
        }

        String text;

        if (ss.getText() == null) {
            text = "";
        } else {
            text = ss.getText();
        }

        List<Sentence> sentences = new ArrayList<Sentence>();
        List<Term> words = new ArrayList<Term>();

        int pos = 0;
        if (ss.getComplexConcepts() != null) {
            for (ComplexConcept cc : ss.getComplexConcepts()) {
                if (cc.getTerms() != null) {
                    for (SemanticTerm st : cc.getTerms()) {

                        // overlapping terms are ignored
                        if (st.getOffset() != null && st.getOffset() >= pos) {

                            List<Meaning> meanings = new ArrayList();

                            if (st.getConceptTerms() != null) {
                                for (ConceptTerm ct : st.getConceptTerms()) {
                                    if (ct.getValue() != null) {
                                        double weight;
                                        if (ct.getWeight() == null) {
                                            weight = 1.0;
                                        } else {
                                            weight = ct.getWeight();
                                        }
                                        Long id = ct.getValue();
                                        if (id != null) {
                                            meanings.add(Meaning.of(urlMapper.conceptIdToUrl(id), MeaningKind.CONCEPT, weight));
                                        }

                                    }
                                }
                            }
                            if (st.getInstanceTerms() != null) {
                                for (InstanceTerm it : st.getInstanceTerms()) {
                                    if (it.getValue() != null) {
                                        double weight;
                                        if (it.getWeight() == null) {
                                            weight = 1.0;
                                        } else {
                                            weight = it.getWeight();
                                        }
                                        Long id = it.getValue();
                                        if (id != null) {
                                            meanings.add(Meaning.of(urlMapper.entityIdToUrl(it.getValue()), MeaningKind.ENTITY, weight));
                                        }
                                    }

                                }
                            }
                            if (meanings.size() > 0) {

                                Meaning selectedMeaning = SemTexts.disambiguate(meanings);
                                MeaningStatus meaningStatus;
                                if (selectedMeaning == null) {
                                    meaningStatus = MeaningStatus.TO_DISAMBIGUATE;
                                } else {
                                    meaningStatus = MeaningStatus.SELECTED;
                                }
                                words.add(Term.of(st.getOffset(),
                                        st.getOffset() + st.getText().length(),
                                        meaningStatus,
                                        selectedMeaning,
                                        meanings
                                ));
                                pos = st.getOffset() + st.getText().length();
                            }
                        }
                    }
                }
            }
        }

        sentences.add(Sentence.of(0, text.length(), words));

        return SemText.ofSentences(Locale.ROOT, text,  sentences);
    }

    public UrlMapper getUrlMapper() {
        return urlMapper;
    }
}
