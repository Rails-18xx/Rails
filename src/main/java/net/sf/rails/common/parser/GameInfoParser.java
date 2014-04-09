package net.sf.rails.common.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;

import net.sf.rails.common.GameInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.google.common.collect.Sets;


public class GameInfoParser {
    
    public final static String DIRECTORY = "data";
    private final static String FILENAME = "GamesList.xml";
    
	private final XMLParser parser = new XMLParser();
    private String credits;

	public GameInfoParser() {}

	public String getCredits() {
		return credits;
	}

	public SortedSet<GameInfo> processGameList() throws ConfigurationException {

		SortedSet<GameInfo> gameList = Sets.newTreeSet();

		Document doc = parser.getDocument(FILENAME, DIRECTORY);
		Element root = parser.getTopElement(doc);

		// <CREDITS>
		List<Element> creditsElement = parser.getElementList(XMLTags.CREDITS_TAG, root.getChildNodes());
		this.credits = parser.getElementText(creditsElement.get(0).getChildNodes());
		
		ArrayList<Element> gameElements = parser.getElementList(XMLTags.GAME_TAG, root
				.getChildNodes());

		// <GAME>
		Iterator<Element> it = gameElements.iterator();
		int count = 0;
		while (it.hasNext()) {
			Element el = it.next();
			
			GameInfo.Builder gameInfo = GameInfo.builder();
			
//			ArrayList<GameOption> optionsList = new ArrayList<GameOption>();

			//TODO: push validation into getAttributeAs* methods
			gameInfo.setName(parser.getAttributeAsString(XMLTags.NAME_ATTR, el));

			ArrayList<Element> childElements = parser.getElementList(el.getChildNodes());
			
			// <PLAYER> , <OPTION>, <DESCRIPTION>
			Iterator<Element> childIt = childElements.iterator();
			while (childIt.hasNext()) {
				Element child = childIt.next();
				
				if (child.getNodeName().equals(XMLTags.DESCR_TAG)) {
					gameInfo.setDescription(parser.getElementText(child.getChildNodes()));
				}
				
				if (child.getNodeName().equals(XMLTags.NOTE_TAG)) {
				    gameInfo.setNote(parser.getElementText(child.getChildNodes()));
				}

				if (child.getNodeName().equals(XMLTags.PLAYERS_TAG)) {
					gameInfo.setMinPlayers(parser.getAttributeAsInteger(XMLTags.MIN_ATTR, child));
					gameInfo.setMaxPlayers(parser.getAttributeAsInteger(XMLTags.MAX_ATTR, child));
				}

			}
			gameList.add(gameInfo.build(count++));
		}
		return gameList;
	}
}
