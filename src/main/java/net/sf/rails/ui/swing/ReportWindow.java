package net.sf.rails.ui.swing;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.List;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.BadLocationException;
import javax.swing.text.html.HTMLDocument;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.rails.common.Config;
import net.sf.rails.common.LocalText;
import net.sf.rails.common.ReportBuffer;
import net.sf.rails.game.state.ChangeStack;
import net.sf.rails.sound.SoundManager;
import net.sf.rails.ui.swing.elements.ActionButton;
import net.sf.rails.ui.swing.elements.RailsIcon;
import rails.game.action.GameAction;


/**
 * ReportWindow displays the game history
 */
public class ReportWindow extends JFrame implements ActionListener, HyperlinkListener, ReportBuffer.Observer {

    private static final long serialVersionUID = 1L;

    private static final Logger log = LoggerFactory.getLogger(ReportWindow.class);

    private final GameUIManager gameUIManager;
    private final ChangeStack changeStack;

    private JLabel message;

    private JEditorPane editorPane;

    private ActionButton forwardButton;
    private ActionButton backwardButton;
    private JButton returnButton;
    private JButton playFromHereButton;
    private JButton commentButton;

    private boolean timeWarpMode;

    private final boolean isStatic;

    public ReportWindow(GameUIManager gameUIManager, boolean isStaticWindow) {
        this.gameUIManager = gameUIManager;
        this.isStatic = isStaticWindow;
        timeWarpMode = false;

        ReportBuffer reportBuffer = gameUIManager.getRoot().getReportManager().getReportBuffer();
        reportBuffer.addObserver(this);
        changeStack = gameUIManager.getRoot().getStateManager().getChangeStack();

        init();

        // set initial text
        editorPane.setText(reportBuffer.getCurrentText());
    }

