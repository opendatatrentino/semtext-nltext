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
import java.io.Serializable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Metadata associated to a SemText
 * {@link eu.trentorise.opendata.semtext.Meaning} when converting from
 * {@link it.unitn.disi.sweb.core.nlp.model.NLMeaning}
 *
 * @author David Leoni
 */
@Immutable
@ParametersAreNonnullByDefault
public final class NLMeaningMetadata implements Serializable {

    private static final NLMeaningMetadata INSTANCE = new NLMeaningMetadata();
    
    private static final long serialVersionUID = 1L;

    private String lemma;
    private String summary;    

    private NLMeaningMetadata() {
        this.lemma = "";
        this.summary = "";
    }

    private NLMeaningMetadata(String lemma, String summary) {
        this();
        checkNotNull(lemma);
        checkNotNull(summary);
        this.lemma = lemma;
        this.summary = summary;
    }

    // needed by Jackson
    private void setLemma(String lemma) {
        checkNotNull(lemma);
        this.lemma = lemma;
    }

    // needed by Jackson
    private void setSummary(String summary) {
        checkNotNull(summary);
        this.summary = summary;
    }

 
    /**
     * Returns the lemma of the meaning. It will be in the same language of the
     * whole SemText containing it.
     */
    public String getLemma() {
        return lemma;
    }

    /**
     * Returns the summary of the meaning. It will be in the same language of
     * the whole SemText containing it.
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Returns an NLMeaningMetadata instance with the given lemma and summary
     *
     * @param lemma the lemma of the meaning. Must be in the same language of
     * the whole SemText containing it.
     * @param summary the summary of the meaning. Must be in the same language
     * of the whole SemText containing it.
     */
    public static NLMeaningMetadata of(String lemma, String summary) {
        return new NLMeaningMetadata(lemma, summary);
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 47 * hash + (this.lemma != null ? this.lemma.hashCode() : 0);
        hash = 47 * hash + (this.summary != null ? this.summary.hashCode() : 0);
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
        final NLMeaningMetadata other = (NLMeaningMetadata) obj;
        if ((this.lemma == null) ? (other.lemma != null) : !this.lemma.equals(other.lemma)) {
            return false;
        }
        if ((this.summary == null) ? (other.summary != null) : !this.summary.equals(other.summary)) {
            return false;
        }
        return true;
    }


    /**
     * Returns an NLMeaningMetadata with empty fields.
     */
    public static NLMeaningMetadata of(){
        return INSTANCE;
    }    

}
