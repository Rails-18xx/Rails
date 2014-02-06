package net.sf.rails.common.parser;

import java.io.*;
import java.util.*;

import javax.xml.parsers.*;

import net.sf.rails.common.ResourceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.*;
import org.xml.sax.SAXException;


/**
 * XMLParser is our catch-all class for parsing an XML Document.
 */
public class XMLParser {
    protected final static Logger log = LoggerFactory.getLogger(XMLParser.class);

    protected final HashMap<String, Document> documentCache = new HashMap<String, Document>();

	public XMLParser() {}

	/**
	 * Opens and parses an xml file.
	 * 
	 * @param fileName
	 *            the name of the file to open
	 * @return Document
	 * @throws ConfigurationException
	 *             if there is any problem opening and parsing the file
	 */
	protected Document getDocument(String filename, String directory)
			throws ConfigurationException {

		if (documentCache.containsKey(filename)) {
			return documentCache.get(filename);
		} else {
			Document doc = null;

			try {
				DocumentBuilderFactory dbf = DocumentBuilderFactory
						.newInstance();
				dbf.setNamespaceAware(true);
				doc = dbf.newDocumentBuilder().parse(
						ResourceLoader.getInputStream(filename, directory));
			} catch (ParserConfigurationException e) {
				throw new ConfigurationException("Could not read/parse "
						+ filename, e);
			} catch (SAXException e) {
				throw new ConfigurationException("Could not read/parse "
						+ filename, e);
			} catch (IOException e) {
				throw new ConfigurationException("Could not read/parse "
						+ filename, e);
			} catch (Exception e) {
				throw new ConfigurationException(
						"Cannot find file " + filename, e);
			}
			return doc;
		}

	}

	/**
	 * Returns the first (and hopefully root) element within a document.
	 * 
	 * @param doc
	 * @return First (root) element in the document.
	 */
	protected Element getTopElement(Document doc) {

		NodeList nodeList = doc.getChildNodes();

		for (int iNode = 0; (iNode < nodeList.getLength()); iNode++) {
			Node childNode = nodeList.item(iNode);
			if ((childNode != null)
					&& (childNode.getNodeType() == Node.ELEMENT_NODE)) {
				return (Element) childNode;
			}
		}

		return null;
	}

	protected ArrayList<Element> getElementList(String tagName, Document doc) {
		return getElementList(tagName, doc.getChildNodes());
	}

	/**
	 * Recursive method to retrieve all elements and sub-elements containing a
	 * specific tag.
	 * 
	 * @param tagName
	 * @param nodeList
	 * @return All elements with tagName
	 */
	protected ArrayList<Element> getElementList(String tagName, NodeList nodeList) {

		ArrayList<Element> elements = new ArrayList<Element>();

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);

			if ((childNode != null)
					&& (childNode.getNodeName().equals(tagName))
					&& (childNode.getNodeType() == Node.ELEMENT_NODE)) {
				elements.add((Element) childNode);
			} else if ((childNode != null)
					&& (childNode.getNodeType() == Node.ELEMENT_NODE)) {
				// Recurse through the document, searching for our tag.
				elements.addAll(getElementList(tagName, childNode
						.getChildNodes()));
			} 
		}

		return elements;
	}

	/**
	 * Recursive method to retrieve all elements and sub-elements in the
	 * NodeList.
	 * 
	 * @param nodeList
	 * @return All elements in the NodeList
	 */
	protected ArrayList<Element> getElementList(NodeList nodeList) {
		ArrayList<Element> elements = new ArrayList<Element>();

		for (int i = 0; i < nodeList.getLength(); i++) {
			Node childNode = nodeList.item(i);

			if ((childNode != null)
					&& (childNode.getNodeType() == Node.ELEMENT_NODE)) {
				elements.add((Element) childNode);
				// Recurse through the document
				elements.addAll(getElementList(childNode.getChildNodes()));
			}
		}

		return elements;
	}

	/**
	 * Retrieves the given attribute from the given element as a String.
	 * 
	 * @param String
	 *            attributeName
	 * @param Element
	 *            el
	 * @return String, or empty string if none found.
	 */
	protected String getAttributeAsString(String attributeName, Element el) {
		NamedNodeMap nnp = el.getAttributes();

		for (int i = 0; i < nnp.getLength(); i++) {
			if (nnp.item(i).getNodeName() == attributeName) {
				return nnp.item(i).getNodeValue();
			}
		}
		return "";
	}

	/**
	 * Retrieves the given attribute from the given element as an Integer.
	 * 
	 * @param String
	 *            attributeName
	 * @param Element
	 *            el
	 * @return Integer, or -1 if none found.
	 */
	protected Integer getAttributeAsInteger(String attributeName, Element el) {
		NamedNodeMap nnp = el.getAttributes();

		for (int i = 0; i < nnp.getLength(); i++) {
			if (nnp.item(i).getNodeName() == attributeName) {
				return Integer.parseInt(nnp.item(i).getNodeValue());
			}
		}
		return -1;
	}
	
	/**
	 * Retrieves all attributes for an Element
	 * 
	 * @param Element
	 *            el
	 * @return HashMap<String,String> of all Attributes
	 */
	protected HashMap<String, String> getAllAttributes(Element el) {
		HashMap<String, String> attributes = new HashMap<String, String>();
		NamedNodeMap nnp = el.getAttributes();

		for (int i = 0; i < nnp.getLength(); i++) {
			attributes.put(nnp.item(i).getNodeName(), nnp.item(i)
					.getNodeValue());
		}

		return attributes;
	}

	protected String getElementText(NodeList nodeList) {
		
		for (int i=0; i < nodeList.getLength(); i++) {
            if (nodeList.item(i).getNodeType() == Node.TEXT_NODE) {
            	return (String) nodeList.item(i).getNodeValue();
            }
		}
		return null;
	}
}
