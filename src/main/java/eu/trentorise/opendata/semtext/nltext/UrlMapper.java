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
import eu.trentorise.opendata.commons.OdtUtils;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Simple prefix-based converter to/from numerical IDs and urls.
 *
 * @author David Leoni
 */
@Immutable
@ParametersAreNonnullByDefault
public final class UrlMapper {

    private static final UrlMapper INSTANCE = new UrlMapper();

    private String entityPrefix;
    private String conceptPrefix;

    private UrlMapper() {
        entityPrefix = "";
        conceptPrefix = "";
    }

    private UrlMapper(String entityPrefix, String conceptPrefix) {
        this();
        checkNotNull(entityPrefix);
        checkNotNull(conceptPrefix);
        this.entityPrefix = entityPrefix;
        this.conceptPrefix = conceptPrefix;
    }

    /**
     * @throws IllegalArgumentException on unparseable URL
     */
    public long urlToConceptId(String url) {
        return OdtUtils.parseNumericalId(conceptPrefix, url);
    }

    /**
     * @throws IllegalArgumentException on unparseable URL
     */
    public long urlToEntityId(String url) {
        return OdtUtils.parseNumericalId(entityPrefix, url);
    }

    /**
     * Returns the entity id as an url
     *
     * @param id must not be null.
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String entityIdToUrl(Long id) {

        checkNotNull(id);
        return entityPrefix + id;
    }

    /**
     * Returns the concept id as an url
     *
     * @param id must not be null.
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String conceptIdToUrl(Long id) {
        checkNotNull(id);
        return conceptPrefix + id;
    }

    /**
     * Returns default mapper with empty entity prefix and concept prefix
     */
    public static UrlMapper of() {
        return INSTANCE;
    }
    
    /**
     * Returns an UrlMapper with provided prefixes for entities and concepts
     */
    public static UrlMapper of(String entityPrefix, String conceptPrefix) {
        return new UrlMapper(entityPrefix, conceptPrefix);
    }
}
