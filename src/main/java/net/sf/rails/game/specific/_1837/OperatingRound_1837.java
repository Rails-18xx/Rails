/**
 * 
 */
package net.sf.rails.game.specific._1837;

import java.util.Map;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.Iterables;
import com.google.common.collect.Table;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.specific._1835.GameManager_1835;
import net.sf.rails.game.specific._1835.PrussianFormationRound;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.MoneyOwner;

/**
 * @author Martin
 *
 */
public class OperatingRound_1837 extends OperatingRound {
  
    private final BooleanState needSuedBahnFormationCall = BooleanState.create(this, "NeedSuedBahnFormationCall");
    private final BooleanState needHungaryFormationCall = BooleanState.create(this, "NeedHungaryFormationCall");
    private final BooleanState needKuKFormationCall = BooleanState.create(this, "NeedKuKFormationCall");

    /**
     * Registry of percentage of nationals revenue to be denied per player
     * because of having produced revenue in the same OR.
     */
    private final Table<Player, PublicCompany, Integer> deniedIncomeShare = HashBasedTable.create();
    /**
     * @param parent
     * @param id
     */
    public OperatingRound_1837(GameManager parent, String id) {
        super(parent, id);
        // TODO Auto-generated constructor stub
    }

    /* (non-Javadoc)
     * @see net.sf.rails.game.OperatingRound#payout(int)
     */
    @Override
    public void payout(int amount) {
        // TODO Auto-generated method stub
        super.payout(amount);
    }
    @Override
    protected void newPhaseChecks() {
        Phase phase = getCurrentPhase();
        if (phase.getId().equals("4")
                && !companyManager.getPublicCompany(GameManager_1837.SU_ID).hasStarted()
                && !SuedBahnFormationRound.SuedbahnIsComplete(gameManager)) {
            if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                // Postpone until trains are discarded
                needSuedBahnFormationCall.set(true);
            } else {
                // Do it immediately
                ((GameManager_1837)gameManager).startSuedBahnFormationRound (this);
            }
        }
    }
    
    private void addIncomeDenialShare (Player player, PublicCompany company, int share) {

        if (!deniedIncomeShare.contains(player, company)) {
            deniedIncomeShare.put(player, company, share);
        } else {
            deniedIncomeShare.put(player, company, share + deniedIncomeShare.get(player, company));
        }
        //log.debug("+++ Denied "+share+"% share of PR income to "+player.getName());
    }

    /** Count the number of shares per revenue recipient<p>
     * A special rule applies to 1835 to prevent black privates and minors providing
     * income twice during an OR.
     */
    @Override
    protected  Map<MoneyOwner, Integer>  countSharesPerRecipient () {

        Map<MoneyOwner, Integer> sharesPerRecipient = super.countSharesPerRecipient();

        if (operatingCompany.value().getId().equalsIgnoreCase(GameManager_1837.SU_ID)) {
            for (Player player : deniedIncomeShare.rowKeySet()) {
                if (!sharesPerRecipient.containsKey(player)) continue;
                int share = deniedIncomeShare.get(player,operatingCompany.value());
                int shares = share / operatingCompany.value().getShareUnit();
                sharesPerRecipient.put (player, sharesPerRecipient.get(player) - shares);
                ReportBuffer.add(this, LocalText.getText("NoIncomeForPreviousOperation",
                        player.getId(),
                        share,
                        GameManager_1837.SU_ID));
            }
        }
        

        return sharesPerRecipient;
    }
    /**
     * Register black minors as having operated
     * for the purpose of denying income after conversion to a PR share
     */
    @Override
    protected void initTurn() {

        super.initTurn();

        Set<SpecialProperty> sps = operatingCompany.value().getSpecialProperties();
        if (sps != null && !sps.isEmpty()) {
            ExchangeForShare efs = (ExchangeForShare) Iterables.get(sps, 0);
            addIncomeDenialShare (operatingCompany.value().getPresident(), operatingCompany.value(), efs.getShare());
        }
    }

    @Override
    public void resume() {
        PublicCompany suedbahn = companyManager.getPublicCompany(GameManager_1837.SU_ID);

        if (suedbahn.hasFloated() && !suedbahn.hasOperated()
                // PR has just started. Check if it can operate this round
                // That's only the case if a CoalTrain has just bought
                // the first 4-train or or 4E or 4+1-train
                && operatingCompany.value().getType().getId().equals("Coal") ) {
            log.debug("S1 has not operated: Suedbahn can operate");

            // Insert the Prussian before the first major company
            // with a lower current price that has not yet operated
            // and isn't currently operating

            int index = 0;
            int operatingCompanyndex = getOperatingCompanyndex();
            for (PublicCompany company : setOperatingCompanies()) {
                if (index > operatingCompanyndex
                        && company.hasStockPrice()
                        && company.hasFloated()
                        && !company.isClosed()
                        && company != operatingCompany.value()
                        && company.getCurrentSpace().getPrice()
                        < suedbahn.getCurrentSpace().getPrice()) {
                    log.debug("PR will operate before "+company.getId());
                    break;
                }
                index++;
            }
            // Insert PR at the found index (possibly at the end)
            operatingCompanies.add(index, suedbahn);
            log.debug("SU will operate at order position "+index);

        } else {

            log.debug("S1 has operated: SU cannot operate");

        }

        guiHints.setCurrentRoundType(getClass());
        super.resume();
    }

}
