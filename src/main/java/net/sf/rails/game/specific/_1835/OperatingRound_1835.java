package net.sf.rails.game.specific._1835;

import java.util.*;

import rails.game.action.DiscardTrain;
import rails.game.action.LayTile;
import net.sf.rails.common.DisplayBuffer;
import net.sf.rails.common.GameOption;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.GameDef;
import net.sf.rails.game.GameManager;
import net.sf.rails.game.OperatingRound;
import net.sf.rails.game.Phase;
import net.sf.rails.game.Player;
import net.sf.rails.game.PrivateCompany;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.special.ExchangeForShare;
import net.sf.rails.game.special.SpecialProperty;
import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Currency;
import net.sf.rails.game.state.HashMapState;
import net.sf.rails.game.state.MoneyOwner;
import net.sf.rails.game.state.Owner;

import com.google.common.collect.Iterables;


public class OperatingRound_1835 extends OperatingRound {

    private final BooleanState needPrussianFormationCall = BooleanState.create(this, "NeedPrussianFormationCall");
    private final BooleanState hasLaidExtraOBBTile = BooleanState.create(this, "HasLaidExtraOBBTile");

    /**
     * Registry of percentage of PR revenue to be denied per player
     * because of having produced revenue in the same OR.
     */
    private final HashMapState<Player, Integer> deniedIncomeShare = HashMapState.create(this, "deniedIncomeShare");

    /**
     * Constructed via Configure
     */
    public OperatingRound_1835 (GameManager parent, String id) {
        super (parent, id);
    }

    /** Can a public company operate? (1835 special version) */
    @Override
    protected boolean canCompanyOperateThisRound (PublicCompany company) {
        if (!company.hasFloated() || company.isClosed()) {
            return false;
        }
        // 1835 specials
        // Majors always operate
        if (company.hasStockPrice()) return true;
        // In some variants minors don't run if BY has not floated
        if (GameOption.getValue(this,GameOption.VARIANT).equalsIgnoreCase("Clemens")
                || GameOption.getValue(this, "MinorsRequireFloatedBY").equalsIgnoreCase("yes")) {
            return companyManager.getPublicCompany(GameManager_1835.BY_ID).hasFloated();
        }
        return true;
    }

