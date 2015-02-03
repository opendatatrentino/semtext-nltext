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
import eu.trentorise.opendata.commons.OdtUtils;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;

/**
 * Simple prefix-based converter to/from numerical IDs and urls. 
 * @author David Leoni
 */
@ParametersAreNonnullByDefault
@Immutable
public final class UrlMapper {

    private static final UrlMapper INSTANCE = new UrlMapper("","");
    
    private String entityPrefix;
    private String conceptPrefix;

    private UrlMapper(){}
    
    private UrlMapper(String entityPrefix, String conceptPrefix) {
        checkNotNull(entityPrefix);
        checkNotNull(conceptPrefix);
        this.entityPrefix = entityPrefix;
        this.conceptPrefix = conceptPrefix;
    }

    /**
     * @throws IllegalArgumentException on unparseable URL
     */
    public long urlToConceptId(String URL) {
        return OdtUtils.parseNumericalId(conceptPrefix, URL);
    }

    /**
     * @throws IllegalArgumentException on unparseable URL
     */
    @Nullable
    public Long urlToEntityId(String URL) {
        return OdtUtils.parseNumericalId(entityPrefix, URL);
    }

    public String entityIdToUrl(Long ID) {
        checkNotNull(ID);
        return entityPrefix + ID;
    }

    public String conceptIdToUrl(Long ID) {
        checkNotNull(ID);
        return conceptPrefix + ID;
    }


    /**
     * Returns default mapper with empty entity prefix and concept prefix
     */
    public static UrlMapper of() {
        return INSTANCE;
    }

    public static UrlMapper of(String entityPrefix, String conceptPrefix) {
        return new UrlMapper(entityPrefix, conceptPrefix);
    }
}