    public void init() {
        setLayout(new BorderLayout());

        JPanel messagePanel = new JPanel();
        messagePanel.setLayout(new BorderLayout());

        message = new JLabel();
        message.setText( LocalText.getText("REPORT_TIMEWARP_ACTIVE"));
        message.setHorizontalAlignment(JLabel.CENTER);
        message.setVisible(false);
        messagePanel.add(message, "North");

        JPanel timeWarpButtons = new JPanel();
        returnButton = new JButton(LocalText.getText("REPORT_LEAVE_TIMEWARP"));
        returnButton.addActionListener(arg0 -> gotoLastIndex());
        returnButton.setVisible(false);
        timeWarpButtons.add(returnButton);

        playFromHereButton = new JButton(LocalText.getText("REPORT_PLAY_FROM_HERE"));
        playFromHereButton.addActionListener(arg0 -> deactivateTimeWarp());
        playFromHereButton.setVisible(false);
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

        JScrollPane reportPane = new JScrollPane(editorPane);
        add(reportPane, "Center");

        JPanel buttonPanel = new JPanel();
        add(buttonPanel, "South");

        backwardButton = new ActionButton(RailsIcon.REPORT_MOVE_BACKWARD);
        backwardButton.addActionListener(this);
        backwardButton.setEnabled(! isStatic);
        buttonPanel.add(backwardButton);

        forwardButton = new ActionButton(RailsIcon.REPORT_MOVE_FORWARD);
        forwardButton.addActionListener(this);
        forwardButton.setEnabled(! isStatic);
        buttonPanel.add(forwardButton);

        // TODO: Add new command button functionality
//        commentButton = new JButton(LocalText.getText("REPORT_COMMENT"));
//        commentButton.addActionListener(
//                new ActionListener() {
//                    public void actionPerformed(ActionEvent arg0) {
//                        String newComment = (String)JOptionPane.showInputDialog(
//                                this,
//                                LocalText.getText("REPORT_COMMENT_ASK"),
//                                LocalText.getText("REPORT_COMMENT_TITLE"),
//                                JOptionPane.PLAIN_MESSAGE,
//                                null,
//                                null,
//                                ReportBuffer.getComment()
//                        );
//                        if (newComment != null) {
//                            ReportBuffer.addComment(newComment);
//                            updateLog();
//                            scrollDown();
//                        }
//                    }
//                }
//        );
//        buttonPanel.add(commentButton);

        // remaining code from AbstractReportWindow

        setSize(400, 400);
        setLocation(600, 400);
        setTitle(LocalText.getText("GameReportTitle"));

        final JFrame frame = this;
        this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (timeWarpMode) return;
                gameUIManager.uncheckMenuItemBox(StatusWindow.REPORT_CMD);
                frame.dispose();
            }
        });
        final GameUIManager guiMgr = gameUIManager;
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentMoved(ComponentEvent e) {
                guiMgr.getWindowSettings().set(frame);
            }
            @Override
            public void componentResized(ComponentEvent e) {
                guiMgr.getWindowSettings().set(frame);
            }
        });

        gameUIManager.packAndApplySizing(this);

        gameUIManager.setMeVisible(this, "yes".equalsIgnoreCase(Config.get("report.window.open")));
    }

    // FIXME (Rails2.0): Replace this by toTe
    public void setActions() {
        forwardButton.setEnabled(false);
        backwardButton.setEnabled(false);

        if ( ! gameUIManager.isMyTurn() ) {
            // not our turn, we shouldn't enable undo/etc buttons
            return;
        }

        boolean haveRedo = false;
        boolean undoFlag = false;
        List<GameAction> gameActions = gameUIManager.getGameManager().getPossibleActions().getType(GameAction.class);
        for (GameAction action:gameActions) {
            switch (action.getMode()) {
            case UNDO:
                undoFlag = true;
                backwardButton.setPossibleAction(action);
                backwardButton.setEnabled(true);
                break;
            case FORCED_UNDO:
                if (undoFlag) break; // only activate forced undo, if no other undo available
                backwardButton.setPossibleAction(action);
                backwardButton.setEnabled(true);
                break;
            case REDO:
                forwardButton.setPossibleAction(action);
                forwardButton.setEnabled(true);
                haveRedo = true;
                break;
            default:
                break;
            }
        }
        if (!haveRedo) deactivateTimeWarp();
    }

    public void scrollDown() {
        // only set caret if visible
        //if (!this.isVisible()) return;

        // find the active message in the parsed html code (not identical to the position in the html string)
        // thus the message indicator is used
        SwingUtilities.invokeLater(() -> {
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
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        ActionButton button = (ActionButton)e.getSource();
        GameAction action = (GameAction)button.getPossibleActions().get(0);
        if ( action != null && (action.getMode() == GameAction.Mode.FORCED_UNDO)) {
            activateTimeWarp();
        }

        gameUIManager.processAction(action);
    }

    @Override
    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED && ! isStatic ) {
            activateTimeWarp();
            URL url = e.getURL();
            int index = url.getPort();
            gotoIndex(index);
            toFront();
        }
    }

    private void gotoLastIndex() {
        gotoIndex(changeStack.getMaximumIndex());
    }

    private void gotoIndex(int index) {
        int currentIndex = changeStack.getCurrentIndex();
        if (index > currentIndex) { // move forward
            GameAction action = new GameAction(gameUIManager.getRoot(), GameAction.Mode.REDO);
            action.setmoveStackIndex(index);
            gameUIManager.processAction(action);
        } else if (index < currentIndex) { // move backward
            GameAction action = new GameAction(gameUIManager.getRoot(), GameAction.Mode.FORCED_UNDO);
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
    }

    // ReportBuffer.Observer methods

    // FIXME: Rails 2.0 Not used so far
    @Override
    public void append(String text) {
        // do nothing
    }

    @Override
    public void update(String text) {
        log.debug("Update dynamic report window");
        // set the content of the pane to the current
        editorPane.setText(text);
        scrollDown();
    }

}
