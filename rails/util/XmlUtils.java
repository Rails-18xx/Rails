package rails.util;


import java.io.IOException;
import java.util.*;

import javax.xml.parsers.*;

import org.w3c.dom.*;
import org.xml.sax.SAXException;

import rails.game.ConfigurationException;
import rails.game.Game;

/**
 * Booch utility class providing helper functions for working with XML.
 */
public final class XmlUtils {

    /**
     * No-args private constructor, to prevent (meaningless) construction of one of these. 
     */
    private XmlUtils(){
    }
    
    
    /**
     * Extract all attributes of an Element into a HashMap.
     * This includes conditional values, embedded in (possibly nested)
     * &lt;IfOption&gt; subnodes.
     * <p>
     * The generic XML construct being parsed here must look like:<p>
     * <code>
     * &lt;AnyElement attr1="value1" attr2="value2" ...&gt;
     *   &lt;IfOption name="optname1" value="optvalue1"&gt;
     *     &lt;IfOption name="optname2" value="optvalue2"&gt;
     *       &lt;Attributes attr3="value3" attr4="value4"/&gt;
     *     &lt;/IfOption&gt;
     *   &lt;/IfOption&gt;
     * &lt;/AnyElement&gt;
     * </code>
     * <p>
     * For variant names, the fixed option name "variant" is used.
     * @param element
     * @return
     */
    public static Map<String, String> getAllAttributes (Element element) 
    throws ConfigurationException {
        
        Map<String, String> attributes = new HashMap<String, String>();
        
        NamedNodeMap nnp = element.getAttributes();
        Node attribute;
        String name, value;
        for (int i=0; i<nnp.getLength(); i++) {
            attribute = nnp.item(i);
            name = attribute.getNodeName();
            value = attribute.getNodeValue();
            attributes.put(name, value);
        }
        
        addConditionalAttributes (element, attributes);
        
        return attributes;
    }
    
