package rails.game.specific._18AL;

import java.util.ArrayList;
import java.util.List;

import rails.game.Bank;
import rails.game.ConfigurationException;
import rails.game.GameManagerI;
import rails.game.move.Moveable;
import rails.game.special.SpecialProperty;
import rails.util.LocalText;
import rails.util.Tag;

public class NameTrains extends SpecialProperty implements Moveable {

    private String tokenClassName;
    private Class<?> tokenClass;
    private List<NamedTrainToken> tokens = new ArrayList<NamedTrainToken>(2);
    private String name = "NameTrains";

    @Override
    public void configureFromXML(Tag tag) throws ConfigurationException {

        super.configureFromXML(tag);

        Tag assignTag = tag.getChild("NameTrains");
        if (assignTag == null) {
            throw new ConfigurationException("<NamedTrains> tag missing");
        }

        tokenClassName = assignTag.getAttributeAsString("class");
        if (tokenClassName == null) {
            throw new ConfigurationException(
                    "No named train token class name provided");
        }

        try {
            tokenClass = Class.forName(tokenClassName);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException("Unknown class " + tokenClassName,
                    e);
        }

        String tokenTagName = tokenClassName.replaceAll(".*\\.", "");
        List<Tag> tokenTags = assignTag.getChildren(tokenTagName);
        if (tokenTags == null || tokenTags.isEmpty()) {
            throw new ConfigurationException(
                    "No <" + tokenTagName
                            + "> tags found in <AssignNamedTrain>");
        }

        description = name + ": ";

        for (Tag tokenTag : tokenTags) {
            try {
                NamedTrainToken token =
                        (NamedTrainToken) tokenClass.newInstance();
                tokens.add(token);
                token.configureFromXML(tokenTag);
                description += token.getLongName() + ": " + Bank.format(token.getValue()) + ", ";
            } catch (Exception e) {
                throw new ConfigurationException("Cannot instantiate class "
                                                 + tokenClassName, e);
            }
        }
        description = description.replaceFirst(", $", "");
    }

    @Override
    public void finishConfiguration (GameManagerI gameManager)
    throws ConfigurationException {

        for (NamedTrainToken token : tokens) {
            token.finishConfiguration(gameManager);
        }
    }

    public List<NamedTrainToken> getTokens() {
        return tokens;
    }

    public boolean isExecutionable() {
        return true;
    }

    /**
     * Tokens can be reassigned indefinitely, so the ability is never set to
     * exercised.
     */
    @Override
    public void setExercised() {}

    public String getName() {
        return name;
    }

    @Override
    public String toMenu() {
        return description;
    }

    @Override
    public String toString() {
        return description;
    }
    
    @Override
    public String getInfo() {
        String infoText = LocalText.getText("SpecialNameTrains",
                tokens.size());
        infoText += "<br>" + description;
        infoText.replaceFirst("NameTrains: ", "");
        return infoText;
    }
}
