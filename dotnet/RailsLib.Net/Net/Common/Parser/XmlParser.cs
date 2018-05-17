using System;
using System.Collections.Generic;
using System.Text;
using System.Xml;

/**
 * XMLParser is our catch-all class for parsing an XML Document.
 */

namespace GameLib.Net.Common.Parser
{
    public class XmlParser
    {
        protected static Logger<XmlParser> log = new Logger<XmlParser>();

        protected Dictionary<string, XmlDocument> documentCache = new Dictionary<string, XmlDocument>();

        public XmlParser() { }

        /**
         * Opens and parses an xml file.
         * 
         * @param fileName
         *            the name of the file to open
         * @return Document
         * @throws ConfigurationException
         *             if there is any problem opening and parsing the file
         */
        public XmlDocument GetDocument(string filename, string directory)
        {

            if (documentCache.ContainsKey(filename))
            {
                return documentCache[filename];
            }
            else
            {
                XmlDocument doc = null;

                try
                {
                    doc = new XmlDocument();
                    doc.PreserveWhitespace = true;
                    //doc.SetNamespaceAware(true);
                    //doc = dbf.newDocumentBuilder().parse(
                    //                    ResourceLoader.getInputStream(filename, directory));
                    doc.Load(directory + "\\" + filename);
                }
                catch (XmlException e)
                {
                    throw new ConfigurationException("Could not read/parse "
                            + filename + " " + e.Message);
                    //} catch (SAXException e) {
                    //	throw new ConfigurationException("Could not read/parse "
                    //			+ filename, e);
                    //} catch (IOException e) {
                    //	throw new ConfigurationException("Could not read/parse "
                    //			+ filename, e);
                }
                catch (Exception e)
                {
                    throw new ConfigurationException(
                            "Cannot find file " + filename + " " + e.Message);
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
        public XmlElement GetTopElement(XmlDocument doc)
        {

            XmlNodeList nodeList = doc.ChildNodes;

            for (int iNode = 0; (iNode < nodeList.Count); iNode++)
            {
                XmlNode childNode = nodeList[iNode];
                if ((childNode != null)
                        && (childNode.NodeType == XmlNodeType.Element))
                {
                    return (XmlElement)childNode;
                }
            }

            return null;
        }

        public List<XmlElement> GetElementList(string tagName, XmlDocument doc)
        {
            return GetElementList(tagName, doc.ChildNodes);
        }

        /**
         * Recursive method to retrieve all elements and sub-elements containing a
         * specific tag.
         * 
         * @param tagName
         * @param nodeList
         * @return All elements with tagName
         */
        public List<XmlElement> GetElementList(string tagName, XmlNodeList nodeList)
        {

            List<XmlElement> elements = new List<XmlElement>();

            for (int i = 0; i < nodeList.Count; i++)
            {
                XmlNode childNode = nodeList[i];

                if ((childNode != null)
                        && (childNode.Name.Equals(tagName))
                        && (childNode.NodeType == XmlNodeType.Element))
                {
                    elements.Add((XmlElement)childNode);
                }
                else if ((childNode != null)
                      && (childNode.NodeType == XmlNodeType.Element))
                {
                    // Recurse through the document, searching for our tag.
                    elements.AddRange(GetElementList(tagName, childNode.ChildNodes));
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
        public List<XmlElement> GetElementList(XmlNodeList nodeList)
        {
            List<XmlElement> elements = new List<XmlElement>();

            for (int i = 0; i < nodeList.Count; i++)
            {
                XmlNode childNode = nodeList[i];

                if ((childNode != null)
                        && (childNode.NodeType == XmlNodeType.Element))
                {
                    elements.Add((XmlElement)childNode);
                    // Recurse through the document
                    elements.AddRange(GetElementList(childNode.ChildNodes));
                }
            }

            return elements;
        }

        /**
         * Retrieves the given attribute from the given element as a string.
         * 
         * @param string
         *            attributeName
         * @param Element
         *            el
         * @return string, or empty string if none found.
         */
        public string GetAttributeAsString(string attributeName, XmlElement el)
        {
            XmlNamedNodeMap nnp = el.Attributes;

            for (int i = 0; i < nnp.Count; i++)
            {
                if (nnp.Item(i).Name == attributeName)
                {
                    return nnp.Item(i).Value;
                }
            }
            return "";
        }

        /**
         * Retrieves the given attribute from the given element as an Integer.
         * 
         * @param string
         *            attributeName
         * @param Element
         *            el
         * @return Integer, or -1 if none found.
         */
        public int GetAttributeAsInteger(string attributeName, XmlElement el)
        {
            XmlNamedNodeMap nnp = el.Attributes;

            for (int i = 0; i < nnp.Count; i++)
            {
                if (nnp.Item(i).Name == attributeName)
                {
                    return int.Parse(nnp.Item(i).Value);
                }
            }
            return -1;
        }

        /**
         * Retrieves all attributes for an Element
         * 
         * @param Element
         *            el
         * @return HashMap<string,string> of all Attributes
         */
        public Dictionary<string, string> GetAllAttributes(XmlElement el)
        {
            Dictionary<string, string> attributes = new Dictionary<string, string>();
            XmlNamedNodeMap nnp = el.Attributes;

            for (int i = 0; i < nnp.Count; i++)
            {
                attributes[nnp.Item(i).Name] = nnp.Item(i).Value;
            }

            return attributes;
        }

        public string GetElementText(XmlNodeList nodeList)
        {

            for (int i = 0; i < nodeList.Count; i++)
            {
                if (nodeList.Item(i).NodeType == XmlNodeType.Text)
                {
                    return (string)nodeList.Item(i).Value;
                }
            }
            return null;
        }
    }
}
