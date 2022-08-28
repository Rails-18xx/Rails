package rails.game.action;

import com.google.common.base.Objects;
import net.sf.rails.common.LocalText;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.util.RailsObjects;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.EnumSet;

public class AdjustSharePrice extends PossibleAction {

    public enum Direction {
        UP,
        DOWN,
        LEFT,
        RIGHT
    }

    /* Preset server attributes */
    private transient PublicCompany company;
    private String companyName;

    private EnumSet<Direction> directions;

    /* User-set client attributes */
    private Direction chosenDirection;

    public AdjustSharePrice(PublicCompany company, EnumSet<Direction> directions) {

        super(company.getRoot());

        this.company = company;
        this.companyName = company.getId();

        this.directions = directions;
    }

    public void setChosenDirection(Direction chosenDirection) {
        this.chosenDirection = chosenDirection;
    }

    public PublicCompany getCompany() {
        return company;
    }

    public String getCompanyName() {
        return companyName;
    }

    public EnumSet<Direction> getDirections() {
        return directions;
    }

    public Direction getChosenDirection() {
        return chosenDirection;
    }

    public String toMenu() {
        return LocalText.getText("AdjustSharePrice", companyName);
    }

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        AdjustSharePrice action = (AdjustSharePrice) pa;
        boolean options =
                Objects.equal(this.company, action.company)
                        && Objects.equal(this.directions, action.directions);

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
                && Objects.equal(this.chosenDirection, action.chosenDirection)
                ;
    }

    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                        .addToString("company", company)
                        .addToString("directions", directions)
                        .addToStringOnlyActed("chosenDirection", chosenDirection)
                        .toString()
                ;

    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        company = getCompanyManager().getPublicCompany(companyName);
    }

}
