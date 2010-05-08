package rails.game.specific._1835;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import rails.game.Bank;
import rails.game.CashHolder;
import rails.game.DisplayBuffer;
import rails.game.GameDef;
import rails.game.GameManagerI;
import rails.game.GameOption;
import rails.game.OperatingRound;
import rails.game.PhaseI;
import rails.game.Player;
import rails.game.Portfolio;
import rails.game.PrivateCompanyI;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.ReportBuffer;
import rails.game.StockSpaceI;
import rails.game.action.DiscardTrain;
import rails.game.action.LayTile;
import rails.game.move.CashMove;
import rails.game.move.MapChange;
import rails.game.special.ExchangeForShare;
import rails.game.special.SpecialPropertyI;
import rails.game.special.SpecialTileLay;
import rails.game.state.BooleanState;
import rails.util.LocalText;

public class OperatingRound_1835 extends OperatingRound {
    
    private BooleanState needPrussianFormationCall 
            = new BooleanState ("NeedPrussianFormationCall", false);
    private BooleanState hasLaidExtraOBBTile
            = new BooleanState ("HasLaidExtraOBBTile", false);
    
    /** 
     * Registry of percentage of PR revenue to be denied per player
     * because of having produced revenue in the same OR.
     */
    private Map<Player, Integer> deniedIncomeShare;

    public OperatingRound_1835 (GameManagerI gameManager) {
        super (gameManager);
        deniedIncomeShare = new HashMap<Player, Integer> ();
    }

    /** Can a public company operate? (1835 special version) */
    protected boolean canCompanyOperateThisRound (PublicCompanyI company) {
        return company.hasFloated() && !company.isClosed()
                // 1835 special: in some variants minors don't run if BY has not floated
                && (!gameManager.getGameOption(GameOption.VARIANT).equalsIgnoreCase("Clemens")
                        || companyManager.getPublicCompany(GameManager_1835.BY_ID).hasFloated());
    }

