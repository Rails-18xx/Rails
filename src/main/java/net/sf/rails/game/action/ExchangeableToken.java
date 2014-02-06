/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/ExchangeableToken.java,v 1.1 2009/05/04 20:29:15 evos Exp $
 *
 * Created on 20-May-2006
 * Change Log:
 */
package net.sf.rails.game.action;


import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * A simple, serializable class that holds the <i>original</i> location
 * of a Base token, to facilitate its replacement even after its company
 * has been closed and all its tokens removed. This class is used in
 * the ExchangeTokens action class.
 * @author Erik Vos
 */
public class ExchangeableToken implements Serializable {
    
    private String cityName;
    private String oldCompanyName;
    private boolean selected = false;
    
    public static final long serialVersionUID = 1L;
    
    public ExchangeableToken (String cityName, String oldCompanyName) {
        this.cityName = cityName;
        this.oldCompanyName = oldCompanyName;
    }

    public String getCityName() {
        return cityName;
    }

    public String getOldCompanyName() {
        return oldCompanyName;
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
    
    public boolean equalsAsOption(ExchangeableToken other) {
        return Objects.equal(this.cityName, other.cityName) 
                && Objects.equal(this.oldCompanyName, other.oldCompanyName);
    }
    
    public String toString() {
        return cityName+"["+oldCompanyName+"]"
            + (selected ? "*" : "");
    }
    
}

