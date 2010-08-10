package rails.ui.swing;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.List;

import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.UIManager;
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

    private GameUIManager gameUIManager;
    
    private JScrollPane reportPane;
    private JEditorPane editorPane;
    
    private JPanel buttonPanel;
    private ActionButton forwardButton;
    private ActionButton backwardButton;
    
    protected static Logger log =
        Logger.getLogger(ReportWindowDynamic.class.getPackage().getName());

    public ReportWindowDynamic(GameUIManager gameUIManager) {
        super();
        this.gameUIManager = gameUIManager;
        init();
    }

    public void init() {
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
        
        super.init();
    }
    
    @Override
    public void updateLog() {
        // set the content of the pane to the current 
        editorPane.setText(ReportBuffer.getReportItems());
        scrollDown();
        
        forwardButton.setEnabled(false);
        backwardButton.setEnabled(true);
        List<GameAction> gameActions = PossibleActions.getInstance().getType(GameAction.class);
        for (GameAction action:gameActions) {
            switch (action.getMode()) {
            case GameAction.UNDO:
            case GameAction.FORCED_UNDO:
                backwardButton.setPossibleAction(action);
                backwardButton.setEnabled(true);
                break;
            case GameAction.REDO:
                forwardButton.setPossibleAction(action);
                forwardButton.setEnabled(true);
                break;
            }
        }
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
        gameUIManager.processOnServer(action);
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
        if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
            URL url = e.getURL();
//            String protocol = e.getURL().getProtocol();
            int index = url.getPort();
            gotoIndex(index + 1);
        }
    }

    private void gotoIndex(int index) {
        MoveStack stack = gameUIManager.getGameManager().getMoveStack();
        int currentIndex = stack.getIndex();
        if (index > currentIndex) { // move forward
            if (index != currentIndex +1) {
                stack.gotoIndex(index - 1);
            }
            GameAction action = new GameAction(GameAction.REDO);
            gameUIManager.processOnServer(action);
        } else if (index < currentIndex) { // move backward
            if (index != currentIndex - 1) {
                stack.gotoIndex(index + 1);
            }
            GameAction action = new GameAction(GameAction.FORCED_UNDO);
            gameUIManager.processOnServer(action);
        }
    }

}
