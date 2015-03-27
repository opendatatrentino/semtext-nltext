<p class="jadoc-to-strip">
WARNING: WORK IN PROGRESS - THIS IS ONLY A TEMPLATE FOR THE DOCUMENTATION. <br/>
RELEASE DOCS ARE ON THE PROJECT WEBSITE
</p>

This release supports the following conversions:

  * NLText to SemText
  * SemText to SemanticString
  * SemanticString to SemText

All of them may be lossy depending on the input objects.

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


#### NLText

This example constructs an `NLText` of one `NLSentence` with one `NLToken` inside
having an `NLMeaning` and then converts it to a `SemText`:


```
    NLTextConverter converter = NLTextConverter.of(
            UrlMapper.of("http://mysite.org/entities/",
                    "http://mysite.org/concepts/"));
            // when creating semtext, string ids will have these prefixes
            // followed by the numerical ids of found in nltext


    String text = "hello world";

    NLText nltext = new NLText("hello world");


    // NLText has sentences:
    NLSentence sentence = new NLSentence(text);
    sentence.setProp(NLTextUnit.PFX, "startOffset", 0);
    sentence.setProp(NLTextUnit.PFX, "endOffset", text.length());

    // Let's create an NLMeaning:
    Set<NLMeaning> meanings = new HashSet<NLMeaning>();
    NLSenseMeaning nlSenseMeaning = new NLSenseMeaning("hello lemma", 1L, "NOUN", 2L, 3, 4, "hello description");        
    nlSenseMeaning.setSummary("hello summary");
    meanings.add(nlSenseMeaning);

    // An NLToken to be converted to SemText Term needs offsets:
    NLToken token = new NLToken("hello", meanings);
    token.setProp(NLTextUnit.PFX, "sentenceStartOffset", 0);
    token.setProp(NLTextUnit.PFX, "sentenceEndOffset", 5);              

    sentence.addToken(token);
    nltext.addSentence(sentence);

    // in the converter with the second parameter we specify NLTest is supposed 
    // to have not been reviewed yet by a human:
    SemText semText = converter.semText(nltext, false);

    // Locale.ROOT is the default locale in SemText:
    assert semText.getLocale().equals(Locale.ROOT); 
    // Locale.ROOT is represented by the empty string.
    assert semText.getLocale().toString().equals(""); 

    // SemText has sentences that contain terms:
    Term term = semText.getSentences().get(0).getTerms().get(0);

    // The meaning status of the term will be either TO_DISAMBIGUATE 
    // or SELECTED as we told the converter NLText was automatically tagged 
    // and has not been reviewed yet by a human
    assert term.getMeaningStatus().equals(MeaningStatus.TO_DISAMBIGUATE);

    // A semtext Term contains meanings:
    Meaning meaning = term.getMeanings().get(0);

    assert meaning.getName().string(Locale.ROOT).equals("hello lemma");
    assert meaning.getDescription().string(Locale.ROOT).equals("hello description");

    // NLTextConverter will include additional metadata in meanings under namespace "nltext":
    NLMeaningMetadata metadata = (NLMeaningMetadata) meaning.getMetadata("nltext");
    assert metadata.getLemma().equals("hello lemma");
    assert metadata.getSummary().equals("hello summary");    
```
