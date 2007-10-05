/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/util/Tag.java,v 1.2 2007/10/05 22:02:26 evos Exp $*/
package rails.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


import rails.game.ConfigurationException;
import rails.game.Game;

/** 
 * Each object of this class both contains and represents 
 * a DOM Element object. Its purpose it to hide the XML
 * parsing details from the application program. 
 * The methods of this class intend to replace the corresponding
 * methods in XmlUtils.
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
    
    public Tag (Element element) {
        this.element = element;
    }

    /**
     * Return all child Elements with a given name of an Element.
     * @param element
     * @param tagName
     * @return
     * @throws ConfigurationException
     */
    public List<Tag> getChildren (String tagName) 
    throws ConfigurationException {
        
        if (!parsed) parse (element);
        
        return children.get(tagName);
    }

    /**
     * Return the (first) child Element with a given name from an Element.
     * @param element
     * @param tagName
     * @return
     * @throws ConfigurationException
     */
    public Tag getChild (String tagName) 
    throws ConfigurationException {
        
        if (!parsed) parse (element);
        
        List<Tag> list = children.get(tagName);
        if (list != null && list.size() > 0) {
            return list.get(0);
        } else {
            return null;
        }
    }
    
    public String getText () 
    throws ConfigurationException {
        
        if (!parsed) parse (element);
       
       return text;
    }
    
   public Map<String, String> getAttributes () 
    throws ConfigurationException {
        
       if (!parsed) parse (element);
        
        return attributes;
        
    }
    
    public String getAttributeAsString (String name, String defaultValue) 
    throws ConfigurationException {
    	
        if (!parsed) parse (element);
        
        String value = attributes.get(name);
        if (value == null) return defaultValue;
        return value;
    }
    
    public String getAttributeAsString (String name) 
    throws ConfigurationException {
    	
        return getAttributeAsString (name, null);
    }
    
    public int getAttributeAsInteger (String name, int defaultValue) 
    throws ConfigurationException {
    	
        if (!parsed) parse (element);
        
		String value = attributes.get(name);
		if (value == null) return defaultValue;
		try {
			return Integer.parseInt (value);
		} catch (Exception e) {
			throw new ConfigurationException ("Invalid integer value: "+value, e);
		}
    }
    
    public int getAttributeAsInteger (String name) 
    throws ConfigurationException {
    	
    	return getAttributeAsInteger (name, 0);
    }
    
    public boolean getAttributeAsBoolean (String name, boolean defaultValue) 
    throws ConfigurationException {
    	
        if (!parsed) parse (element);
        
		String value = attributes.get(name);
		if (value == null) return defaultValue;
		
		return value.matches("^[TtYy].*");
    }
    
    public boolean getAttributeAsBoolean (String name) 
    throws ConfigurationException {
    	
    	return getAttributeAsBoolean (name, false);
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
     private synchronized void parse(Element element) 
     throws ConfigurationException {
        
        if (parsed || parsing) return;
        parsing = true;
        
        attributes = new HashMap<String, String>();
        children = new HashMap<String, List<Tag>>();
        
        NamedNodeMap nnp = element.getAttributes();
        Node attribute;
        String name, value;
        for (int i=0; i<nnp.getLength(); i++) {
            attribute = nnp.item(i);
            name = attribute.getNodeName();
            value = attribute.getNodeValue();
            attributes.put(name, value);
        }
        
        parseSubTags (element);
        
     }
     
     private void parseSubTags (Element element) 
     throws ConfigurationException {
        
        NodeList childNodes = element.getChildNodes();
        Node childNode;
        Element childElement;
        String childTagName;
        Node attribute;
        String name, value;
        StringBuffer textBuffer = new StringBuffer();
         
        for (int i = 0; i < childNodes.getLength(); i++)
        {
            childNode = childNodes.item(i);
            if (childNode.getNodeType() == Node.ELEMENT_NODE) { 
                childElement = (Element) childNode;
                childTagName = childElement.getNodeName();
                NamedNodeMap nnp = childElement.getAttributes();
                if (childTagName.equalsIgnoreCase("Attributes")) {
                    for (int j=0; j<nnp.getLength(); j++) {
                        attribute = nnp.item(j);
                        name = attribute.getNodeName();
                        value = attribute.getNodeValue();
                        attributes.put(name, value);
                    }
                } else if (childTagName.equalsIgnoreCase("IfOption")) {
                    name = XmlUtils.extractStringAttribute (nnp, "name");
                    value = XmlUtils.extractStringAttribute (nnp, "value");
                    if (name == null) {
                        throw new ConfigurationException ("IfOption has no optionName attribute");
                    }
                    if (value == null) {
                        throw new ConfigurationException ("IfOption has no optionValue attribute");
                    }
                    // Check if the option has been chosen; if not, skip the rest
                    String optionValue = Game.getGameOption(name);
                    if (optionValue == null) {
                        throw new ConfigurationException ("GameOption "+name+"="+value+" but no assigned value found");
                    }
                    if (optionValue.equalsIgnoreCase(value)) {
                        parse (childElement);
                    }
                } else {
                    if (!children.containsKey(childTagName)) {
                        children.put(childTagName, new ArrayList<Tag>());
                    }
                    children.get(childTagName).add(new Tag(childElement));
                }
            } else if (childNode.getNodeType() == Node.TEXT_NODE) {
                textBuffer.append(childNode.getNodeValue());
            }
        }
        
        text = textBuffer.toString();
        parsed = true;
        parsing = false;
    }
    
     
}
