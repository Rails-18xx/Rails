/**
 * 
 */
package rails.ui.swing.gamespecific._1880;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JOptionPane;

import rails.game.StartRound;
import rails.game.StockMarketI;
import rails.game.StockSpace;
import rails.game.StockSpaceI;
import rails.ui.swing.*;
import rails.ui.swing.GameUIManager;
import rails.ui.swing.StartRoundWindow;
import rails.common.LocalText;
import rails.game.GameManager;
import rails.game.action.BuyStartItem;
import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;
import rails.game.specific._1880.BuyStartItem_1880;


/**
 * @author Martin
 * @date 07.05.2011
 */
public class StartRoundWindow_1880 extends StartRoundWindow {

    private static final long serialVersionUID = 1L;
    /**
     * @param round
     * @param parent
     */
    public StartRoundWindow_1880() {
    }
    
  
   @Override
   public boolean processImmediateAction() {
           BuyStartItem_1880 action2;
           
       log.debug("ImmediateAction=" + immediateAction);
       if (immediateAction != null) {
           // Make a local copy and discard the original,
           // so that it's not going to loop.
          PossibleAction nextAction = immediateAction;
          immediateAction = null;
          if (nextAction instanceof StartItemAction) {
               StartItemAction action = (StartItemAction) nextAction;
               if (action instanceof BuyStartItem_1880) {
                   requestStartPrice((BuyStartItem_1880) action);
                   requestBuildingRight((BuyStartItem_1880) action);
                  return process(action);
               }
           }
       }
       return true;
   }

   
   
   protected boolean requestStartPrice(BuyStartItem_1880 activeItem) {

       if (activeItem.hasSharePriceToSet()) {
           String compName = activeItem.getCompanyToSetPriceFor();
           StockMarketI stockMarket = getGameUIManager().getGameManager().getStockMarket();

           // Get a sorted prices List
           // TODO: should be included in BuyStartItem
           List<StockSpaceI> startSpaces = stockMarket.getStartSpaces();
           Map<Integer, StockSpaceI> spacePerPrice =
                   new HashMap<Integer, StockSpaceI>();
           int[] prices = new int[startSpaces.size()];
           StockSpaceI[] options = new StockSpaceI[startSpaces.size()];
           for (int i = 0; i < startSpaces.size(); i++) {
               prices[i] = startSpaces.get(i).getPrice();
               spacePerPrice.put(prices[i], startSpaces.get(i));
           }
           Arrays.sort(prices);
           for (int i = 0; i < startSpaces.size(); i++) {
               options[i] = spacePerPrice.get(prices[i]);
           }

           StockSpace sp =
                   (StockSpace) JOptionPane.showInputDialog(this,
                           LocalText.getText("WHICH_START_PRICE",
                                   activeItem.getPlayerName(),
                                   compName),
                           LocalText.getText("WHICH_PRICE"),
                           JOptionPane.QUESTION_MESSAGE, null, options,
                           options[0]);
           if (sp == null) {
               return false;
           }
           int price = sp.getPrice();
           activeItem.setAssociatedSharePrice(price);
       }
       return true;
   }
  
  protected boolean requestBuildingRight(BuyStartItem_1880 activeItem) {
       
      String[] bRights = {"A","B","C","D","A+B","A+B+C","B+C","B+C+D","C+D"};
      
       String compName = activeItem.getCompanyToSetPriceFor();
      
      String initialBuildingRight = null;
                initialBuildingRight=(String) JOptionPane.showInputDialog(this,
                      LocalText.getText("WHICH_BUILDING_RIGHT",
                              activeItem.getPlayerName(),
                              compName),
                       LocalText.getText("WHICH_RIGHT"),
                       JOptionPane.QUESTION_MESSAGE, null, bRights,
                       bRights[0]);
       if (initialBuildingRight.isEmpty()){
          return false;
       }
       activeItem.setAssociatedBuildingRight(initialBuildingRight);
       return true;
   }   
 
}
   
