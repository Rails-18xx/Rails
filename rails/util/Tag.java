/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Tag.java,v 1.16 2010/04/30 15:22:42 evos Exp $*/
package rails.util;

import java.io.IOException;
import java.util.*;

import javax.xml.parsers.*;

import org.apache.log4j.Logger;
import org.w3c.dom.*;
import org.xml.sax.SAXException;

import rails.game.ConfigurationException;
import rails.game.GameOption;

/**
 * Each object of this class both contains and represents a DOM Element object.
 * Its purpose it to hide the XML parsing details from the application program.
 * The methods of this class intend to replace the corresponding methods in
 * XmlUtils.
 *
 * @author Erik Vos
 *
 */
public class Tag {

    private Element element = null;
    private Map<String, String> attributes = null;
    private Map<String, List<Tag>> children = null;
    private String text = null;
    private boolean parsed = false;
    private boolean parsing = false;

    /** A reference to the current game chosen options */
    private Map<String, String> gameOptions = null;

    protected static Logger log =
            Logger.getLogger(Tag.class.getPackage().getName());

    public Tag(Element element) {
        this.element = element;
    }

    public Tag (Element element, Map<String, String> gameOptions) {
        this (element);
        this.gameOptions = gameOptions;
    }

    public void setGameOptions (Map<String, String> gameOptions) {
        this.gameOptions = gameOptions;
    }

    public Map<String, String> getGameOptions () {
        return gameOptions;
    }

    public Map<String, List<Tag>> getChildren() throws ConfigurationException {

        if (!parsed) parse(element);

        return children;
    }

    /**
     * Return all child Elements with a given name of an Element.
     *
     * @param element
     * @param tagName
     * @return
     * @throws ConfigurationException
     */
    public List<Tag> getChildren(String tagName) throws ConfigurationException {

        if (!parsed) parse(element);

        return children.get(tagName);
    }

    /**
     * Return the (first) child Element with a given name from an Element.
     *
     * @param element
     * @param tagName
     * @return
     * @throws ConfigurationException
     */
    public Tag getChild(String tagName) throws ConfigurationException {

        if (!parsed) parse(element);

        List<Tag> list = children.get(tagName);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public boolean hasChild (String tagName) throws ConfigurationException {

        //br: this was hardcoded  to "AllowsMultipleBasesOfOneCompany" -- looks like a bug.
        return getChildren(tagName) != null;
    }

    public String getText() throws ConfigurationException {

        if (!parsed) parse(element);

        return text;
    }

    public Map<String, String> getAttributes() throws ConfigurationException {

        if (!parsed) parse(element);

        return attributes;

    }

    public String getAttributeAsString(String name, String defaultValue)
            throws ConfigurationException {

        if (!parsed) parse(element);

        String value = attributes.get(name);
        if (value == null) return defaultValue;
        return value;
    }

    public String getAttributeAsString(String name)
            throws ConfigurationException {

        return getAttributeAsString(name, null);
    }

    public int getAttributeAsInteger(String name, int defaultValue)
            throws ConfigurationException {

        if (!parsed) parse(element);

        String value = attributes.get(name);
        if (value == null) return defaultValue;
        try {
            return Integer.parseInt(value);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid integer value: " + value,
                    e);
        }
    }

    public float getAttributeAsFloat(String name) throws ConfigurationException {

        return getAttributeAsFloat(name, 0.0f);
    }

    public float getAttributeAsFloat(String name, float defaultValue)
    throws ConfigurationException {

        if (!parsed) parse(element);

        String value = attributes.get(name);
        if (value == null) return defaultValue;
        try {
            return Float.parseFloat(value);
        } catch (Exception e) {
            throw new ConfigurationException("Invalid floating point value: " + value,
                    e);
        }
    }

public int getAttributeAsInteger(String name) throws ConfigurationException {

return getAttributeAsInteger(name, 0);
}

    public int[] getAttributeAsIntegerArray(String name, int[] defaultArray)
            throws ConfigurationException {

        String valueString = getAttributeAsString(name);
        if (!Util.hasValue(valueString)) return defaultArray;

        String[] values = valueString.split(",");
        int[] result = new int[values.length];
        int i = 0;
        try {
            for (i = 0; i < values.length; i++) {
                result[i] = Integer.parseInt(values[i]);
            }
        } catch (NumberFormatException e) {
            throw new ConfigurationException("Invalid integer '" + values[i]
                                             + "' in attribute '" + name + "'");
        }
        return result;

    }

    public int[] getAttributeAsIntegerArray(String name)
            throws ConfigurationException {

        return getAttributeAsIntegerArray(name, new int[0]);
    }

    public boolean getAttributeAsBoolean(String name, boolean defaultValue)
            throws ConfigurationException {

        if (!parsed) parse(element);

        String value = attributes.get(name);
        if (value == null) return defaultValue;

        return value.matches("^[TtYy].*");
    }

    public boolean getAttributeAsBoolean(String name)
            throws ConfigurationException {

        return getAttributeAsBoolean(name, false);
    }
    
