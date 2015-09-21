package net.sf.rails.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import net.sf.rails.game.Company;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.state.MoneyOwner;



public class SequenceUtil {

    // private constructor
    private SequenceUtil() {};
    
    private static <E extends MoneyOwner> List<E> 
        selectMoneyOwners(Class<E> clazz, Collection<MoneyOwner> coll)  {
        
        // select all cashholders of that type
        List<E> list = new ArrayList<E>();
        
        for (MoneyOwner c:coll) { 
            if (clazz.isAssignableFrom(c.getClass())) {
                @SuppressWarnings("unchecked")
                E cast = (E) c;
                list.add(cast);
            }
        }
        
        return list;
    }
    
    
    /**
     * Defines a sorting on cashHolders
     * @return sorted list of cashholders
     */
    public static List<MoneyOwner> sortCashHolders(Collection<MoneyOwner> coll) {
        
       List<MoneyOwner> sortedList = new ArrayList<MoneyOwner>();

       // first add players
       List<Player> players = selectMoneyOwners(Player.class, coll);
       Collections.sort(players);
       sortedList.addAll(players);
       
       // then public companies
       List<PublicCompany> PublicCompanys = selectMoneyOwners(PublicCompany.class, coll);
       Collections.sort(PublicCompanys, Company.COMPANY_COMPARATOR);
       sortedList.addAll(PublicCompanys);
       
       // last add the bank
       sortedList.addAll(selectMoneyOwners(Bank.class, coll));
       
       return sortedList;
    }
}
