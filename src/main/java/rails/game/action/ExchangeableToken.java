package rails.game.action;

import java.io.Serializable;

import com.google.common.base.Objects;

/**
 * A simple, serializable class that holds the <i>original</i> location
 * of a Base token, to facilitate its replacement even after its company
 * has been closed and all its tokens removed. This class is used in
 * the ExchangeTokens action class.
 * 
 * Rails 2.0: Added new equals and hashcode classes to allow passing tests of the according action
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
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null) return false;
        if (this.getClass() != o.getClass()) return false;
        
        ExchangeableToken other = (ExchangeableToken) o;
        return Objects.equal(this.cityName, other.cityName)
                && Objects.equal(this.oldCompanyName, other.oldCompanyName);
    }
    
    @Override 
    public int hashCode() {
        return Objects.hashCode(cityName, oldCompanyName);
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this)
                .add("cityName", cityName)
                .add("oldCompanyName", oldCompanyName)
                .add("selected", selected)
                .toString()
        ;
    }
}

