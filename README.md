# Yaidom4j

**Y**et **a**nother **I**mmutable (XML) **DOM** for **J**ava

## Introduction

**Yaidom4j** is a small and easy-to-use tree-based Java XML library. It requires Java 25 or later.

It mainly offers the following features:
* An *immutable* and *thread-safe* XML DOM implementation
* An easy-to-use *generic node query API*, leveraging the *Java Stream API*
* A similar easy-to-use *generic element node query API*, as a convenience API

The node/element query APIs have multiple implementations, and more implementations can always be added.
One such implementation is backed by the above-mentioned immutable DOM elements.

Yaidom4j is **not** any of the following:
* An O-X mapper, such as JAXB
* A schema validator, against an XSD or DTD (yaidom4j itself is completely unaware of any schema information)
* An XPath or XQuery processor
* An HTML library

Some of many use cases for yaidom4j include the following:
* Light-weight ad-hoc XML querying, requiring no involved bootstrapping of an XPath processor (including bootstrapping of function libraries)
* Light-weight ad-hoc XML transformation scripts, such as those for updating XML namespaces or namespace prefixes
* Keeping potentially large XBRL taxonomies in memory as thread-safe immutable DOM trees, for querying across XBRL taxonomy documents

## Example code

Suppose we have the following XML document, in file `bookstore.xml`:

```xml
<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
    <Book ISBN="ISBN-0-13-713526-2" Price="85" Edition="3rd">
        <Title>A First Course in Database Systems</Title>
        <Authors>
            <Author>
                <First_Name>Jeffrey</First_Name>
                <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
                <First_Name>Jennifer</First_Name>
                <Last_Name>Widom</Last_Name>
            </Author>
        </Authors>
    </Book>
    <Book ISBN="ISBN-0-13-815504-6" Price="100">
        <Title>Database Systems: The Complete Book</Title>
        <Authors>
            <Author>
                <First_Name>Hector</First_Name>
                <Last_Name>Garcia-Molina</Last_Name>
            </Author>
            <Author>
                <First_Name>Jeffrey</First_Name>
                <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
                <First_Name>Jennifer</First_Name>
                <Last_Name>Widom</Last_Name>
            </Author>
        </Authors>
        <Remark>Buy this book bundled with "A First Course" - a great deal!
        </Remark>
    </Book>
    <Book ISBN="ISBN-0-11-222222-3" Price="50">
        <Title>Hector and Jeff's Database Hints</Title>
        <Authors>
            <Author>
                <First_Name>Jeffrey</First_Name>
                <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
                <First_Name>Hector</First_Name>
                <Last_Name>Garcia-Molina</Last_Name>
            </Author>
        </Authors>
        <Remark>An indispensable companion to your textbook</Remark>
    </Book>
    <Book ISBN="ISBN-9-88-777777-6" Price="25">
        <Title>Jennifer's Economical Database Hints</Title>
        <Authors>
            <Author>
                <First_Name>Jennifer</First_Name>
                <Last_Name>Widom</Last_Name>
            </Author>
        </Authors>
    </Book>
    <Magazine Month="January" Year="2009">
        <Title>National Geographic</Title>
    </Magazine>
    <Magazine Month="February" Year="2009">
        <Title>National Geographic</Title>
    </Magazine>
    <Magazine Month="February" Year="2009">
        <Title>Newsweek</Title>
    </Magazine>
    <Magazine Month="March" Year="2009">
        <Title>Hector and Jeff's Database Hints</Title>
    </Magazine>
</books:Bookstore>
```

Then we can obtain the February magazine titles (in a namespace-aware manner) as follows:

```java
import eu.cdevreeze.yaidom4j.dom.Document;
import eu.cdevreeze.yaidom4j.dom.Element;
import eu.cdevreeze.yaidom4j.parse.DocumentParser;
import eu.cdevreeze.yaidom4j.parse.DocumentParsers;

import java.nio.file.Path;
import java.util.List;

import static eu.cdevreeze.yaidom4j.dom.Element.*;

DocumentParser parser = DocumentParsers.instance();
Document doc = parser.parse(Path.of("bookstore.xml").toUri());

var ns = "http://bookstore";

List<String> februaryMagazineTitles = doc.documentElement()
        .childElementStream(hasName(ns, "Magazine"))
        .filter(hasAttributeValue("Month", "February"))
        .flatMap(e -> e.childElementStream(hasName(ns, "Title")))
        .map(Element::text)
        .toList();

// [National Geographic, Newsweek]
```

As shown above, XML querying in yaidom4j is *Java Stream processing*. It's just that yaidom4j adds
*Stream factories* corresponding to XPath axes.

The same querying stream pipeline above (but with different imports and bootstrapping, of course)
works for multiple yaidom4j element implementations, and not just for the default immutable DOM
implementation.

## Characteristics of yaidom4j

