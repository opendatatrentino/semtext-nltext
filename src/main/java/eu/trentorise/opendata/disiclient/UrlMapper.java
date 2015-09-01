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
package eu.trentorise.opendata.disiclient;

import static com.google.common.base.Preconditions.checkNotNull;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import eu.trentorise.opendata.commons.OdtUtils;
import static eu.trentorise.opendata.commons.validation.Preconditions.checkNotEmpty;
import java.net.MalformedURLException;
import java.net.URL;
import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;
import javax.annotation.concurrent.Immutable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple prefix-based converter to/from numerical IDs and urls.
 *
 * @author David Leoni
 */
@Immutable
@ParametersAreNonnullByDefault
public final class UrlMapper {

    private static final String CONCEPT_PREFIX = "/concepts";

    private static final String ENTITY_PREFIX = "/instances";

    private static final String ATTR_DEF_PREFIX = "/attributedefinitions";

    private static final String ETYPE_PREFIX = "/types";

    private static Logger LOG = LoggerFactory.getLogger(UrlMapper.class);

    private static final UrlMapper INSTANCE = new UrlMapper();

    private static final String DEBUG_GLOBAL_CONCEPT_ID = "debugGlobalConceptId";
    private static final String DEBUG_CONCEPT_ID = "debugConceptId";
    private static final String DEBUG_TYPE_ID = "debugTypeId";

    private String base;

    private UrlMapper() {
        this.base = "http://localhost";
    }

    private UrlMapper(String base) {
        this();
        checkNotNull(base);
        this.base = OdtUtils.removeTrailingSlash(base);
    }

    /**
     * Values less than -1 are automatically converted to -1
     */
    private long parseId(String s) {
        checkNotNull(s);
        long ret = Long.parseLong(s);
        if (ret >= -1) {
            return ret;
        } else {
            LOG.warn("Found id " + ret + " which is less than -1, automatically converting it to -1");
            return -1;
        }
    }

    /**
     * For extracting i.e. id '123' from
     * http://my-website.com/concepts/123?some-param=bla
     *
     * @param prefix prefix like '/concepts/'
     * @throws IllegalArgumentException on invalid URL
     */
    private long parseIdFromPrefix(String prefix, String url) {
        URL u;
        try {
            u = new URL(url);
        }
        catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Found invalid url: " + url, ex);
        }

        String urlWithoutQuery = u.getProtocol() + "://" + u.getAuthority() + u.getPath();
        String toStrip = base + prefix + "/";

