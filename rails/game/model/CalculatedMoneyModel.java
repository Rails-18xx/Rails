package rails.game.model;

import java.lang.reflect.Method;

import org.apache.log4j.Logger;

import rails.game.Bank;
import rails.game.state.Item;

/**
 * This class allows calculated values to be used as model objects by using
 * reflection. The value to be calculated is obtained by calling a method
 * specified by name. This method must return an Integer and may not have any
 * parameters.
 * 
 * @author Erik Vos
 * 
 */
public class CalculatedMoneyModel extends AbstractModel<String> implements Observer {

    protected static Logger log =
        Logger.getLogger(CalculatedMoneyModel.class.getPackage().getName());

    private Item owner;
    private String methodName;

    public boolean suppressZero = false;

    public CalculatedMoneyModel(Item owner, String methodName) {
        super(owner, methodName);
        this.owner = owner;
        this.methodName = methodName;
    }

    
    public void setSuppressZero(boolean suppressZero) 
    {
        this.suppressZero = suppressZero;
    }
    
    protected int calculate() {

        Class<?> objectClass = owner.getClass();
        Integer amount;
        try {
            Method method = objectClass.getMethod(methodName, (Class[]) null);
            amount = (Integer) method.invoke(owner, (Object[]) null);
        } catch (Exception e) {
            log.error("ERROR while invoking method " + methodName
                      + " on class " + objectClass.getName(), e);
            return -1;
        }
        return amount.intValue();
    }

    public String getData() {
        int amount = calculate();
        if (amount == 0 && suppressZero)
            return "";
        else
            return Bank.format(amount);
    }

}
