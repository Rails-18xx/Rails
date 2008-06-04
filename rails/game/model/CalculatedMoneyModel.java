/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/model/CalculatedMoneyModel.java,v 1.5 2008/06/04 19:00:37 evos Exp $*/
package rails.game.model;

import java.lang.reflect.Method;

import rails.game.Bank;

/**
 * This class allows calculated values to be used as model objects by using
 * reflection. The value to be calculated is obtained by calling a method
 * specified by name. This method must return an Integer and may not have any
 * parameters.
 * 
 * @author Erik Vos
 * 
 */
public class CalculatedMoneyModel extends ModelObject {

    public static final int SUPPRESS_ZERO = 1;

    private Object object;
    private String methodName;

    public CalculatedMoneyModel(Object object, String methodName) {

        this.object = object;
        this.methodName = methodName;

    }

    protected int calculate() {

        Class<?> objectClass = object.getClass();
        Integer amount;
        try {
            Method method = objectClass.getMethod(methodName, (Class[]) null);
            amount = (Integer) method.invoke(object, (Object[]) null);
        } catch (Exception e) {
            log.error("ERROR while invoking method " + methodName
                      + " on class " + objectClass.getName(), e);
            return -1;
        }
        return amount.intValue();
    }

    @Override
    public String getText() {
        int amount = calculate();
        if (amount == 0 && option == SUPPRESS_ZERO)
            return "";
        else
            return Bank.format(amount);
    }

}
