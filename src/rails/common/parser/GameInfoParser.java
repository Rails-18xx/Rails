package rails.common.parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.w3c.dom.Document;
import org.w3c.dom.Element;


public class GameInfoParser extends XMLParser {
    
	private String credits = "";
	
	private final static String DIRECTORY = "data";
	private final static String FILENAME = "GamesList.xml";

	public GameInfoParser() {}

	public String getCredits() {
		return credits;
	}

	public ArrayList<GameInfo> processGameList() throws ConfigurationException {

		ArrayList<GameInfo> gameList = new ArrayList<GameInfo>();

		Document doc = getDocument(FILENAME, DIRECTORY);
		Element root = getTopElement(doc);

		// <CREDITS>
		ArrayList<Element> creditsElement = getElementList(XMLTags.CREDITS_TAG, root.getChildNodes());
		this.credits = getElementText(creditsElement.get(0).getChildNodes());
		
		ArrayList<Element> gameElements = getElementList(XMLTags.GAME_TAG, root
				.getChildNodes());

		// <GAME>
		Iterator<Element> it = gameElements.iterator();
		while (it.hasNext()) {
			Element el = it.next();
			
			GameInfo gameInfo = new GameInfo();
			
			ArrayList<GameOption> optionsList = new ArrayList<GameOption>();

			//TODO: push validation into getAttributeAs* methods
			gameInfo.setName(getAttributeAsString(XMLTags.NAME_ATTR, el));
			
			if (getAttributeAsString(XMLTags.COMPLETE_ATTR, el).equals(
					"yes")) {
				gameInfo.setComplete(true);
			}

			ArrayList<Element> childElements = getElementList(el.getChildNodes());
			
			// <PLAYER> , <OPTION>, <DESCRIPTION>
			Iterator<Element> childIt = childElements.iterator();
			while (childIt.hasNext()) {
				Element child = childIt.next();
				
				if (child.getNodeName().equals(XMLTags.DESCR_TAG)) {
					gameInfo.setDescription(getElementText(child.getChildNodes()));
				}
				
				if (child.getNodeName().equals(XMLTags.NOTE_TAG)) {
				    gameInfo.setNote(getElementText(child.getChildNodes()));
				}

				if (child.getNodeName().equals(XMLTags.PLAYERS_TAG)) {
					gameInfo.setMinPlayers(getAttributeAsInteger(XMLTags.MIN_ATTR, child));
					gameInfo.setMaxPlayers(getAttributeAsInteger(XMLTags.MAX_ATTR, child));
				}

				if (child.getNodeName().equals(XMLTags.OPTION_TAG)) {
					HashMap<String, String> optionMap = getAllAttributes(child);

					GameOption options = null;

					if (optionMap.containsKey(XMLTags.NAME_ATTR)) {
						options = new GameOption(optionMap.get(XMLTags.NAME_ATTR));
					}

					if (options != null) {
						if (optionMap.containsKey(XMLTags.TYPE_ATTR)) {
							options.setType(optionMap.get(XMLTags.TYPE_ATTR));
						}

						if (optionMap.containsKey(XMLTags.DEFAULT_ATTR)) {
							options.setDefaultValue(optionMap.get(XMLTags.DEFAULT_ATTR));
						}

					    if (optionMap.containsKey(XMLTags.PARM_ATTR)) {
                            options.setParameters(optionMap.get(XMLTags.PARM_ATTR).split(XMLTags.VALUES_DELIM));
                        }
						if (optionMap.containsKey(XMLTags.VALUES_ATTR)) {
							String values = optionMap.get(XMLTags.VALUES_ATTR);
							String[] valArr = values.split(XMLTags.VALUES_DELIM);
							options.setAllowedValues(valArr);
						}
						optionsList.add(options);
					}
					gameInfo.setOptions(optionsList);
				}
			}
			gameList.add(gameInfo);
		}
		return gameList;
	}
}
