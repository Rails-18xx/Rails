package net.sf.rails.game.specific._18VA;

import java.util.Map;

/**
 * Externalised constants for 18VA
 */

public class GameDef_18VA {

    public final static String GOODS = "GOODS";
    public final static String BO = "B&O";

    // Phases
    public final static String PHASE_5 = "5";

    // Port cities
    // Note: this only works because all involved hexes have only one stop
    public final static Map<String, String> citiesWithPorts = Map.of (
            "C8","D9",
            "E8","F9",
            "M8","N9",
            "O8","P9"
    );



}
