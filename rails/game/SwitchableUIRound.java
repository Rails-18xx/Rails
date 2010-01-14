package rails.game;

/** Abstract class for rounds that cannot be subclassed for one of the
 * other Round subclasses because UI is switchable: in some steps,
 * an SR-type UI and in other steps an OR-type UI should be displayed.
 * @author Erik Vos
 *
 */
public abstract class SwitchableUIRound extends Round {

    public SwitchableUIRound (GameManagerI gameManager) {
        super (gameManager);
    }

}
