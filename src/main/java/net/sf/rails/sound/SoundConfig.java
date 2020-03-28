package net.sf.rails.sound;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import net.sf.rails.common.Config;


/**
 * A wrapper to the standard config providing additional services for
 * - contextual config elements (e.g., phase-dependent OR music)
 * - default config elements (e.g., default OR music)
 *
 * In addition, config keys are available as constants.
 *
 * @author Frederick Weld
 *
 */
public class SoundConfig {
    public static final String KEY_BGM_ENABLED = "sound.backgroundMusic";
    public static final String KEY_BGM_GAME_SETUP = "sound.backgroundMusic.gameSetup";
    public static final String KEY_BGM_START_ROUND = "sound.backgroundMusic.startRound";
    public static final String KEY_BGM_STOCK_ROUND = "sound.backgroundMusic.stockRound";
    public static final String KEY_BGM_OPERATING_ROUND = "sound.backgroundMusic.operatingRound";
    public static final String KEY_BGM_END_OF_GAME_ROUND = "sound.backgroundMusic.endOfGameRound";
    public static final String KEY_SFX_ENABLED = "sound.sfx";
    public static final String KEY_SFX_GEN_GAME_OVER_PENDING = "sound.sfx.gen.gameOverPending";
    public static final String KEY_SFX_GEN_PASS = "sound.sfx.gen.pass";
    public static final String KEY_SFX_GEN_SELECT = "sound.sfx.gen.select";
    public static final String KEY_SFX_GEN_NEW_CURRENT_PLAYER = "sound.sfx.gen.newCurrentPlayer";
    public static final String KEY_SFX_STR_BID_START_ITEM = "sound.sfx.str.bidStartItem";
    public static final String KEY_SFX_STR_BUY_START_ITEM = "sound.sfx.str.buyStartItem";
    public static final String KEY_SFX_SR_OPENING_BELL = "sound.sfx.sr.openingBell";
    public static final String KEY_SFX_SR_NEW_PRESIDENT = "sound.sfx.sr.newPresident";
    public static final String KEY_SFX_SR_BUY_SHARE_PRESIDENT = "sound.sfx.sr.buyShare.president";
    public static final String KEY_SFX_SR_BUY_SHARE_NON_PRESIDENT = "sound.sfx.sr.buyShare.nonPresident";
    public static final String KEY_SFX_SR_SELL_SHARE_PRESIDENT = "sound.sfx.sr.sellShare.president";
    public static final String KEY_SFX_SR_SELL_SHARE_NON_PRESIDENT = "sound.sfx.sr.sellShare.nonPresident";
    public static final String KEY_SFX_SR_COMPANY_FLOATS = "sound.sfx.sr.companyFloats";
    public static final String KEY_SFX_OR_ROTATE_TILE = "sound.sfx.or.rotateTile";
    public static final String KEY_SFX_OR_LAY_TILE_TRACK = "sound.sfx.or.layTile.track";
    public static final String KEY_SFX_OR_LAY_TILE_CITY = "sound.sfx.or.layTile.city";
    public static final String KEY_SFX_OR_LAY_TILE_LAST_TILE_LAID = "sound.sfx.or.layTile.lastTileLaid";
    public static final String KEY_SFX_OR_LAY_TOKEN = "sound.sfx.or.layToken";
    public static final String KEY_SFX_OR_SET_REVENUE = "sound.sfx.or.setRevenue";
    public static final String KEY_SFX_OR_DECISION_PAYOUT = "sound.sfx.or.decision.payout";
    public static final String KEY_SFX_OR_DECISION_SPLIT = "sound.sfx.or.decision.split";
    public static final String KEY_SFX_OR_DECISION_WITHHOLD = "sound.sfx.or.decision.withhold";
    public static final String KEY_SFX_OR_BUY_TRAIN = "sound.sfx.or.buyTrain";
    public static final String KEY_SFX_OR_BUY_PRIVATE = "sound.sfx.or.buyPrivate";

    /**
     * list of sfx which are to be played immediately (without waiting for completion of
     * prior sfx threads)
     */
    protected static final Set<String> KEYS_SFX_IMMEDIATE_PLAYING = new HashSet<String>
        (Arrays.asList(KEY_SFX_GEN_PASS,
                KEY_SFX_GEN_SELECT,
                KEY_SFX_OR_ROTATE_TILE));

    //if set to true, sfx is reported not to be enabled irrespective of the configuration
    private static boolean isSFXDisabled = false;

    public static String get(String configKey) {
        return get(configKey,null);
    }
    public static String get(String configKey, String parameter) {
        String value = Config.get(configKey,"");
        if (parameter == null) return value;

        //processing parameterized config elements
        String resultValue = null;
        String[] assignments = value.split(",");
        for ( String s : assignments ) {
            String[] assignment = s.split("=");
            if ( assignment.length == 1 ) {
                //default assignment (meaning, parameter-independent)
                //only to be considered if no value has been found yet
                if ( resultValue == null ) resultValue = assignment[0];
            }
            else if ( assignment.length == 2 ) {
                //parameterized assignment
                //only to be considered if parameter is as specified
                if ( parameter.equals(assignment[0]) ) resultValue = assignment[1];
            }
        }
        if (resultValue == null) resultValue = "";
        return resultValue;
    }
    public static boolean isBGMEnabled() {
        return isEnabled(KEY_BGM_ENABLED);
    }
    public static boolean isSFXEnabled() {
        return isEnabled(KEY_SFX_ENABLED) && !isSFXDisabled;
    }
    private static boolean isEnabled(String key) {
        return "enabled".equals(get(key));
    }
    public static void setSFXDisabled(boolean timeWarpMode) {
        isSFXDisabled = timeWarpMode;
    }
}
