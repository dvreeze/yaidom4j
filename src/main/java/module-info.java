module eu.cdevreeze.yaidom4j {
    requires transitive com.google.common;
    requires transitive java.xml;
    requires org.jspecify;

    exports eu.cdevreeze.yaidom4j.core;
    exports eu.cdevreeze.yaidom4j.dom.ancestryaware;
    exports eu.cdevreeze.yaidom4j.dom.clark;
    exports eu.cdevreeze.yaidom4j.dom.clark.jaxpinterop;
    exports eu.cdevreeze.yaidom4j.dom.immutabledom;
    exports eu.cdevreeze.yaidom4j.dom.immutabledom.jaxpinterop;
    exports eu.cdevreeze.yaidom4j.jaxp;
    exports eu.cdevreeze.yaidom4j.queryapi;
    exports eu.cdevreeze.yaidom4j.transformationapi;
}
