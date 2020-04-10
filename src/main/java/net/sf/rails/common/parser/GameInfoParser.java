package net.sf.rails.common.parser;

import com.google.common.collect.Sets;
import lombok.Getter;
import net.sf.rails.common.GameInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GameInfoParser {

    public final static String DIRECTORY = "data";
    private final static String FILENAME = "GamesList.xml";

    private final XMLParser parser = new XMLParser();

    @Getter
    private String credits;

    public GameInfoParser() {
        // do nothing
    }

    public SortedSet<GameInfo> processGameList() throws ConfigurationException {
        final Document doc = parser.getDocument(FILENAME, DIRECTORY);
        final Element root = parser.getTopElement(doc);

        // <CREDITS>
        final List<Element> creditsElement = parser.getElementList(XMLTags.CREDITS_TAG, root.getChildNodes());

        this.credits = parser.getElementText(creditsElement.get(0).getChildNodes());

        // <GAME>
        final List<Element> gameElements = parser.getElementList(XMLTags.GAME_TAG, root.getChildNodes());

        return IntStream
                .range(0, gameElements.size())
                .mapToObj(index -> {
                    final Element gameElement = gameElements.get(index);

                    return convertElement(index, gameElement);
                })
                .collect(Collectors.toCollection(Sets::newTreeSet));
    }

    private GameInfo convertElement(int ordering, Element gameElement) {
        GameInfo.Builder gameInfo = GameInfo.builder();

        //TODO: push validation into getAttributeAs* methods
        gameInfo.withName(parser.getAttributeAsString(XMLTags.NAME_ATTR, gameElement));

        List<Element> childElements = parser.getElementList(gameElement.getChildNodes());

        // <PLAYER> , <OPTION>, <DESCRIPTION>
        for (Element child : childElements) {
            if (child.getNodeName().equals(XMLTags.DESCR_TAG)) {
                gameInfo.withDescription(parser.getElementText(child.getChildNodes()));
            }

            if (child.getNodeName().equals(XMLTags.NOTE_TAG)) {
                gameInfo.withNote(parser.getElementText(child.getChildNodes()));
            }

            if (child.getNodeName().equals(XMLTags.PLAYERS_TAG)) {
                gameInfo.withMinPlayers(parser.getAttributeAsInteger(XMLTags.MIN_ATTR, child));
                gameInfo.withMaxPlayers(parser.getAttributeAsInteger(XMLTags.MAX_ATTR, child));
            }
        }

        gameInfo.withOrdering(ordering);

        return gameInfo.build();
    }
}
