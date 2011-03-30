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

import org.apache.log4j.Logger;

import rails.game.ReportBuffer;
import rails.game.action.GameAction;
import rails.game.action.PossibleActions;
import rails.game.move.MoveStack;
import rails.ui.swing.elements.ActionButton;
import rails.util.LocalText;

/**
 * Dynamic Report window that acts as linked game history
 */

public class ReportWindowDynamic extends AbstractReportWindow implements  ActionListener, HyperlinkListener {
    private static final long serialVersionUID = 1L;

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
        Logger.getLogger(ReportWindowDynamic.class.getPackage().getName());

    public ReportWindowDynamic(GameUIManager gameUIManager) {
        super(gameUIManager);
        init();
    }

    @Override
    public void init() {
        super.init();

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

        backwardButton = new ActionButton(LocalText.getText("REPORT_MOVE_BACKWARD"));
        backwardButton.addActionListener(this);
        buttonPanel.add(backwardButton);

        forwardButton = new ActionButton(LocalText.getText("REPORT_MOVE_FORWARD"));
        forwardButton.addActionListener(this);
        buttonPanel.add(forwardButton);


        commentButton = new JButton(LocalText.getText("REPORT_COMMENT"));
        commentButton.addActionListener(
                new ActionListener() {
                    public void actionPerformed(ActionEvent arg0) {
                        String newComment = (String)JOptionPane.showInputDialog(
                                ReportWindowDynamic.this,
                                LocalText.getText("REPORT_COMMENT_ASK"),
                                LocalText.getText("REPORT_COMMENT_TITLE"),
                                JOptionPane.PLAIN_MESSAGE,
                                null,
                                null,
                                ReportBuffer.getComment()
                        );
                        if (newComment != null) {
                            ReportBuffer.addComment(newComment);
                            updateLog();
                            scrollDown();
                        }
                    }
                }
        );
        buttonPanel.add(commentButton);

    }

    @Override
    public void updateLog() {
        // set the content of the pane to the current
        editorPane.setText(ReportBuffer.getReportItems());
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
        if (!this.isVisible()) return;

        // find the active message in the parsed html code (not identical to the position in the html string)
        // thus the message indicator is used
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

    public void actionPerformed(ActionEvent e) {
        ActionButton button = (ActionButton)e.getSource();
        GameAction action = (GameAction)button.getPossibleActions().get(0);
        if (action instanceof GameAction && (action.getMode() == GameAction.FORCED_UNDO)) {
            if (!timeWarpMode) {
                activateTimeWarp();
            }
        }


        gameUIManager.processAction(action);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            URL url = e.getURL();
//            String protocol = e.getURL().getProtocol();
            int index = url.getPort();
            gotoIndex(index + 1);
            if (!timeWarpMode) {
                activateTimeWarp();
            }
        }
    }

    private void gotoLastIndex() {
       gotoIndex(gameUIManager.getGameManager().getMoveStack().size());
    }

    private void gotoIndex(int index) {
        MoveStack stack = gameUIManager.getGameManager().getMoveStack();
        int currentIndex = stack.getIndex();
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
        message.setVisible(true);
        playFromHereButton.setVisible(true);
        returnButton.setVisible(true);
        gameUIManager.setEnabledAllWindows(false, this);
        timeWarpMode = true;
        closeable = false;
    }

    private void deactivateTimeWarp() {
        gameUIManager.setEnabledAllWindows(true, this);
        message.setVisible(false);
        playFromHereButton.setVisible(false);
        returnButton.setVisible(false);
        timeWarpMode = false;
        closeable = true;
    }
}
