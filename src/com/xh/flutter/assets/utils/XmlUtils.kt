package com.xh.flutter.assets.utils

import org.apache.commons.lang3.StringUtils
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

object XmlUtils {

    fun parse(path: String): Element {
        val xmlDomFactory = DocumentBuilderFactory.newInstance()
        val document = xmlDomFactory.newDocumentBuilder()
            .parse(path)
        document.getDocumentElement().normalize()
        return document.documentElement
    }

    fun getValueByAttribute(
        element: Element?,
        elementName: String,
        attributeName: String,
        default: String
    ): String {
        if (element == null) {
            return default
        }
        val nodeList = element.getElementsByTagName(elementName)
        for (i in 0 until nodeList.length) {
            val element = nodeList.item(i) as Element
            val attribute = element.getAttribute(attributeName)
            if (!StringUtils.isEmpty(attribute)) {
                return attribute
            }
        }
        return default
    }
}