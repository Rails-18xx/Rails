package rails.game.specific._1880;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.TrainType;
import rails.game.action.PossibleAction;

public class ExchangeForCash extends PossibleAction {
    private static final Map<String, Integer> CASH_VALUE_MAP = createCashValueMap();
    private static Map<String, Integer> createCashValueMap() {
        Map<String, Integer> result = new HashMap<String, Integer>();
        result.put("2+2", 40);
        result.put("3", 70);
        result.put("3+3", 100);
        return Collections.unmodifiableMap(result);
    }

    private static final Map<String, Boolean> CHOICE_MAP = createChoiceMap();
    private static Map<String, Boolean> createChoiceMap() {
        Map<String, Boolean> result = new HashMap<String, Boolean>();
        result.put("2+2", true);
        result.put("3", true);
        result.put("3+3", false);
        return Collections.unmodifiableMap(result);
    }

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    private transient boolean ownerHasChoice;
    private String ownerName;
    private boolean exchangeCompany = false;
    private int value = 0;
    
    public ExchangeForCash() {
        
    }
    
    private ExchangeForCash(PrivateCompany company, int value, boolean ownerHasChoice) {
        this.ownerName = company.getOwner().getId(); //TODO: Check if GetPortfolio is not needed anymore..
        this.value = value;
        this.ownerHasChoice = ownerHasChoice;
    }

    static public ExchangeForCash getAction(PrivateCompany company, TrainType soldOutTrainType) {
        ExchangeForCash action = null;
        String trainName = soldOutTrainType.getName();
        if (CASH_VALUE_MAP.containsKey(trainName) == true) {
            action = new ExchangeForCash(company, CASH_VALUE_MAP.get(trainName), CHOICE_MAP.get(trainName));
        }
        return action;
    }
    
    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        if ((pa instanceof ExchangeForCash) && 
                (exchangeCompany == ((ExchangeForCash) pa).getExchangeCompany()) &&
                (value == ((ExchangeForCash) pa).getCashValue()) &&
                (ownerName == ((ExchangeForCash) pa).getOwnerName())) {
            return true;
        }
        return false;
    }

    @Override
    public boolean equalsAsAction(PossibleAction pa) {
        if ((pa instanceof ExchangeForCash) && 
                (exchangeCompany == ((ExchangeForCash) pa).getExchangeCompany()) &&
                (value == ((ExchangeForCash) pa).getCashValue()) &&
                (ownerName == ((ExchangeForCash) pa).getOwnerName())) {
            return true;
        }
        return false;
    }

    public String getOwnerName() {
        return ownerName;
    }

    public int getCashValue() {
        return value;
    }
    
    public boolean getOwnerHasChoice() {
        return ownerHasChoice;
    }

    public boolean getExchangeCompany() {
        return exchangeCompany;
    }

    public void setExchangeCompany(boolean exchangeCompany) {
        this.exchangeCompany = exchangeCompany;
    }
}
