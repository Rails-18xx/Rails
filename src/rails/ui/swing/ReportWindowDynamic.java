package rails.ui.swing;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import rails.common.LocalText;
import rails.common.ReportBuffer;
import rails.game.action.GameAction;
import rails.game.action.PossibleActions;
import rails.game.state.Observable;
import rails.game.state.Observer;
import rails.sound.SoundManager;
import rails.ui.swing.elements.ActionButton;
import rails.ui.swing.elements.RailsIcon;

/**
 * Dynamic Report window that acts as linked game history
 */

public class ReportWindowDynamic extends AbstractReportWindow 
    implements Observer, ActionListener, HyperlinkListener {
    private static final long serialVersionUID = 1L;

    private final ReportBuffer buffer;
    
    private JLabel message;

    private JScrollPane reportPane;
    private JEditorPane editorPane;

    private JPanel buttonPanel;
    private ActionButton forwardButton;
    private ActionButton backwardButton;
    private JButton returnButton;
    private JButton playFromHereButton;
    private JButton commentButton;

    private boolean timeWarpMode;

    protected static Logger log =
        LoggerFactory.getLogger(ReportWindowDynamic.class);

    public ReportWindowDynamic(GameUIManager gameUIManager) {
        super(gameUIManager);
        init();
        // add as observer
        buffer = gameUIManager.getRoot().getReportManager().getReportBuffer(); 
        buffer.addObserver(this);
    }

    @Override
    public void init() {
        setLayout(new BorderLayout());

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());

        message = new JLabel();
        message.setText( LocalText.getText("REPORT_TIMEWARP_ACTIVE"));
        message.setHorizontalAlignment(JLabel.CENTER);
        messagePanel.add(message, "North");

        JPanel timeWarpButtons = new JPanel();
        returnButton = new JButton(LocalText.getText("REPORT_LEAVE_TIMEWARP"));
        returnButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        gotoLastIndex();
                    }
                }
        );
        timeWarpButtons.add(returnButton);

        playFromHereButton = new JButton(LocalText.getText("REPORT_PLAY_FROM_HERE"));
        playFromHereButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        deactivateTimeWarp();
                    }
                }
        );
        timeWarpButtons.add(playFromHereButton);
        messagePanel.add(timeWarpButtons, "South");
        add(messagePanel, "North");

        editorPane = new JEditorPane();
        editorPane.setEditable(false);
        editorPane.setContentType("text/html");
        editorPane.addHyperlinkListener(this);
        editorPane.setOpaque(false);
        editorPane.setBorder(null);

        // add a CSS rule to force body tags to use the default label font
        // instead of the value in javax.swing.text.html.default.csss
        Font font = UIManager.getFont("Label.font");
        String bodyRule = "body { font-family: " + font.getFamily() + "; " +
                "font-size: " + font.getSize() + "pt; }";
        ((HTMLDocument)editorPane.getDocument()).getStyleSheet().addRule(bodyRule);

        reportPane = new JScrollPane(editorPane);
        add(reportPane, "Center");

        buttonPanel = new JPanel();
        add(buttonPanel, "South");

        backwardButton = new ActionButton(RailsIcon.REPORT_MOVE_BACKWARD);
        backwardButton.addActionListener(this);
        buttonPanel.add(backwardButton);

        forwardButton = new ActionButton(RailsIcon.REPORT_MOVE_FORWARD);
        forwardButton.addActionListener(this);
        buttonPanel.add(forwardButton);


        // FIXME (Rails2.0): Add Comment has to be added again
        //commentButton = new JButton(LocalText.getText("REPORT_COMMENT"));
        // buttonPanel.add(commentButton);

        super.init();

    }

    // FIXME (Rails2.0): Replace this by toTe
    @Override
    public void updateLog() {
        updateLogInternal(buffer.toText());
    }
        
    private void updateLogInternal(String text) {
        // set the content of the pane to the current
        editorPane.setText(text);
        scrollDown();

        forwardButton.setEnabled(false);
        backwardButton.setEnabled(false);

        boolean haveRedo = false;
        List<GameAction> gameActions = PossibleActions.getInstance().getType(GameAction.class);
        boolean undoFlag = false;
        for (GameAction action:gameActions) {
            switch (action.getMode()) {
            case GameAction.UNDO:
                undoFlag = true;
                backwardButton.setPossibleAction(action);
                backwardButton.setEnabled(true);
                break;
            case GameAction.FORCED_UNDO:
                if (undoFlag) break; // only activate forced undo, if no other undo available
                backwardButton.setPossibleAction(action);
                backwardButton.setEnabled(true);
                break;
            case GameAction.REDO:
                forwardButton.setPossibleAction(action);
                forwardButton.setEnabled(true);
                haveRedo = true;
                break;
            }
        }
        if (!haveRedo) deactivateTimeWarp();
    }

    @Override
    public void scrollDown() {
        // only set caret if visible
        //if (!this.isVisible()) return;

        // find the active message in the parsed html code (not identical to the position in the html string)
        // thus the message indicator is used
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                int caretPosition;
                try{
                    String docText = editorPane.getDocument().getText(0, editorPane.getDocument().getLength());
                    caretPosition = docText.indexOf(ReportBuffer.ACTIVE_MESSAGE_INDICATOR);
                } catch (BadLocationException e){
                    caretPosition = -1;
                };
                final int caretPositionStore = caretPosition;
                if (caretPosition != -1) {
                    editorPane.setCaretPosition(caretPositionStore);
                }
            }
        });
    }

    public void actionPerformed(ActionEvent e) {
        ActionButton button = (ActionButton)e.getSource();
        GameAction action = (GameAction)button.getPossibleActions().get(0);
        if (action instanceof GameAction && (action.getMode() == GameAction.FORCED_UNDO)) {
            activateTimeWarp();
        }

        gameUIManager.processAction(action);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            activateTimeWarp();
            URL url = e.getURL();
//          String protocol = e.getURL().getProtocol();
            int index = url.getPort();
            gotoIndex(index + 1);
            toFront();
        }
    }

    private void gotoLastIndex() {
        // FIXME: This has to be rewritten
       //gotoIndex(gameUIManager.getGameManager().getChangeStack().size());
    }

    // TODO: Rewrite this procedure
    private void gotoIndex(int index) {
        // ChangeStack stack = gameUIManager.getGameManager().getChangeStack();
        // FIXME: This has to be rewritten
        // int currentIndex = stack.getIndex();
        int currentIndex = 0;
        if (index > currentIndex) { // move forward
            GameAction action = new GameAction(GameAction.REDO);
            action.setmoveStackIndex(index);
            gameUIManager.processAction(action);
        } else if (index < currentIndex) { // move backward
            GameAction action = new GameAction(GameAction.FORCED_UNDO);
            action.setmoveStackIndex(index);
            gameUIManager.processAction(action);
        }
    }

    private void activateTimeWarp() {
        if (!timeWarpMode) {
            message.setVisible(true);
            playFromHereButton.setVisible(true);
            returnButton.setVisible(true);
            gameUIManager.setEnabledAllWindows(false, this);
            timeWarpMode = true;
            closeable = false;
            SoundManager.notifyOfTimeWarp(timeWarpMode);
        }
    }

    private void deactivateTimeWarp() {
        gameUIManager.setEnabledAllWindows(true, this);
        message.setVisible(false);
        playFromHereButton.setVisible(false);
        returnButton.setVisible(false);
        timeWarpMode = false;
        SoundManager.notifyOfTimeWarp(timeWarpMode);
        closeable = true;
    }

    // Observer methods
    public void update(String text) {
        updateLogInternal(text);
    }

    public Observable getObservable() {
        return buffer;
    }
}
