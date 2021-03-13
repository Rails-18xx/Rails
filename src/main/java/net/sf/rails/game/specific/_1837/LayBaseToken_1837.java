package net.sf.rails.game.specific._1837;

import com.google.common.base.Objects;
import net.sf.rails.game.CompanyManager;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsRoot;
import net.sf.rails.util.RailsObjects;
import net.sf.rails.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rails.game.action.PossibleAction;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

/**
 * This class is only intended to lay a major company token
 * on a former minor hex after a merge action, if requested so.
 * It also allows laying two tokens on both minors
 * that are related to some majors.
 *
 * As the CoalExchangeRound is a Stock Round subclass,
 * this action cannot be a PossibleORAction, and therefore
 * not be a subclass of LayBaseToken.
 */
public class LayBaseToken_1837 extends PossibleAction {

    /*--- Preconditions --*/

    /** The major company involved in the merger(s) */
    private transient PublicCompany major;
    private String majorName;

    /** The minor company(ies) being merged */
    private transient List<PublicCompany> minors;
    private String minorNames;

    /*--- Postconditions ---*/

    /** Whether or not tokens are to be laid */
    private List<Boolean> selected;
    //private transient boolean[] selected;
    private String selectedString;

    //private static final Logger log = LoggerFactory.getLogger(LayBaseToken_1837.class);

    public static final long serialVersionUID = 1L;

    public LayBaseToken_1837 (RailsRoot root, PublicCompany major, PublicCompany minor) {
        this(root, major, Arrays.asList(minor));
    }

    public LayBaseToken_1837 (RailsRoot root, PublicCompany major, List<PublicCompany> minors) {
        super (root);
        this.major = major;
        this.minors = minors;
        majorName = major.getId();
        int numberOfMinors = minors.size();
        String[] names = new String[numberOfMinors];
        int i=0;
        for (PublicCompany minor : minors) {
            names[i++] = minor.getId();
        }
        minorNames = String.join(",", names);
        //log.info (">>>>> Serialize: {} -> \"{}\"", minors, minorNames);

        selected = new ArrayList<>(Collections.nCopies(numberOfMinors, false));
        //Collections.fill(selected, false);
        setSelectedString();
        // Warning: the above initializes the Boolean objects as null,
        // so these must always be set later to either false or true.
        // For an alternative that initializes to false:
        //selected = Collections.nCopies(numberOfMinors, false);
    }

    /*--- Getters ---*/

    public PublicCompany getMajor () {
        return major;
    };

    public List<PublicCompany> getMinors () {
        return minors;
    }

    public PublicCompany getMinor (int index) {
        return minors.get(index);
    }

    public List<Boolean> getSelected() {
    //public boolean[] getSelected() {
        return selected;
    }

    public boolean getSelected(int index) {
        return selected.get(index);
        //return selected[index];
    }

    /*--- Setters ---*/

    public void setSelected(int index, boolean value) {
        //selected[index] = value;
        selected.set(index, value);
        setSelectedString();
    }

    private void setSelectedString() {
        selectedString = "";
        if (selected != null && !selected.isEmpty()) {
            for (boolean b : selected) {
                selectedString += ("," + Boolean.valueOf(b));
            }
            selectedString = selectedString.replaceFirst("^,", "");
        }
    }

    /*--- Validation ---*/
    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        LayBaseToken_1837 action = (LayBaseToken_1837)pa;

        // check asOption attributes
        boolean options = Objects.equal(this.major, action.major)
                && Objects.equal(this.minors, action.minors);

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options && Objects.equal(this.selected, action.selected);
    }

    /*--- Printable contents ---*/
    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                        .addToString("major", majorName)
                        .addToString("minors", minors)
                        .addToStringOnlyActed("selected", selected)
                        .toString()
                ;
    }

    /*--- Deserialize ---*/
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        CompanyManager cmgr = getRoot().getCompanyManager();
        if (Util.hasValue(majorName)) {
            major = cmgr.getPublicCompany(majorName);
        }

        String[] names = minorNames.split(",");
        minors = new ArrayList<>();
        for (int i=0; i<names.length; i++) {
            minors.add(cmgr.getPublicCompany(names[i]));
        }
        //log.info ("<<<<< Deserialize: \"{}\" -> {}", minorNames, minors);

        //selected = new boolean[minors.size()];
        selected = new ArrayList<>();
        //int index = 0;
        for (String s : selectedString.split(",")) {
            //selected[index++] = Boolean.parseBoolean(s);
            selected.add(Boolean.parseBoolean(s));
        }
    }
}
