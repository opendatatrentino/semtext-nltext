<p class="jadoc-to-strip">
WARNING: WORK IN PROGRESS - THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE PROJECT WEBSITE
</p>

This release supports the following conversions:

  * NLText to SemText 
  * SemText to SemanticString
  * SemanticString to SemText

### Maven

SemText NLText is available on Maven Central. To use it, put this in the dependencies section of your _pom.xml_:

```
<dependency>
  <groupId>eu.trentorise.opendata.semtext</groupId>
  <artifactId>semtext-nltext</artifactId>
  <version>#{version}</version>            
</dependency>
```

### Usage Examples

#### NLText

```
       NLTextConverter converter = NLTextConverter.of(
               UrlMapper.of("http://mysite.org/entities/", 
                            "http://mysite.org/concepts/")); 
                // when creating semtext, string ids will have these prefixes 
                // followed by the numerical ids of found in nltexts

       // second parameter indicates the nltext tags are supposed to have been 
       // entirely reviewed by a human
       SemText semtext = converter.semText(new NLText("ciao"), true);
```

####  SemanticString
```
        SemanticStringConverter conv = SemanticStringConverter.of(
                UrlMapper.of("http://mysite.org/entities/",
                        "http://mysite.org/concepts/"));
                // when creating semtext, string ids will have these prefixes
        // followed by the numerical ids of found in semantic strings

        // by setting true as second parameter we are instructing the converter
        // that we suppose the SemanticString meanings have been reviewed by a human.
        SemText semtext = conv.semText(new SemanticString("ciao"), true);

        SemanticString semstring = conv.semanticString(SemText.of("ciao"));
```