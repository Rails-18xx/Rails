using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Xml;

/**
 * Each object of this class both contains and represents a DOM Element object.
 * Its purpose it to hide the XML parsing details from the application program.
 * The methods of this class intend to replace the corresponding methods in
 * XmlUtils.
 *
 * @author Erik Vos
 *
 */

// UWP: Wrap XmlElement 
namespace GameLib.Net.Common.Parser
{
    public class Tag
    {
        private static Logger<Tag> log = new Logger<Tag>();

        // static data
        private XmlElement element;
        private GameOptionsSet gameOptions;

        // dynamic data
        private Dictionary<string, string> attributes = null;
        private Dictionary<string, List<Tag>> children = null;
        private string text = null;
        private bool parsed = false;
        private bool parsing = false;


        public Tag(XmlElement element, GameOptionsSet gameOptions)
        {
            this.element = element;
            this.gameOptions = gameOptions;
        }

        public Dictionary<string, List<Tag>> GetChildren()
        {

            if (!parsed) Parse(element);

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
        public List<Tag> GetChildren(string tagName)
        {

            if (!parsed) Parse(element);

            if (!children.ContainsKey(tagName)) return null;

            return children[tagName];
        }

        /**
         * Return the (first) child Element with a given name from an Element.
         *
         * @param element
         * @param tagName
         * @return
         * @throws ConfigurationException
         */
        public Tag GetChild(string tagName)
        {

            if (!parsed) Parse(element);
            if (!children.ContainsKey(tagName)) return null;
            List<Tag> list = children[tagName];
            if (list != null && list.Count > 0)
            {
                return list[0];
            }
            else
            {
                return null;
            }
        }

        public bool HasChild(string tagName)
        {

            //br: this was hardcoded  to "AllowsMultipleBasesOfOneCompany" -- looks like a bug.
            return GetChildren(tagName) != null;
        }

        public string GetText()
        {
            if (!parsed) Parse(element);

            return text;
        }

        public Dictionary<string, string> GetAttributes()
        {
            if (!parsed) Parse(element);

            return attributes;
        }

        public string GetAttributeAsString(string name, string defaultValue)
        {
            if (!parsed) Parse(element);

            if (!attributes.ContainsKey(name)) return defaultValue;

            string value = attributes[name];
            if (value == null) return defaultValue;
            return value;
        }

        public string GetAttributeAsString(string name)
        {
            return GetAttributeAsString(name, null);
        }

        public int GetAttributeAsInteger(string name, int defaultValue)
        {
            if (!parsed) Parse(element);

            if (!attributes.ContainsKey(name)) return defaultValue;

            string value = attributes[name];
            if (value == null) return defaultValue;
            try
            {
                // Unlike Java, we want to allow '+' signs
                if (value[0] == '+') value = value.Substring(1);
                return int.Parse(value); // Integer.parseInt(value);
            }
            catch (Exception e)
            {
                throw new ConfigurationException("Invalid integer value: " + value,
                        e);
            }
        }

        public float GetAttributeAsFloat(string name)
        {
            return GetAttributeAsFloat(name, 0.0f);
        }

        public float GetAttributeAsFloat(string name, float defaultValue)
        {
            if (!parsed) Parse(element);

            if (!attributes.ContainsKey(name)) return defaultValue;

            string value = attributes[name];
            if (value == null) return defaultValue;
            try
            {
                return float.Parse(value); //Float.parseFloat(value);
            }
            catch (Exception e)
            {
                throw new ConfigurationException("Invalid floating point value: " + value,
                        e);
            }
        }

        public int GetAttributeAsInteger(string name)
        {
            return GetAttributeAsInteger(name, 0);
        }

        public ReadOnlyCollection<int> GetAttributeAsIntegerList(string name)
        {
            string valueString = GetAttributeAsString(name);
            if (string.IsNullOrEmpty(valueString)) return new ReadOnlyCollection<int>(new List<int>());

            var result = new List<int>();
            //ImmutableList.Builder<Integer> result = ImmutableList.builder();
            try
            {
                foreach (string value in valueString.Split(','))
                {
                    result.Add(int.Parse(value));
                }
            }
            catch (FormatException)
            {
                throw new ConfigurationException("Invalid integer in attribute " + name + "'");
            }
            return new ReadOnlyCollection<int>(result);
        }

        public bool GetAttributeAsBoolean(string name, bool defaultValue)
        {

            if (!parsed) Parse(element);

            if (!attributes.ContainsKey(name)) return defaultValue;

            string value = attributes[name];
            if (value == null) return defaultValue;

            return 1 == value.IndexOfAny(new char[] { 'T', 't', 'Y', 'y' }); //value.matches("^[TtYy].*");
        }

        public bool GetAttributeAsBoolean(string name)
        {
            return GetAttributeAsBoolean(name, false);
        }

        // br: needed to test if a Tradeable tag has a toCompany or toPlayer attribute
        public bool HasAttribute(string name)
        {
            return GetAttributeAsString(name) != null;
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
        private void Parse(XmlElement element)
        {

            if (parsed || parsing) return;
            parsing = true;

            attributes = new Dictionary<string, string>();
            children = new Dictionary<string, List<Tag>>();

            //XmlNamedNodeMap nnp = element.Attributes;
            var nnp = element.Attributes;
            XmlAttribute attribute;
            string name, value;
            foreach (XmlAttribute attr in nnp)
            {
                attribute = attr;
                name = attribute.Name;  //.getNodeName();
                value = attribute.Value; //getNodeValue();
                attributes[name] = value;
            }

            ParseSubTags(element);

        }

        private void ParseSubTags(XmlElement element)
        {

            XmlNodeList childNodes = element.ChildNodes;
            XmlNode childNode;
            XmlElement childElement;
            string childTagName;
            XmlAttribute attribute;
            string name, value;
            List<string> valueList;
            StringBuilder textBuffer = new StringBuilder();

            for (int i = 0; i < childNodes.Count; i++)
            {
                childNode = childNodes[i];
                if (childNode.NodeType == XmlNodeType.Element)
                {
                    childElement = (XmlElement)childNode;
                    childTagName = childElement.Name;
                    XmlAttributeCollection nnp = childElement.Attributes;
                    if (childTagName.Equals("Attributes", StringComparison.OrdinalIgnoreCase))
                    {
                        for (int j = 0; j < nnp.Count; j++)
                        {
                            attribute = nnp[j];
                            name = attribute.Name;
                            value = attribute.Value;
                            attributes[name] = value;
                        }
                    }
                    else if (childTagName.Equals("IfOption", StringComparison.OrdinalIgnoreCase))
                    {
                        XmlNode nameAttr = nnp.GetNamedItem("name");
                        if (nameAttr == null)
                            throw new ConfigurationException("IfOption has no optionName attribute");

                        name = nameAttr.Value;

                        XmlNode parmAttr = nnp.GetNamedItem("parm");
                        if (parmAttr != null)
                        {
                            value = parmAttr.Value;
                            string[] parameters = value.Split(XmlTags.VALUES_DELIM);// Splitter.on(XMLTags.VALUES_DELIM).split(value);
                            name = GameOption.ConstructParameterizedName(name, new ReadOnlyCollection<string>(parameters));
                        }

                        XmlNode valueAttr = nnp.GetNamedItem("value");
                        if (valueAttr == null)
                            throw new ConfigurationException("IfOption has no optionValue attribute");

                        value = valueAttr.Value;
                        valueList = value.Split(',').ToList();

                        // Check if the option has been chosen; if not, skip the
                        // rest
                        if (gameOptions == null)
                        {
                            throw new ConfigurationException("No GameOptions available in tag " + element.Name);
                        }

                        string optionValue = gameOptions.Get(name);

                        // For backwards compatibility: search for an extended name
                        /* This applies to parametrized options, such as "UnlimitedTopTrains".
                         * It parametrized with a parameter "D" to allow display as "Unlimited D-trains"
                         * and still remaining generic.
                         * Parametrization means that the actual name is UnlimitedTopTrains_D,
                         * for instance in saved files, and so the name must be shortened to find a match.
                         */

                        // FIXME: Rails 2.0 removed that handling, only logging errors now
                        if (optionValue == null)
                        {
                            log.Error("GameOption " + name + "=" + value + " has no assigned value");
                        }

                        //                    if (optionValue == null) {
                        //                    	for (String optName : gameOptions.getOptions().keySet()) {
                        //                    	    // startsWith is a shortcut, perhaps it should be matches(name+"_.*").
                        //                    		if (optName != null && optName.startsWith(name)) {
                        //                    			optionValue = gameOptions.get(optName);
                        //                    			log.warn("Option name "+name+" replaced by "+optName);
                        //                    			break;
                        //                    		}
                        //                    	}
                        //                    }
                        //                    
                        //                    // If not assigned in the previous step, take the default value
                        //                    if (optionValue == null) {
                        //                        GameOption go = GameOption.getByName(name);
                        //                        optionValue = go != null ? go.getDefaultValue() : "";
                        //                        log.warn("GameOption " + name + "=" + value
                        //                                 + " but no assigned value found, assumed "+optionValue);
                        //
                        //                    }

                        if (valueList.Contains(optionValue))
                        {
                            ParseSubTags(childElement);
                        }
                    }
                    else
                    {
                        if (!children.ContainsKey(childTagName))
                        {
                            children[childTagName] = new List<Tag>();
                        }
                        children[childTagName].Add(new Tag(childElement, gameOptions));
                    }
                }
                else if (childNode.NodeType == XmlNodeType.Text)
                {
                    textBuffer.Append(childNode.Value);
                }
            }

            text = textBuffer.ToString();
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
        //public static Tag FindTopTagInFile(string filename, string directory, string tagName, GameOptionsSet gameOptions)
        // Example:
        //try
        //{
        //    var file = await Windows.ApplicationModel.Package.Current.InstalledLocation.GetFileAsync("test.xml");
        //    var content = await Windows.Storage.FileIO.ReadTextAsync(file);
        //    var xmlDocumentToTransform = new XmlDocument();
        //    xmlDocumentToTransform.LoadXml(content);
        //}
        //catch (Exception ex)
        //{
        //    var dlg = new MessageDialog(ex.ToString());
        //    await dlg.ShowAsync();
        //}
        public static Tag FindTopTagInFile(string xmlData, string tagName, GameOptionsSet gameOptions)
        {
            XmlDocument doc = new XmlDocument();
            try
            {
                //// Step 1: create a DocumentBuilderFactory and setNamespaceAware
                //DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
                //dbf.setNamespaceAware(true);
                //// Step 2: create a DocumentBuilder
                //DocumentBuilder db = dbf.newDocumentBuilder();

                //// Step 3: parse the input file to get a Document object
                //doc =
                //        db.parse(ResourceLoader.getInputStream(filename,
                //                directory));
                doc.LoadXml(xmlData);
            }
            catch (System.Xml.XmlException e)
            {
                throw new ConfigurationException("Could not read/parse " //+ filename
                                                 + " to find element " + tagName, e);
            }
            //} catch (SAXException e) {
            //    throw new ConfigurationException("Could not read/parse " + filename
            //                                     + " to find element " + tagName, e);
            //} catch (IOException e) {
            //    throw new ConfigurationException("Could not read/parse " + filename
            //                                     + " to find element " + tagName, e);
            //}

            //if (doc == null)
            //{
            //    throw new ConfigurationException("Cannot find file " + filename);
            //}

            // Now find the named Element
            XmlNodeList nodeList = doc.ChildNodes;
            for (int iNode = 0; (iNode < nodeList.Count); iNode++)
            {
                XmlNode childNode = nodeList[iNode];
                if ((childNode != null)
                    && (childNode.Name.Equals(tagName))
                    && (childNode.NodeType == XmlNodeType.Element))
                {
                    return new Tag((XmlElement)childNode, gameOptions);
                }
            }
            throw new ConfigurationException("Could not find " + tagName + " in "
                                             /*+ filename*/);
        }

        public XmlElement GetElement()
        {
            return element;
        }

        override public string ToString()
        {
            //return Objects.toStringHelper(this)
            //        .add("attributes", attributes)
            //        .add("children", children)
            //        .toString();

            return $"{this.GetType().Name}{{attributes={attributes.ToString()}}}{{children={children.ToString()}}}";
        }

    }
}
