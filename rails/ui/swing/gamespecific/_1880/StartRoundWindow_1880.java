/**
 * 
 */
package rails.ui.swing.gamespecific._1880;



import javax.swing.JDialog;
import rails.ui.swing.StartRoundWindow;
import rails.common.LocalText;
import rails.game.action.PossibleAction;
import rails.game.action.StartItemAction;
import rails.game.specific._1880.BuyStartItem_1880;
import rails.ui.swing.elements.*;


/**
 * @author Martin
 * @date 07.05.2011
 */
public class StartRoundWindow_1880 extends StartRoundWindow {

    
    /* Keys of dialogues owned by this class */
    public static final String COMPANY_START_PRICE_DIALOG = "CompanyStartPrice";
    public static final String COMPANY_BUILDING_RIGHT_DIALOG = "CompanyBuildingRight";
    public static final String COMPANY_PRESIDENCY_PERCENTAGE_DIALOG = "CompanyPresidentPercentage";
    
    protected JDialog currentDialog = null;
    protected PossibleAction currentDialogAction = null;
    protected int[] startPrices = null;
    protected int[] operationOrder = null;
    String[] bRights = {"A+B+C", "B+C+D"};
    
    private static final long serialVersionUID = 1L;
    /**
     * @param round
     * @param parent
     */
    public StartRoundWindow_1880() {
    }
    
  
   @Override
   public boolean processImmediateAction() {
           
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
                  return false;
               }
           }
       }
       return true;
   }

   
   
   protected boolean requestStartPrice(BuyStartItem_1880 activeItem) {

       if (activeItem.hasSharePriceToSet()) {
           String compName = activeItem.getCompanyToSetPriceFor();

           // Get a sorted prices List
           // TODO: should be included in BuyStartItem
//           List<StockSpaceI> startSpaces = stockMarket.getStartSpaces();
//           Map<Integer, StockSpaceI> spacePerPrice =
//                   new HashMap<Integer, StockSpaceI>();
//           startPrices = new int[startSpaces.size()];
           String[] options = {"100-Slot 1","100-Slot 2","100-Slot 3","100-Slot 4"};
//           for (int i = 0; i < startSpaces.size(); i++) {
//               if (((StockMarket_1880) stockMarket).getParSlot(startSpaces.get(i).getPrice())) { //Make sure we got a Parslot left over
//               startPrices[i] = startSpaces.get(i).getPrice();
//               spacePerPrice.put(startPrices[i], startSpaces.get(i));
//               }
//           }
//           Arrays.sort(startPrices);
//           for (int i = 0; i < startSpaces.size(); i++) {
//               options[i] = Bank.format(spacePerPrice.get(startPrices[i]).getPrice());
//           }
//          options[0] = "100";
           RadioButtonDialog dialog = new RadioButtonDialog(
                   COMPANY_START_PRICE_DIALOG,
                   this,
                   this,
                   LocalText.getText("PleaseSelect"),
                           LocalText.getText("WHICH_START_PRICE",
                                   activeItem.getPlayerName(),
                                   compName),
                           options,
                           0);
           setCurrentDialog (dialog, activeItem);
           }
       return true;
   }
  
  protected boolean requestBuildingRight(BuyStartItem_1880 activeItem) {
       
      
      
       String compName = activeItem.getCompanyToSetPriceFor();
      
       
       RadioButtonDialog dialog = new RadioButtonDialog(
               COMPANY_BUILDING_RIGHT_DIALOG,
               this,
               this,
               LocalText.getText("PleaseSelect"),       
                       LocalText.getText("WhichBuildingRight",
                              activeItem.getPlayerName(),
                              compName),
                       bRights,
                       0);
       setCurrentDialog (dialog, activeItem);
       return true;
   }   

  public void dialogActionPerformed () {
      
      String key="";
      
      if (currentDialog instanceof NonModalDialog) key = ((NonModalDialog) currentDialog).getKey();
      
      if (COMPANY_START_PRICE_DIALOG.equals(key)) {
      
          RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
          BuyStartItem_1880 action = (BuyStartItem_1880) currentDialogAction;

          int index = dialog.getSelectedOption();
          if (index >= 0) {
              int price = 100;
              action.setAssociatedSharePrice(price);
              action.setParSlotIndex(index);  // index happens to line up to allows this.
              
              requestBuildingRight((BuyStartItem_1880) action);
              } else {
              // No selection done - no action
              return;
          }

      } else if (COMPANY_BUILDING_RIGHT_DIALOG.equals(key)) {
        
          RadioButtonDialog dialog = (RadioButtonDialog) currentDialog;
          BuyStartItem_1880 action = (BuyStartItem_1880) currentDialogAction;

          int index = dialog.getSelectedOption();
          if (index >= 0) {
            String buildingRight=bRights[index];
            action.setAssociatedBuildingRight(buildingRight);
            process (action);
          } else {
           // No selection done - no action
              return; 
          }
          
      }
      return;
  }
  
  public void setCurrentDialog (JDialog dialog, PossibleAction action) {
      if (currentDialog != null) {
          currentDialog.dispose();
      }
      currentDialog = dialog;
      currentDialogAction = action;
      disableButtons();
  }
}
   
