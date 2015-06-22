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
import com.google.common.collect.ImmutableList;
import java.io.Serializable;
import java.util.List;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Metadata associated to a SemText
 * {@link eu.trentorise.opendata.semtext.Term} when converting from
 * {@link it.unitn.disi.sweb.core.nlp.model.NLToken}
 *
 * @author David Leoni
 */
@Immutable
@ParametersAreNonnullByDefault
public class NLTermMetadata implements Serializable {

    private static final NLTermMetadata INSTANCE = new NLTermMetadata();

    private static final long serialVersionUID = 1L;

    private ImmutableList<String> stems;
    private ImmutableList<String> derivedLemmas;

    private NLTermMetadata() {
        this.stems = ImmutableList.of();
        this.derivedLemmas = ImmutableList.of();
    }

    private NLTermMetadata(Iterable<String> stems, Iterable<String> derivedLemmas) {
        this();
        checkNotNull(stems);
        checkNotNull(derivedLemmas);
        this.stems = ImmutableList.copyOf(stems);
        this.derivedLemmas = ImmutableList.copyOf(derivedLemmas);
    }

    /**
     * Returns the stems of the meaning. This field was added for indexing
     * purposes.
     */
    public List<String> getStems() {
        return stems;
    }

    /**
     * Returns the derived lemmas. Returned list may include the lemma returned
     * by {@link #getLemma()}. This field was added for indexing purposes.
     */
    public List<String> getDerivedLemmas() {
        return derivedLemmas;
    }

    // needed by Jackson
    private void setStems(Iterable<String> stems) {
        checkNotNull(stems);
        this.stems = ImmutableList.copyOf(stems);
    }

    // needed by Jackson
    private void setDerivedLemmas(Iterable<String> derivedLemmas) {
        checkNotNull(derivedLemmas);
        this.derivedLemmas = ImmutableList.copyOf(derivedLemmas);
    }

    /**
     * Returns an NLMeaningMetadata instance with the given lemma and summary
     *
     * @param stems the stems of the single words composing the term
     * @param derivedLemmas the derived lemmas of the single words composing the
     * term
     */
    public static NLTermMetadata of(Iterable<String> stems, Iterable<String> derivedLemmas) {
        return new NLTermMetadata(stems, derivedLemmas);
    }

    /**
     * Returns an NLTermMetadata with empty fields.
     */
    public static NLTermMetadata of(){
        return INSTANCE;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 19 * hash + (this.stems != null ? this.stems.hashCode() : 0);
        hash = 19 * hash + (this.derivedLemmas != null ? this.derivedLemmas.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NLTermMetadata other = (NLTermMetadata) obj;
        if (this.stems != other.stems && (this.stems == null || !this.stems.equals(other.stems))) {
            return false;
        }
        if (this.derivedLemmas != other.derivedLemmas && (this.derivedLemmas == null || !this.derivedLemmas.equals(other.derivedLemmas))) {
            return false;
        }
        return true;
    }
    
    
}
