package net.sf.rails.game.specific._1826;

import java.util.ArrayList;
import java.util.List;

/**
 * Externalised constants for 1826
 */
public class GameDef_1826 {

    /* Companies */
    public static final String ETAT ="Etat";
    public static final String SNCF = "SNCF";
    public static final String BELG = "Belg";

    /* Train types, also phase names */
    public static final String E = "E";
    public static final String TGV = "TGV";
    public static final String _10H = "10H";

    /* Various */
    public static final int[] E_TRAIN_STOPS = new int[] {2,3,3,4};
    public static final List<String> BELG_HEXES
            = new ArrayList<>(List.of("B10", "B12", "B14", "C11", "C13", "C15", "D12", "D14", "D16"));

}
