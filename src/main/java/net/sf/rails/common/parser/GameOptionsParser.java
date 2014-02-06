package net.sf.rails.common.parser;

import java.io.File;
import java.util.List;
import java.util.Map;

import net.sf.rails.common.GameOption;
import net.sf.rails.common.GameOptionsSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.base.Splitter;


public class GameOptionsParser {

    private final static String FILENAME = "GameOptions.xml";
    private final XMLParser parser = new XMLParser();

    public GameOptionsParser() {}

    public GameOptionsSet.Builder processOptions(String directory) throws ConfigurationException {

        GameOptionsSet.Builder options = GameOptionsSet.builder();
        
        Document doc = parser.getDocument(FILENAME, directory);
        Element root = parser.getTopElement(doc);

        List<Element> elements = parser.getElementList(XMLTags.OPTION_TAG, root.getChildNodes());
       
        // use ordering provided in the xml-file
        int ordering = 0;
        for (Element element:elements) {
            Map<String, String> optionMap = parser.getAllAttributes(element);

            GameOption.Builder option;
            if (optionMap.containsKey(XMLTags.NAME_ATTR)) {
                option = GameOption.builder(optionMap.get(XMLTags.NAME_ATTR));
            } else {
                option = null;
            }

            if (option != null) {
                option.setOrdering(ordering++);
                
                if (optionMap.containsKey(XMLTags.TYPE_ATTR)) {
                    option.setType(optionMap.get(XMLTags.TYPE_ATTR));
                }

                if (optionMap.containsKey(XMLTags.DEFAULT_ATTR)) {
                    option.setDefaultValue(optionMap.get(XMLTags.DEFAULT_ATTR));
                }

                if (optionMap.containsKey(XMLTags.PARM_ATTR)) {
                    String parameters = optionMap.get(XMLTags.PARM_ATTR);
                    option.setParameters(Splitter.on(XMLTags.VALUES_DELIM).split(parameters));
                }
                
                if (optionMap.containsKey(XMLTags.VALUES_ATTR)) {
                    String values = optionMap.get(XMLTags.VALUES_ATTR);
                    option.setAllowedValues(Splitter.on(XMLTags.VALUES_DELIM).split(values));
                }
                options.add(option.build());
            }
        }
        return options;
    }

    public static GameOptionsSet.Builder load(String gameName) throws ConfigurationException {
        GameOptionsParser gop = new GameOptionsParser();
        String directory =  GameInfoParser.DIRECTORY + File.separator + gameName;
        return gop.processOptions(directory);
    }

}