    @Override
    protected void privatesPayOut() {
        int count = 0;
        for (PrivateCompany priv : companyManager.getAllPrivateCompanies()) {
            if (!priv.isClosed()) {
                // The bank portfolios are all not cashOwners themselves!
                if (priv.getOwner() instanceof MoneyOwner) {
                    Owner recipient = priv.getOwner();
                    int revenue = priv.getRevenueByPhase(Phase.getCurrent(this)); // sfy 1889: revenue by phase
                    if (count++ == 0) ReportBuffer.add(this, "");
                    String revText = Currency.fromBank(revenue, (MoneyOwner)recipient);
                    ReportBuffer.add(this, LocalText.getText("ReceivesFor",
                            recipient.getId(),
                            revText,
                            priv.getId()));

                    /* Register black private equivalent PR share value
                     * so it can be subtracted if PR operates */
                    if (recipient instanceof Player && priv.getSpecialProperties() != null
                            && priv.getSpecialProperties().size() > 0) {
                        SpecialProperty sp = Iterables.get(priv.getSpecialProperties(), 0);
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
            deniedIncomeShare.put(player, share);
        } else {
            deniedIncomeShare.put(player, share + deniedIncomeShare.get(player));
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

        if (operatingCompany.value().getId().equalsIgnoreCase(GameManager_1835.PR_ID)) {
            for (Player player : deniedIncomeShare.viewKeySet()) {
                if (!sharesPerRecipient.containsKey(player)) continue;
                int share = deniedIncomeShare.get(player);
                int shares = share / operatingCompany.value().getShareUnit();
                if (this.wasInterrupted()) { //Assuming that the interruption was cause by the Prussian Formation Round
                sharesPerRecipient.put (player, sharesPerRecipient.get(player) - shares);
                ReportBuffer.add(this, LocalText.getText("NoIncomeForPreviousOperation",
                        player.getId(),
                        share,
                        GameManager_1835.PR_ID));
                }

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
            addIncomeDenialShare (operatingCompany.value().getPresident(), efs.getShare());
        }
    }

    @Override
    public void resume() {
        PublicCompany prussian = companyManager.getPublicCompany(GameManager_1835.PR_ID);

        if (prussian.hasFloated() && !prussian.hasOperated()
                // PR has just started. Check if it can operate this round
                // That's only the case if M1 has just bought
                // the first 4-train or 4+4-train
                && operatingCompany.value() == companyManager.getPublicCompany("M1")) {
            log.debug("M2 has not operated: PR can operate");

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
                        < prussian.getCurrentSpace().getPrice()) {
                    log.debug("PR will operate before "+company.getId());
                    break;
                }
                index++;
            }
            // Insert PR at the found index (possibly at the end)
            operatingCompanies.add(index, prussian);
            log.debug("PR will operate at order position "+index);

        } else {

            log.debug("M2 has operated: PR cannot operate");

        }
        
        // Check if the operating company still exists
        if (operatingCompany.value().isClosed()) finishTurn();

        guiHints.setCurrentRoundType(getClass());
        super.resume();
    }

    @Override
    protected boolean validateSpecialTileLay (LayTile layTile) {

        if (!super.validateSpecialTileLay(layTile)) return false;

        // Exclude the second OBB free tile if the first was laid in this round
        if (layTile.getSpecialProperty().getLocationNameString().matches("M1(7|9)")
                && hasLaidExtraOBBTile.value()) return false;

        return true;
    }

    @Override
    public boolean layTile(LayTile action) {

        boolean hasJustLaidExtraOBBTile = action.getSpecialProperty() != null
        && action.getSpecialProperty().getLocationNameString().matches("M1(5|7)");

        // The extra OBB tiles may not both be laid in the same round
        if (hasJustLaidExtraOBBTile) {
            if (hasLaidExtraOBBTile.value()) {
                String errMsg = LocalText.getText("InvalidTileLay");
                DisplayBuffer.add(this, LocalText.getText("CannotLayTileOn",
                        action.getCompanyName(),
                        action.getLaidTile().toText(),
                        action.getChosenHex().getId(),
                        Bank.format(this, 0),
                        errMsg ));
                return false;
            } else {
                 // Duplicate, but we have to
                hasLaidExtraOBBTile.set(true);
                // Done here to make getSpecialTileLays() return the correct value.
                // It's provisional, on the assumption that other validations are OK.
                // TODO To get it really right, we should separate validation and execution.
            }
        }

        boolean result = super.layTile(action);

        if (!result && hasJustLaidExtraOBBTile) {
            // Revert if tile lay is unsuccessful
            hasLaidExtraOBBTile.set(false);
        }

        return result;
    }

    @Override
    protected void newPhaseChecks() {
        Phase phase = Phase.getCurrent(this);
        if (phase.getId().equals("4")
                || phase.getId().equals("4+4")
                && !companyManager.getPublicCompany(GameManager_1835.PR_ID).hasStarted()
                || phase.getId().equals("5")
                && !PrussianFormationRound.prussianIsComplete(gameManager)) {
            if (getStep() == GameDef.OrStep.DISCARD_TRAINS) {
                // Postpone until trains are discarded
                needPrussianFormationCall.set(true);
            } else {
                // Do it immediately
                ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            }
        }
    }

    @Override
    public boolean discardTrain(DiscardTrain action) {

        boolean result = super.discardTrain(action);
        if (result && getStep() == GameDef.OrStep.BUY_TRAIN
                && needPrussianFormationCall.value()) {
            // Do the postponed formation calls
            ((GameManager_1835)gameManager).startPrussianFormationRound (this);
            needPrussianFormationCall.set(false);
        }
        return result;
    }
}
