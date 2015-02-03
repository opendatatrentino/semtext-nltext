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
package eu.trentorise.opendata.semantics.nlp.converters;

import static com.google.common.base.Preconditions.checkNotNull;
import eu.trentorise.opendata.semantics.nlp.model.Meaning;
import eu.trentorise.opendata.semantics.nlp.model.MeaningKind;
import eu.trentorise.opendata.semantics.nlp.model.MeaningStatus;
import eu.trentorise.opendata.semantics.nlp.model.SemText;
import eu.trentorise.opendata.semantics.nlp.model.SemTexts;
import eu.trentorise.opendata.semantics.nlp.model.Sentence;
import eu.trentorise.opendata.semantics.nlp.model.Term;
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

    public static SemanticStringConverter of() {
        return INSTANCE;
    }

    public static SemanticStringConverter of(UrlMapper urlMapper) {
        return new SemanticStringConverter(urlMapper);
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
            for (Term word : sentence.getTerms()) {
                List<SemanticTerm> semTerms = new ArrayList<SemanticTerm>();
                List<ConceptTerm> concTerms = new ArrayList<ConceptTerm>();
                List<InstanceTerm> entityTerms = new ArrayList<InstanceTerm>();

                SemanticTerm semTerm = new SemanticTerm();

                for (Meaning m : word.getMeanings()) {
                    double probability;
                    if (MeaningStatus.SELECTED.equals(word.getMeaningStatus())) {
                        probability = 5.0; // so we're sure selected meaning gets the highest weight
                    } else {
                        probability = m.getProbability();
                    }
                    if (MeaningKind.CONCEPT.equals(m.getKind())) {
                        ConceptTerm concTerm = new ConceptTerm();
                        concTerm.setValue(urlMapper.urlToConceptId(m.getId()));

                        concTerm.setWeight(probability);
                        concTerms.add(concTerm);
                        continue;
                    }
                    if (MeaningKind.ENTITY.equals(m.getKind())) {
                        InstanceTerm entityTerm = new InstanceTerm();
                        entityTerm.setValue(urlMapper.urlToEntityId(m.getId()));
                        entityTerm.setWeight(probability);
                        entityTerms.add(entityTerm);
                        continue;
                    }
                    throw new RuntimeException("Found not supported MeaningKind: " + m.getKind());
                }

                semTerm.setOffset(word.getStart());
                semTerm.setText(st.getText(word));
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

        return SemText.ofSentences(text, Locale.ROOT, sentences);
    }

    public UrlMapper getUrlMapper() {
        return urlMapper;
    }
}