# Yaidom4j

**Y**et **a**nother **I**mmutable (XML) **DOM** for **J**ava

## Introduction

**Yaidom4j** is a small and easy-to-use Java XML library. It requires Java 17 or later.

It mainly offers the following features:
* An *immutable* and *thread-safe* XML DOM implementation, leveraging *Guava immutable collections*
* An easy-to-use *generic element query API*, leveraging the *Java Stream API*

The element query API has multiple implementations, and more implementations can always be added.
One such implementation is backed by the above-mentioned immutable DOM elements.

Yaidom4j is **not** any of the following:
* An O-X mapper, such as JAXB
* A schema validator, against an XSD or DTD (yaidom4j is completely unaware of any schema information)
* An XPath or XQuery processor
* An HTML library

Some of many use cases for yaidom4j include the following:
* Light-weight ad-hoc XML querying, requiring no involved bootstrapping of an XPath processor (including bootstrapping of function libraries)
* Light-weight ad-hoc XML transformation scripts, such as those for updating XML namespaces or namespace prefixes
* Keeping potentially large XBRL taxonomies in memory as thread-safe immutable DOM trees, for querying across XBRL taxonomy documents

## Example code

TODO

## Characteristics of yaidom4j

Some characteristics of yaidom4j are as follows:
* It is foremost an XML DOM *element query API*, with multiple implementations backed by different DOM implementations
* It is also an *immutable thread-safe* DOM implementation (offering the element query API mentioned before)
* It requires Java 17 or later
* It depends on *Guava* for its *immutable collections*, and has no other dependencies
* It has a *small conceptual surface area*, and a small, rather *stable API*
* It is *not* a schema validator (against DTD or XSD), nor is it an O-X mapper, or XPath/XQuery processor, or HTML library
* The *element query API* mostly returns *Java Streams*, which can be used in Java Stream pipelines
* Some of those Java element Stream factory methods are *inspired by XPath axes*
* The *immutable DOM elements* can even be transformed recursively; formatting of the resulting XML could then be left to *xmllint*, for example
* The library does not try to implement XML standards, but tries to make XML easier to use in some respects
* In particular, it knows nothing about DTDs or XSDs
* Also, it only partially "implements" the *XML InfoSet* (see below)
* It "respects" *XML namespaces* (see [Understanding XML Namespaces](https://www.lenzconsulting.com/namespaces/))
* It leaves parsing and printing of XML DOM trees to *JAXP* (or Saxon), thus avoiding many XML security issues
* It can be used with StAX for keeping only chunks of XML in memory one chunk at a time, if the structure of the overall XML allows it

Comparisons to some other XML libraries are as follows:
* Unlike JDOM, DOM4J, XOM, JAXP DOM, the native element implementations in yaidom4j are deeply immutable and thread-safe
* Unlike JAXP XPath (1.0) support, the element query API in yaidom4j is very easy to use without any involved bootstrapping
* Like *Saxon* a Java Stream-based query API is offered, but unlike Saxon's query API this query API is element-node-centric

As said above, yaidom4j does not try to implement the full XML InfoSet in its *immutable DOM* implementation. For example:
* A *Document* is not treated as a separate *information item*, or *node*
* Neither are *attributes*, which are simply properties of *element nodes*
* Neither are *namespace information items*; instead, element nodes contain the in-scope namespaces as property of the element
* *Namespace attributes* do not exist in yaidom4j; instead, all attributes are "normal attributes" binding a string value to an attribute name
* The library contains *no unexpanded entity references*
* It contains *no DTD information items*
* It contains *no notation information items*

In a way, yaidom4j is more *type-safe* than a "direct" representation of the XML InfoSet, in that the compiler does not allow
text nodes as child nodes of documents. Neither does the compiler allow for element children to be any other node than
element nodes, text nodes, comment nodes or processing instructions.

On the other hand, if desired, an element query API implementation can be created for DOM implementations that
do model XML InfoSet information items that yaidom4j itself is not interested in.