    // br: needed to test if a Tradeable tag has a toCompany or toPlayer attribute
    public boolean hasAttribute(String name)
        throws ConfigurationException {

        return getAttributeAsString(name) != null;
    }
    /**
     * Extract all attributes of an Element into a HashMap. This includes
     * conditional values, embedded in (possibly nested) &lt;IfOption&gt;
     * subnodes. <p> The generic XML construct being parsed here must look like:<p>
     * <code>
     * &lt;AnyElement attr1="value1" attr2="value2" ...&gt;
     *   &lt;IfOption name="optname1" value="optvalue1"&gt;
     *     &lt;IfOption name="optname2" value="optvalue2"&gt;
     *       &lt;Attributes attr3="value3" attr4="value4"/&gt;
     *     &lt;/IfOption&gt;
     *   &lt;/IfOption&gt;
     * &lt;/AnyElement&gt;
     * </code>
     * <p> For variant names, the fixed option name "variant" is used.
     *
     * @param element
     * @return
     */
    private synchronized void parse(Element element)
            throws ConfigurationException {

        if (parsed || parsing) return;
        parsing = true;

        attributes = new HashMap<String, String>();
        children = new HashMap<String, List<Tag>>();

        NamedNodeMap nnp = element.getAttributes();
        Node attribute;
        String name, value;
        for (int i = 0; i < nnp.getLength(); i++) {
            attribute = nnp.item(i);
            name = attribute.getNodeName();
            value = attribute.getNodeValue();
            attributes.put(name, value);
        }

        parseSubTags(element);

    }

    private void parseSubTags(Element element) throws ConfigurationException {

        NodeList childNodes = element.getChildNodes();
        Node childNode;
        Element childElement;
        String childTagName;
        Node attribute;
        String name, value;
        List<String> valueList;
        StringBuffer textBuffer = new StringBuffer();

        for (int i = 0; i < childNodes.getLength(); i++) {
            childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) {
                childElement = (Element) childNode;
                childTagName = childElement.getNodeName();
                NamedNodeMap nnp = childElement.getAttributes();
                if (childTagName.equalsIgnoreCase("Attributes")) {
                    for (int j = 0; j < nnp.getLength(); j++) {
                        attribute = nnp.item(j);
                        name = attribute.getNodeName();
                        value = attribute.getNodeValue();
                        attributes.put(name, value);
                    }
                } else if (childTagName.equalsIgnoreCase("IfOption")) {
                    Node nameAttr = nnp.getNamedItem("name");
                    if (nameAttr == null)
                        throw new ConfigurationException(
                                "IfOption has no optionName attribute");
                    name = nameAttr.getNodeValue();

                    Node parmAttr = nnp.getNamedItem("parm");
                    if (parmAttr != null) {
                    	value = parmAttr.getNodeValue();
                    	name = GameOption.constructParametrisedName(name, value.split(","));
                    }
                    Node valueAttr = nnp.getNamedItem("value");
                    if (valueAttr == null)
                        throw new ConfigurationException(
                                "IfOption has no optionValue attribute");
                    value = valueAttr.getNodeValue();
                    valueList = Arrays.asList(value.split(","));

                    // Check if the option has been chosen; if not, skip the
                    // rest
                    if (gameOptions == null) {
                        throw new ConfigurationException (
                                "No GameOptions available in tag "+element.getNodeName());
                    }

                    String optionValue = gameOptions.get(name);

                	// For backwards compatibility: search for an extended name
                    if (optionValue == null) {
                    	for (String optName : gameOptions.keySet()) {
                    		if (optName.startsWith(name)) {
                    			optionValue = gameOptions.get(optName);
                    			log.warn("Option name "+name+" replaced by "+optName);
                    			break;
                    		}
                    	}
                    }

                    if (optionValue == null) {
                        // Take the default value
                        GameOption go = GameOption.getByName(name);
                        optionValue = go != null ? go.getDefaultValue() : "";
                        log.warn("GameOption " + name + "=" + value
                                 + " but no assigned value found, assumed "+optionValue);

                    }

                    //if (optionValue.equalsIgnoreCase(value)) {
                    if (valueList.contains(optionValue)) {
                        parseSubTags(childElement);
                    }
                } else {
                    if (!children.containsKey(childTagName)) {
                        children.put(childTagName, new ArrayList<Tag>());
                    }
                    children.get(childTagName).add(new Tag(childElement, gameOptions));
                }
            } else if (childNode.getNodeType() == Node.TEXT_NODE) {
                textBuffer.append(childNode.getNodeValue());
            }
        }

        text = textBuffer.toString();
        parsed = true;
        parsing = false;
    }

    /**
     * Opens and parses an xml file. Searches the root level of the file for an
     * element with the supplied name.
     *
     * @param fileName the name of the file to open
     * @param tagName the name of the top-level tag to find
     * @return the named element in the named file
     * @throws ConfigurationException if there is any problem opening and
     * parsing the file, or if the file does not contain a top level element
     * with the given name.
     */
    public static Tag findTopTagInFile(String filename, List<String> directories,
            String tagName) throws ConfigurationException {
        Document doc = null;
        try {
            // Step 1: create a DocumentBuilderFactory and setNamespaceAware
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // Step 2: create a DocumentBuilder
            DocumentBuilder db = dbf.newDocumentBuilder();

            // Step 3: parse the input file to get a Document object
            doc =
                    db.parse(ResourceLoader.getInputStream(filename,
                            directories));
        } catch (ParserConfigurationException e) {
            throw new ConfigurationException("Could not read/parse " + filename
                                             + " to find element " + tagName, e);
        } catch (SAXException e) {
            throw new ConfigurationException("Could not read/parse " + filename
                                             + " to find element " + tagName, e);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read/parse " + filename
                                             + " to find element " + tagName, e);
        }

        if (doc == null) {
            throw new ConfigurationException("Cannot find file " + filename);
        }

        // Now find the named Element
        NodeList nodeList = doc.getChildNodes();
        for (int iNode = 0; (iNode < nodeList.getLength()); iNode++) {
            Node childNode = nodeList.item(iNode);
            if ((childNode != null)
                && (childNode.getNodeName().equals(tagName))
                && (childNode.getNodeType() == Node.ELEMENT_NODE)) {
                return new Tag((Element) childNode);
            }
        }
        throw new ConfigurationException("Could not find " + tagName + " in "
                                         + filename);
    }

    public Element getElement() {
        return element;
    }
}
