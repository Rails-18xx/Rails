package net.sf.rails.game.specific._18AL;

import java.util.ArrayList;
import java.util.List;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Configure;
import net.sf.rails.common.parser.Tag;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.util.Util;


public class NameTrains extends SpecialProperty {

    private String tokenClassName;
    private Class<? extends NamedTrainToken> tokenClass;
    private List<NamedTrainToken> tokens = new ArrayList<NamedTrainToken>(2);
    private String name = "NameTrains";

    /**
     * Used by Configure (via reflection) only
     */
    public NameTrains(RailsItem parent, String id) {
        super(parent, id);
    }

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

        tokenClass = Configure.getClassForName(NamedTrainToken.class, tokenClassName);

        String tokenTagName = tokenClassName.replaceAll(".*\\.", "");
        List<Tag> tokenTags = assignTag.getChildren(tokenTagName);
        if (tokenTags == null || tokenTags.isEmpty()) {
            throw new ConfigurationException(
                    "No <" + tokenTagName
                            + "> tags found in <AssignNamedTrain>");
        }

        description = name + ": ";

        for (Tag tokenTag : tokenTags) {
            String tokenName = tokenTag.getAttributeAsString("name");
            if (!Util.hasValue(tokenName)) {
                throw new ConfigurationException(
                        "Named Train token must have a name");
            }
            NamedTrainToken token = Configure.create(tokenClass, this, tokenName);
            tokens.add(token);
            token.configureFromXML(tokenTag);
            description += token.getLongName() + ": " + Bank.format(this, token.getValue()) + ", ";
        }
        description = description.replaceFirst(", $", "");
    }

    @Override
    public void finishConfiguration (RailsRoot root)
    throws ConfigurationException {

        for (NamedTrainToken token : tokens) {
            token.finishConfiguration(root);
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
    public void setExercised() {
        // do nothing
    }

    @Override
    public String toMenu() {
        return description;
    }

    @Override
    public String toText() {
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