    protected void privatesPayOut() {
        int count = 0;
        for (PrivateCompanyI priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                if (((Portfolio)priv.getHolder()).getOwner().getClass() != Bank.class) {
                    CashHolder recipient = ((Portfolio)priv.getHolder()).getOwner();
                    int revenue = priv.getRevenueByPhase(getCurrentPhase()); // sfy 1889: revenue by phase
                    if (count++ == 0) ReportBuffer.add("");
                    ReportBuffer.add(LocalText.getText("ReceivesFor",
                            recipient.getName(),
                            Bank.format(revenue),
                            priv.getName()));
                    new CashMove(bank, recipient, revenue);
                    
                    /* Register black private equivalent PR share value 
                     * so it can be subtracted if PR operates */
                    if (recipient instanceof Player && priv.getSpecialProperties() != null
                            && priv.getSpecialProperties().size() > 0) {
                        SpecialPropertyI sp = priv.getSpecialProperties().get(0);
                        if (sp instanceof ExchangeForShare) {
                            ExchangeForShare efs = (ExchangeForShare) sp;
                            if (efs.getPublicCompanyName().equalsIgnoreCase(GameManager_1835.PR_ID)) {
                                int share = efs.getShare();
                                Player player = (Player) recipient;
                                addIncomeDenialShare (player, share);
                            }
                            
                        }
                    }
                }

            }
        }

    }

    private void addIncomeDenialShare (Player player, int share) {
        
        if (!deniedIncomeShare.containsKey(player)) {
            new MapChange<Player, Integer> (deniedIncomeShare, player, share);
        } else {
            new MapChange<Player, Integer> (deniedIncomeShare, player, 
                    share + deniedIncomeShare.get(player));
        }
        //log.debug("+++ Denied "+share+"% share of PR income to "+player.getName());
    }

    /** Count the number of shares per revenue recipient<p>
     * A special rule applies to 1835 to prevent black privates and minors providing
     * income twice during an OR.
     */
    protected  Map<CashHolder, Integer>  countSharesPerRecipient () {
        
        Map<CashHolder, Integer> sharesPerRecipient = super.countSharesPerRecipient();
        
        if (operatingCompany.getName().equalsIgnoreCase(GameManager_1835.PR_ID)) {
            for (Player player : deniedIncomeShare.keySet()) {
                int share = deniedIncomeShare.get(player);
                int shares = share / operatingCompany.getShareUnit();
                sharesPerRecipient.put (player, sharesPerRecipient.get(player) - shares);
                ReportBuffer.add(LocalText.getText("NoIncomeForPreviousOperation",
                        player.getName(),
                        share,
                        GameManager_1835.PR_ID));
                
            }
        }
        
        return sharesPerRecipient;
    }

     /** 
      * Register black minors as having operated 
     * for the purpose of denying income after conversion to a PR share
     */
    protected void initTurn() {
        
        super.initTurn();

        List<SpecialPropertyI> sps = operatingCompany.getSpecialProperties();
        if (sps != null && !sps.isEmpty()) {
            ExchangeForShare efs = (ExchangeForShare) sps.get(0);
            addIncomeDenialShare (operatingCompany.getPresident(), efs.getShare());
        }
    }
    
    @Override
    public void resume() {
        
        PublicCompanyI prussian = companyManager.getPublicCompany(GameManager_1835.PR_ID);
        
        if (prussian.hasFloated() && !prussian.hasOperated()
                // PR has just started. Check if it can operate this round
                // That's only the case if M1 has just bought 
                // the first 4-train or 4+4-train
                && operatingCompany == companyManager.getPublicCompany("M1")) {
            log.debug("M2 has not operated: PR can operate");
            
            // Insert the Prussian before the first major company
            // with a lower current price that hoas not yet operated
            // and isn't currently operating
            
            int index = 0;
            int operatingCompanyIndex = operatingCompanyIndexObject.intValue();
            for (PublicCompanyI company : getOperatingCompanies()) {
                if (index > operatingCompanyIndex
                        && company.hasStockPrice() 
                        && company.hasFloated()
                        && !company.isClosed()
                        && company != operatingCompany
                        && company.getCurrentSpace().getPrice() 
                            < prussian.getCurrentSpace().getPrice()) {
                    log.debug("PR will operate before "+company.getName());
                    break;
                }
                index++;
            }
            // Insert PR at the found index (possibly at the end)
            List<PublicCompanyI> companies
                = new ArrayList<PublicCompanyI>(Arrays.asList(operatingCompanyArray));
            companies.add(index, prussian);
            operatingCompanyArray = companies.toArray(new PublicCompanyI[0]);
            log.debug("PR will operate at order position "+index);

        } else {
            
            log.debug("M2 has operated: PR cannot operate");
               
        }
        
        guiHints.setCurrentRoundType(getClass());
        super.resume();
    }
    
    protected void setSpecialTileLays() {

        /* Special-property tile lays */
        currentSpecialTileLays.clear();

        if (!operatingCompany.canUseSpecialProperties()) return;

        for (SpecialTileLay stl : getSpecialProperties(SpecialTileLay.class)) {
            if (stl.isExtra() || !currentNormalTileLays.isEmpty()) {
                
                // Exclude the second OBB free tile if the first was laid in this round
                if (stl.getLocationNameString().matches("M1(7|9)")
                        && hasLaidExtraOBBTile.booleanValue()) continue;
                
                currentSpecialTileLays.add(new LayTile(stl));
            }
        }
    }

    public boolean layTile(LayTile action) {

        // The extra OBB tiles may not both be laid in the same round
        if (action.getSpecialProperty() != null
                && action.getSpecialProperty().getLocationNameString().matches("M1(5|7)")) {
            if (hasLaidExtraOBBTile.booleanValue()) {
                String errMsg = LocalText.getText("InvalidTileLay");
                DisplayBuffer.add(LocalText.getText("CannotLayTileOn",
                        action.getCompanyName(),
                        action.getLaidTile().getExternalId(),
                        action.getChosenHex().getName(),
                        Bank.format(0),
                        errMsg ));
                return false;
            }
        }
        
        boolean result = super.layTile(action);
        
        if (result && action.getSpecialProperty() != null
                && action.getSpecialProperty().getLocationNameString().matches("M1(5|7)")) {
            hasLaidExtraOBBTile.set(true);
        }
        
        return result;
    }

    protected void newPhaseChecks() {
        PhaseI phase = getCurrentPhase();
        if (phase.getName().equals("4") || phase.getName().equals("4+4")
                || phase.getName().equals("5")) {
            if (!PrussianFormationRound.prussianIsComplete(gameManager)) {
                if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                    // Postpone until trains are discarded
                    needPrussianFormationCall.set(true);
                } else {
                    // Do it immediately
                    ((GameManager_1835)gameManager).startPrussianFormationRound (this);
                }
            }
        }
    }
    
    public boolean discardTrain(DiscardTrain action) {
        
        boolean result = super.discardTrain(action);
        if (result && getStep() == GameDef.OrStep.BUY_TRAIN 
                && needPrussianFormationCall.booleanValue()) {
            // Do the postponed formation calls 
            ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            needPrussianFormationCall.set(false);
        }
        return result;
    }
}
