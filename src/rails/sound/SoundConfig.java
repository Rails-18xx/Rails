package rails.sound;

import rails.common.parser.Config;

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
    public static final String KEY_BGM_Enabled = "sound.backgroundMusic";
    //TODO GameSetup
    public static final String KEY_BGM_GameSetup = "sound.backgroundMusic.gameSetup";
    //TODO StartRound
    public static final String KEY_BGM_StartRound = "sound.backgroundMusic.startRound";
    //TODO StockRound
    public static final String KEY_BGM_StockRound = "sound.backgroundMusic.stockRound";
    //TODO OperatingRound
    public static final String KEY_BGM_OperatingRound = "sound.backgroundMusic.operatingRound";
    //TODO EndOfGame Round
    public static final String KEY_BGM_EndOfGameRound = "sound.backgroundMusic.endOfGameRound";
    public static final String KEY_SFX_Enabled = "sound.sfx";
    //TODO Opening Bell
    public static final String KEY_SFX_SR_OpeningBell = "sound.sfx.sr.openingBell";
    //TODO New president
    public static final String KEY_SFX_SR_NewPresident = "sound.sfx.sr.newPresident";
    //TODO Buy Share Pres
    public static final String KEY_SFX_SR_BuyShare_President = "sound.sfx.sr.buyShare.president";
    //TODO Buy Share non Pres
    public static final String KEY_SFX_SR_BuyShare_NonPresident = "sound.sfx.sr.buyShare.nonPresident";
    //TODO Sell Share Pres
    public static final String KEY_SFX_SR_SellShare_President = "sound.sfx.sr.sellShare.president";
    //TODO Sell Share non Pres
    public static final String KEY_SFX_SR_SellShare_NonPresident = "sound.sfx.sr.sellShare.nonPresident";
    //TODO Company Floats
    public static final String KEY_SFX_SR_CompanyFloats = "sound.sfx.sr.companyFloats";
    public static final String KEY_SFX_OR_LayTile = "sound.sfx.or.layTile";
    public static final String KEY_SFX_OR_LayToken = "sound.sfx.or.layToken";
    //TODO Set Revenue
    public static final String KEY_SFX_OR_SetRevenue = "sound.sfx.or.setRevenue";
    public static final String KEY_SFX_OR_Decision_Payout = "sound.sfx.or.decision.payout";
    public static final String KEY_SFX_OR_Decision_Split = "sound.sfx.or.decision.split";
    public static final String KEY_SFX_OR_Decision_Withhold = "sound.sfx.or.decision.withhold";
    public static final String KEY_SFX_OR_BuyTrain = "sound.sfx.or.buyTrain";
    public static final String KEY_SFX_OR_BuyPrivate = "sound.sfx.or.buyPrivate";
    
    public static String get(String configKey) {
        return get(configKey,null);
    }
    public static String get(String configKey, String parameter) {
        String value = Config.get(configKey,"");
        if (parameter == null) return value;
        
        //processing parameterized config elements
        String resultValue = null;
        String[] assignments = value.split(",");
        for ( int i = 0 ; i < assignments.length ; i++ ) {
            String[] assignment = assignments[i].split("=");
            if (assignment.length == 1) {
                //default assignment (meaning, parameter-independent)
                //only to be considered if no value has been found yet
                if (resultValue == null) resultValue = assignment[0];
            }
            else if (assignment.length == 2) {
                //parameterized assignment
                //only to be considered if parameter is as specified
                if (parameter.equals(assignment[0])) resultValue = assignment[1];
            }
        }
        if (resultValue == null) resultValue = "";
        return resultValue;
    }
    public static boolean isMusicEnabled() {
        return isEnabled(KEY_BGM_Enabled);
    }
    public static boolean isSFXEnabled() {
        return isEnabled(KEY_SFX_Enabled);
    }
    private static boolean isEnabled(String key) {
        return "enabled".equals(get(key));
    }
}