        String s;
        if (toStrip.length() > 0) {
            int pos = urlWithoutQuery.indexOf(toStrip);
            if (pos != 0) {
                throw new IllegalArgumentException("Invalid URL for prefix " + prefix + ": " + url);
            }
            s = urlWithoutQuery.substring(toStrip.length());
        } else {
            s = urlWithoutQuery;
        }
        try {
            return parseId(s);
        }
        catch (Exception ex) {
            throw new IllegalArgumentException("Invalid URL for prefix " + prefix + ": " + url, ex);
        }
    }

    /**
     * @throws IllegalArgumentException on unparseable URL
     */
    public long urlToConceptId(String url) {
        return parseIdFromPrefix(CONCEPT_PREFIX, url);
    }

    /**
     * Extracts from multimap the first item correspond to {@code property}
     *
     * @throws IllegalArgumentException if there is not exactly one {
     * @property}
     */
    private String getFirst(Multimap<String, String> m, String property) {
        int sz = m.get(property).size();
        if (sz != 1) {
            throw new IllegalArgumentException("Expected one " + property + ", found " + sz + " instead.");
        } else {
            return Iterables.getFirst(m.get(property), "");
        }
    }

    /**
     * For an explanation of global vs local id, see {@link #conceptIdToUrl(java.lang.Long, java.lang.Long)
     * }
     *
     * @see #conceptUrlToId(java.lang.String)
     */
    public long conceptUrlToGlobalId(String url) {
        return parseIdFromParam(DEBUG_GLOBAL_CONCEPT_ID, url);
    }

    /**
     * For an explanation of global vs local id, see {@link #conceptIdToUrl(java.lang.Long, java.lang.Long)
     * }
     *
     * @see #conceptUrlToGlobalId(java.lang.String)
     */
    public long conceptUrlToId(String url) {
        return parseIdFromPrefix(CONCEPT_PREFIX, url);
    }

    /**
     * Returns the etype id as an url.
     *
     * @param etypeId if unknown use -1     
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String etypeIdToUrl(Long etypeId) {
        checkValidId(etypeId, "Invalid etype id!");        
        checkCongruent(etypeId);
        return base + ETYPE_PREFIX + "/" + etypeId;
    }

    /**
     *
     * @throws IllegalArgumentException on unparseable URL
     */
    public long etypeUrlToId(String url) {
        return parseIdFromPrefix(ETYPE_PREFIX, url);
    }

  

    private long parseIdFromParam(String paramName, String url) {
        Multimap<String, String> params = OdtUtils.parseUrlParams(url);
        return parseId(getFirst(params, paramName));
    }

    /**
     *
     * @throws IllegalArgumentException on unparseable URL
     */
    public long entityUrlToId(String url) {
        return parseIdFromPrefix(ENTITY_PREFIX, url);
    }

    /**
     * Returns the entity id as an url
     *
     * @param id if unknown use -1
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String entityIdToUrl(Long id) {
        checkValidId(id, "Invalid entity id!");
        return base + ENTITY_PREFIX + "/" + id;
    }

    /**
     *
     * @throws IllegalArgumentException on unparseable URL
     */
    public long entityNewUrlToId(String url) {
        return parseIdFromPrefix(ENTITY_PREFIX + "/new", url);
    }

    /**
     * Returns the entity id as an url
     *
     * @param id if unknown use -1
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String entityNewIdToUrl(Long id) {
        checkValidId(id, "Invalid entity id!");
        return base + ENTITY_PREFIX + "/new/" + id;
    }
    
    
    private void checkCongruent(long... ids) {
        boolean foundMinusOne = false;
        for (long id : ids) {
            if (id == -1) {
                foundMinusOne = true;
            } else {
                if (foundMinusOne) {
                    throw new IllegalArgumentException("All ids must be either -1 or proper ids");
                }
            }
        }
    }

    private void checkValidId(Long id, @Nullable String prependedErrorMessage) {
        if (id == null) {
            throw new IllegalArgumentException(String.valueOf(prependedErrorMessage) + " - Found null id!");
        }
        if (id < -1) {
            throw new IllegalArgumentException(String.valueOf(prependedErrorMessage) + " - Found id less than -1: " + id);
        }
    }

    /**
     * Returns the concept id as an url . Concepts for some reason have TWO ids,
     * one local and one global. The local one seems the 'most' important, so we
     * use that one for urls. Notice column recognizers seems to prefer the
     * global ids, so hacks are needed just for it. Bleah.
     *
     * @param id if unknown use -1
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String conceptIdToUrl(Long id) {
        checkValidId(id, "Invalid concept id!");                
        return base + CONCEPT_PREFIX + "/" + id;
    }

    /**
     * Returns the attr def id as an url. Sometimes it may be useful to identify
     * attribute definitioins with their concepts, for this reason we force the
     * presence of the (local) concept id in the url
     *
     * @param attrDefId if unknown use -1
     * @param globalId If unknown use -1
     */
    // using Long as param instead of long because with the latter it might 
    // be hard to interpret implicit cast failures.
    public String attrDefIdToUrl(Long attrDefId, Long conceptId) {
        checkValidId(attrDefId, "Invalid concept id!");
        checkValidId(conceptId, "Invalid concept id!");
        checkCongruent(attrDefId, conceptId);
        return base + ATTR_DEF_PREFIX + "/" + attrDefId + "?" + DEBUG_CONCEPT_ID + "=" + conceptId;
    }

    /**
     * For an explanation about ids, see {@link #attrDefIdToUrl(java.lang.Long, java.lang.Long)
     * }
     *
     * @throws IllegalArgumentException on unparseable URL
     *
     * @see #attrDefUrlToConceptId(java.lang.String)
     */
    public long attrDefUrlToId(String url) {
        return parseIdFromPrefix(ATTR_DEF_PREFIX, url);
    }

    /**
     * For an explanation about ids, see {@link #attrDefIdToUrl(java.lang.Long, java.lang.Long)
     * }
     *
     * @throws IllegalArgumentException on unparseable URL
     *
     * @see #attrDefUrlToId(java.lang.String)
     */
    public long attrDefUrlToConceptId(String url) {
        return parseIdFromParam(DEBUG_CONCEPT_ID, url);
    }

    public boolean isEntityURL(String entityUrl) {
        checkNotEmpty(entityUrl, "Invalid url!");
        return entityUrl.contains(ENTITY_PREFIX);
    }

    public boolean isConceptUrl(String conceptUrl) {
        checkNotEmpty(conceptUrl, "Invalid url!");
        return conceptUrl.contains(CONCEPT_PREFIX);
    }

    public  boolean isEtypeUrl(String etypeUrl) {
        checkNotEmpty(etypeUrl, "Invalid url!");
        return etypeUrl.contains(ETYPE_PREFIX);
    }

    public boolean isAttrDefURL(String attrDefUrl) {
        checkNotEmpty(attrDefUrl, "Invalid url!");
        return attrDefUrl.contains(ATTR_DEF_PREFIX);
    }

    /**
     * Returns default mapper with localhost address.
     */
    public static UrlMapper of() {
        return INSTANCE;
    }

    /**
     * Returns an UrlMapper with provided endpoint url, like
     * 'http://entitypedia.org/api'
     */
    public static UrlMapper of(String baseUrl) {
        return new UrlMapper(baseUrl);
    }
}