    private static void addConditionalAttributes (Element element,
            Map<String, String> attributes) 
    throws ConfigurationException {
        
        String name, value;
        NodeList children;
        Node child, attribute;

        // Check the element's subnodes.
        // Finally, process any surviving Attributes nodes.
        children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); i++)
        {
        	child = children.item(i);
        	if (child.getNodeType() != Node.ELEMENT_NODE) continue; 
            Element subElement = (Element) child;
            NamedNodeMap nnp = subElement.getAttributes();
            
            if (subElement.getNodeName().equalsIgnoreCase("Attributes")) {
	            for (int j=0; j<nnp.getLength(); j++) {
	                attribute = nnp.item(j);
	                name = attribute.getNodeName();
	                value = attribute.getNodeValue();
	                attributes.put(name, value);
	            }
            } else if (subElement.getNodeName().equalsIgnoreCase("IfOption")) {
	            name = extractStringAttribute (nnp, "name");
	            value = extractStringAttribute (nnp, "value");
	            if (name == null) {
	                throw new ConfigurationException ("No name attribute found in IfConfig");
	            }
	            if (value == null) {
	                throw new ConfigurationException ("No value attribute found in IfConfig");
	            }
	            // Check if the option has been chosen; if not, skip the rest
	            String optionValue = Game.getGameOption(name);
	            if (optionValue == null) {
	                throw new ConfigurationException ("GameOption "+name+"="+value+" but no assigned value found");
	            }
	            if (optionValue.equalsIgnoreCase(value)) {
	                addConditionalAttributes (subElement, attributes);
	            }
            }
        }
        
    }
    
    
    /**
     * Extracts the String value of a given attribute from a NodeNameMap. 
     * Returns null if no such attribute can be found.
     * See extractStringAttribute(NamedNodeMap nnp, String attrName,
    		String defaultValue)
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired
     * @return the named attribute's value or null if absent.
     */
    public static String extractStringAttribute(NamedNodeMap nnp, String attrName) {
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
    public static String extractStringAttribute(NamedNodeMap nnp, String attrName,
    		String defaultValue) {
        
        if (nnp == null) return defaultValue;
        Node nameAttr = nnp.getNamedItem(attrName);
        if (nameAttr == null) return defaultValue;
        return nameAttr.getNodeValue();        
    }

    /**
     * Extracts the integer value of a given attribute from a NodeNameMap. 
     * Returns zero if no such attribute can be found.
     * @see 
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired
     * @return the named attribute's value, or zero if absent.
     */
 	public static int extractIntegerAttribute(NamedNodeMap nnp, String attrName) 
		throws ConfigurationException {
 		return extractIntegerAttribute (nnp, attrName, 0);
 	}
 	
    /**
     * Extracts the integer value of a given attribute from a NodeNameMap. 
     * Returns a default value if no such attribute can be found.
     * @see 
     * @param nnp the NodeNameMap to search for the Attribute
     * @param attrName the name of the attribute who's value is desired.
     * @param defaultValue The value returned if the attribute is absent.
     * @return the named attribute's value or the dedault value.
     */
 	public static int extractIntegerAttribute(NamedNodeMap nnp, String attrName,
 				int defaultValue) 
		throws ConfigurationException {
 	    
        if (nnp == null) return defaultValue;
		Node nameAttr = nnp.getNamedItem(attrName);
		if (nameAttr == null) {
				return defaultValue;
		}
		String value = nameAttr.getNodeValue();
		try {
			return Integer.parseInt (value);
		} catch (Exception e) {
			throw new ConfigurationException ("Invalid integer value: "+value, e);
		}
	}

    /**
     * Extracts the boolean value of a given attribute from a NodeNameMap. 
     * Any string that starts with T or t (for "true") or Y or y (for "yes")
     * is considered to represent true, all other values will produce false.
     * @param nnp The NodeNameMap to search for the Attribute
     * @param attrName The name of the attribute who's value is desired
     * @return The named attribute's value, or false if absent.
     */
 	public static boolean extractBooleanAttribute(NamedNodeMap nnp, String attrName) 
		throws ConfigurationException {
 		return extractBooleanAttribute (nnp, attrName, false);
 	}
 	
    /**
     * Extracts the boolean value of a given attribute from a NodeNameMap. 
     * Returns a default value if no such attribute can be found.
     * Any string that starts with T or t (for "true") or Y or y (for "yes")
     * is considered to represent true, all other values will produce false.
     * @param nnp The NodeNameMap to search for the Attribute
     * @param attrName The name of the attribute who's value is desired
     * @param defaultValue The value returned if the attribute is absent.
     * @return The named attribute's value or the default value.
     */
 	public static boolean extractBooleanAttribute(NamedNodeMap nnp, String attrName,
 				boolean defaultValue) 
		throws ConfigurationException {
 	    
        if (nnp == null) return defaultValue;
		Node nameAttr = nnp.getNamedItem(attrName);
		if (nameAttr == null) {
				return defaultValue;
		}
		String value = nameAttr.getNodeValue();
		return value.matches("^[TtYy].*");
	}

 	public static int[] extractIntegerArrayAttribute (NamedNodeMap nnp,
 	        String attrName) throws ConfigurationException {
 	    
        if (nnp == null) return null;
 	    Node nameAttr = nnp.getNamedItem(attrName);
 	    if (nameAttr == null) return new int[0];
 	    String[] values = nameAttr.getNodeValue().split(",");
 	    int[] result = new int[values.length];
 	    int i = 0;
 	    try {
	 	    for (i=0; i<values.length; i++) {
	 	        result[i] = Integer.parseInt(values[i]);
	 	    }
 	    } catch (NumberFormatException e) {
 	        throw new ConfigurationException ("Invalid integer '"+values[i]
 	                  +"' in attribute '"+attrName+"'");
 	    }
 	    return result;
 	    
 	}
    /**
     * Opens and parses an xml file. Searches the root level of the file for an element
     * with the supplied name.
     * @param fileName the name of the file to open
     * @param elementName the name of the element to find
     * @return the named element in the named file
     * @throws ConfigurationException if there is any problem opening and parsing the file, or
     * if the file does not contain a top level element with the given name.
     */
    public static Element findElementInFile(String filename, List directories, String elementName) throws ConfigurationException
    {
        Document doc = null;
        try {
            // Step 1: create a DocumentBuilderFactory and setNamespaceAware
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            dbf.setNamespaceAware(true);
            // Step 2: create a DocumentBuilder
            DocumentBuilder db = dbf.newDocumentBuilder();
            
            // Step 3: parse the input file to get a Document object
            doc = db.parse(ResourceLoader.getInputStream(filename, directories));
            /*
            File theFile = new File(fileName);
            
            if (theFile.exists()) {
                // Step 3: parse the input file to get a Document object
                doc = db.parse(theFile);
            } else {
                // File not found, then look into the jar file
                File jarFile = new File ("./Rails.jar");
                try {
                    JarFile jf = new JarFile (jarFile);
                    JarInputStream jis = new JarInputStream (new FileInputStream(jarFile));
                    String jeName;
                    for (JarEntry je = jis.getNextJarEntry(); je != null; je = jis.getNextJarEntry()) {
                        if (fileName.equals(je.getName())) {
                            // Step 3: parse the input file to get a Document object
                            doc = db.parse(jis);
                            break;
                        }
                    }
                } catch (IOException e) {
                    throw new ConfigurationException ("Error while opening file " + fileName, e);
                }
               
            }
            */
         } catch (ParserConfigurationException e) {
            throw new ConfigurationException("Could not read/parse " + filename
                    + " to find element " + elementName, e);
        } catch (SAXException e) {
            throw new ConfigurationException("Could not read/parse " + filename
                    + " to find element " + elementName, e);
        } catch (IOException e) {
            throw new ConfigurationException("Could not read/parse " + filename
                    + " to find element " + elementName, e);
        }
        
        if (doc == null) {
            throw new ConfigurationException ("Cannot find file "+ filename);
        }
    
        // Now find the named Element
        NodeList nodeList = doc.getChildNodes();
        for ( int iNode = 0; ( iNode < nodeList.getLength() ) ; iNode++)
        {
            Node childNode = nodeList.item(iNode);
            if (    (childNode != null) 
                 && (childNode.getNodeName().equals(elementName))
                 && (childNode.getNodeType() == Node.ELEMENT_NODE))
            {
                return (Element) childNode;
            }
        }
        throw new ConfigurationException("Could not find " + elementName + " in " + filename);
    }
    
    /**
     * Return all child Elements with a given name of an Element.
     * @param element
     * @param tagName
     * @return
     * @throws ConfigurationException
     */
    public static List<Element> getChildren (Element element, 
            String tagName) throws ConfigurationException {
        
        NodeList children = element.getElementsByTagName(tagName);
        List<Element> list = new ArrayList<Element>();
        Element el;
        
        for (int i = 0; i < children.getLength(); i++)
        {
            el = (Element) children.item(i);
            list.add (el);
        }
        
        return list;
    }

    /**
     * Return the (first) child Element with a given name from an Element.
     * @param element
     * @param tagName
     * @return
     * @throws ConfigurationException
     */
    public static Element getChild (Element element, 
            String tagName) throws ConfigurationException {
        
        NodeList children = element.getElementsByTagName(tagName);
        
        if (children.getLength() > 0) {
            return (Element) children.item(0);
        } else {
            return null;
        }
    }
    
    public static String getText (Element element) 
    throws ConfigurationException {
        
        StringBuffer b = new StringBuffer();
        NodeList children = element.getChildNodes();
        if (children.getLength() == 0) return "";
        Node item;

        for (int i=0; i<children.getLength(); i++) {
            item = children.item(i);
            if (item instanceof Text) b.append(item.getNodeValue());
        }
        
        return b.toString();
    }
    

}