Some characteristics of yaidom4j are as follows:
* It is foremost an XML DOM *node/element query API*, with multiple implementations backed by different DOM implementations
* It is also an *immutable thread-safe* DOM implementation (offering the node/element query API mentioned before)
* It requires Java 25 or later
* It depends on *effectively immutable collections*, using [unmodifiable collections](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Collection.html#unmodifiable)
  * So no dependency on Guava and its immutable collections is needed
  * Unlike Guava immutable collections, these immutable collections do not express immutability in terms of their collection types
  * Instead, factory methods like `List.copyOf`, `List.of`, `Map.copyOf`,`Map.of` and `Stream.toList` express and enforce immutability
  * These effectively immutable collections are also thread-safe; see for example [JEP 269](https://openjdk.org/jeps/269) and [creating immutable collections](https://docs.oracle.com/javase/10/core/creating-immutable-lists-sets-and-maps.htm#JSCOR-GUID-4F3E2B7D-CE90-4862-A78A-414FC08DA6E4)
  * Note the difference between [unmodifiable collections](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Collection.html#unmodifiable) and [unmodifiable view collections](https://docs.oracle.com/en/java/javase/25/docs/api/java.base/java/util/Collection.html#unmodview)
* It has a *small conceptual surface area*, and a small, rather *stable API*
* It is *not* a schema validator (against DTD or XSD), nor is it an O-X mapper, or XPath/XQuery processor, or HTML library
* The *node/element query API* mostly returns *Java Streams*, which can be used in Java Stream pipelines
* Some of those Java node/element Stream factory methods are *inspired by XPath axes*
* The *immutable DOM elements* can even be transformed recursively; formatting of the resulting XML could then be left to *xmllint*, for example
* The library does not try to implement XML standards, but tries to make XML easier to use in some respects
* In particular, it knows nothing about DTDs or XSDs
* Also, it only partially "implements" the *XML InfoSet* (see below)
* It "respects" *XML namespaces* (see [Understanding XML Namespaces](https://www.lenzconsulting.com/namespaces/))
* It leaves parsing and printing of XML DOM trees to *JAXP* (or Saxon)
* Yet it does offer some "secure JAXP factory object" creation support
* It can be used with StAX for keeping only chunks of XML in memory one chunk at a time, if the structure of the overall XML allows it

Unlike libraries like [Saxon](https://www.saxonica.com/download/java.xml), yaidom4j does not have the aim of
implementing XML standards. It does have the aim of making (non-schema-aware or schema-aware) XML querying simple and practical.
Keeping this limited scope in mind, comparisons to some other XML libraries are as follows:
* Unlike JDOM, DOM4J, XOM, JAXP DOM, the native node implementations in yaidom4j are deeply immutable and thread-safe
* Like JAXP DOM, yaidom4j can represent "mixed content" and not just data-oriented XML documents
* Unlike JAXP XPath (1.0) support, the node/element query API in yaidom4j is very easy to use without any involved bootstrapping
* Like *Saxon* a Java Stream-based query API is offered

As said above, yaidom4j does not try to implement the full XML InfoSet in its *immutable DOM* implementation. For example:
* The library contains *no unexpanded entity references*
* It contains *no DTD information items*
* It contains *no notation information items*

Yaidom4j is more like the [XPath Data Model](https://www.w3.org/TR/xpath-datamodel-31/#Node) than the [XML Information Set](https://www.w3.org/TR/2004/REC-xml-infoset-20040204/). The XPath Data Model (only) contains
the following kinds of nodes:
* documents
* elements
* attributes
* text
* namespace
* processing instruction
* comment

So, the XDM contains no entity references etc. The Saxon library seems to follow the XDM rather than the
Info Set. So does yaidom4j. Unlike XDM (and Saxon), yaidom4j native node implementations do leave out many properties
of the nodes in XDM. Again, yaidom4j is not about implementing XML standards, but about easy processing of (most) XML.

## Evolving the library

In a broad sense, yaidom4j is envisioned to remain roughly what it is now. Yet there is still a lot to
improve. See for example the TODOs below.

But first let's mention recent API changes:
* The node/element API has been refactored in order to more closely follow the XPath Data Model (e.g. documents are nodes too)
* This also means that yaidom4j now has a general node query API (with common "XPath axes")
* The element query API is now a convenience query API, that usually happens to be more practical (i.e. type-safe) in practice than the general node query API
* After all, only documents and elements can have children, and therefore descendant nodes
* The distinction between ancestry-aware nodes and non-ancestry-aware nodes is now inspired by so-called "red-green trees"
* For more on red-green trees, see [Roslyn compiler's red-green trees](https://ericlippert.com/2012/06/08/red-green-trees/)
* Our "green trees" are not internal implementation details, but regular ("ancestry-aware") nodes are "red", and the "non-ancestry-aware" nodes are explicitly defined as "green" nodes
* So now we have regular "red" nodes and underlying "green" nodes, from which we can derive "green Clark" nodes; the same 3 kinds as before but with different names

TODO:
* Stabilize API (and keep it small and simple)
  * For tweaks to the API, we may get some inspiration from the upcoming simple JSON API that will be part of the JDK
* Improve performance, but not at the expense of clarity and simplicity of the API
* Add useful "scripts" (or should we keep them outside the main source code tree?)
* Add and improve documentation
* Improve the process around maintaining and releasing the library; e.g.:
  * Release notes
  * Automatic dependency updates (?)
  * Code analysis, security checks, test coverage
  * etc.
