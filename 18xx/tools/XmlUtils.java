/* $Header: /Users/blentz/rails_rcs/cvs/18xx/tools/XmlUtils.java,v 1.1 2010/02/03 20:16:38 evos Exp $*/
package tools;

import java.io.IOException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import rails.game.ConfigurationException;

/**
 * Booch utility class providing helper functions for working with XML.
 */
public final class XmlUtils {

    /**
     * No-args private constructor, to prevent (meaningless) construction of one
     * of these.
     */
    private XmlUtils() {}

    /**
     * Extracts the String value of a given attribute from a NodeNameMap.
     * Returns null if no such attribute can be found. See
     * extractStringAttribute(NamedNodeMap nnp, String attrName, String
     * defaultValue)
     * 
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired
     * @return the named attribute's value or null if absent.
     */
    /** @deprecated */
    public static String extractStringAttribute(NamedNodeMap nnp,
            String attrName) {
        return extractStringAttribute(nnp, attrName, null);
    }

    /**
     * Extracts the String value of a given attribute from a NodeNameMap.
     * Returns a default value if no such attribute can be found.
     * 
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired
     * @param defaultValue the value to be returned if the attribute is absent.
     * @return the named attribute's value, or the default value if absent.
     */
    /** @deprecated */
    public static String extractStringAttribute(NamedNodeMap nnp,
            String attrName, String defaultValue) {

        if (nnp == null) return defaultValue;
        Node nameAttr = nnp.getNamedItem(attrName);
        if (nameAttr == null) return defaultValue;
        return nameAttr.getNodeValue();
    }

    /**
     * Extracts the integer value of a given attribute from a NodeNameMap.
     * Returns zero if no such attribute can be found.
     * 
     * @see
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired
     * @return the named attribute's value, or zero if absent.
     */
    /** @deprecated */
    public static int extractIntegerAttribute(NamedNodeMap nnp, String attrName)
            throws ConfigurationException {
        return extractIntegerAttribute(nnp, attrName, 0);
    }

    /**
     * Extracts the integer value of a given attribute from a NodeNameMap.
     * Returns a default value if no such attribute can be found.
     * 
     * @see
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired.
     * @param defaultValue The value returned if the attribute is absent.
     * @return the named attribute's value or the dedault value.
     */
    /** @deprecated */
    public static int extractIntegerAttribute(NamedNodeMap nnp,
            String attrName, int defaultValue) throws ConfigurationException {

        if (nnp == null) return defaultValue;
        Node nameAttr = nnp.getNamedItem(attrName);
        if (nameAttr == null) {
            return defaultValue;
        }
        String value = nameAttr.getNodeValue();
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid integer value: " + value,
                    e);
        }
    }

    /**
     * Extracts the boolean value of a given attribute from a NodeNameMap. Any
     * string that starts with T or t (for "true") or Y or y (for "yes") is
     * considered to represent true, all other values will produce false.
     * 
     * @param nnp The NodeNameMap to search for the Attribute
     * @param attrName The name of the attribute who's value is desired
     * @return The named attribute's value, or false if absent.
     */
    /** @deprecated */
    public static boolean extractBooleanAttribute(NamedNodeMap nnp,
            String attrName) throws ConfigurationException {
        return extractBooleanAttribute(nnp, attrName, false);
    }

    /**
     * Extracts the boolean value of a given attribute from a NodeNameMap.
     * Returns a default value if no such attribute can be found. Any string
     * that starts with T or t (for "true") or Y or y (for "yes") is considered
     * to represent true, all other values will produce false.
     * 
     * @param nnp The NodeNameMap to search for the Attribute
     * @param attrName The name of the attribute who's value is desired
     * @param defaultValue The value returned if the attribute is absent.
     * @return The named attribute's value or the default value.
     */
    /** @deprecated */
    public static boolean extractBooleanAttribute(NamedNodeMap nnp,
            String attrName, boolean defaultValue)
            throws ConfigurationException {

        if (nnp == null) return defaultValue;
        Node nameAttr = nnp.getNamedItem(attrName);
        if (nameAttr == null) {
            return defaultValue;
        }
        String value = nameAttr.getNodeValue();
        return value.matches("^[TtYy].*");
    }

    /** @deprecated */
    public static int[] extractIntegerArrayAttribute(NamedNodeMap nnp,
            String attrName) throws ConfigurationException {

        if (nnp == null) return null;
        Node nameAttr = nnp.getNamedItem(attrName);
        if (nameAttr == null) return new int[0];
        String[] values = nameAttr.getNodeValue().split(",");
        int[] result = new int[values.length];
        int i = 0;
        try {
            for (i = 0; i < values.length; i++) {
                result[i] = Integer.parseInt(values[i]);
            }
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid integer '" + values[i]
                                             + "' in attribute '" + attrName
                                             + "'");
        }
        return result;

    }

    /**
     * Opens and parses an xml file. Searches the root level of the file for an
     * element with the supplied name.
     * 
     * @param fileName the name of the file to open
     * @param elementName the name of the element to find
     * @return the named element in the named file
     * @throws ConfigurationException if there is any problem opening and
     * parsing the file, or if the file does not contain a top level element
     * with the given name.
     */
    public static Element findElementInFile(String fileName, String elementName)
            throws ConfigurationException {
        Document doc = null;
        try {
            // Step 1: create a DocumentBuilderFactory and setNamespaceAware
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // Step 2: create a DocumentBuilder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Step 3: parse the input file to get a Document object
            doc = db.parse(Util.getStreamForFile(fileName));

        } catch (ParserConfigurationException e) {
            throw new ConfigurationException("Could not read/parse " + fileName
                                             + " to find element "
                                             + elementName, e);
        } catch (SAXException e) {
            throw new ConfigurationException("Could not read/parse " + fileName
                                             + " to find element "
                                             + elementName, e);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read/parse " + fileName
                                             + " to find element "
                                             + elementName, e);
        }

        if (doc == null) {
            throw new ConfigurationException("Cannot find file " + fileName);
        }

        // Now find the named Element
        NodeList nodeList = doc.getChildNodes();
        for (int iNode = 0; (iNode < nodeList.getLength()); iNode++) {
            Node childNode = nodeList.item(iNode);
            if ((childNode != null)
                && (childNode.getNodeName().equals(elementName))
                && (childNode.getNodeType() == Node.ELEMENT_NODE)) {
                return (Element) childNode;
            }
        }
        throw new ConfigurationException("Could not find " + elementName
                                         + " in " + fileName);
    }
}
