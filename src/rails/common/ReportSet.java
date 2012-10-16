package rails.common;

import java.util.ArrayList;
import java.util.List;

import com.google.common.base.Objects;

import rails.game.state.ChangeSet;
import rails.util.Util;

/**
 * ReportSet contains all messages that reference one ChangeSet
 */

class ReportSet {

    /** Newline string
     * &#10; is the linefeed character to induce line feed on copy & paste
     */
    private static final String NEWLINE_STRING = "<br>&#10;";

    private final ChangeSet changeSet;
    private final List<String> messages = new ArrayList<String>();
    
    ReportSet(ChangeSet changeSet){
        this.changeSet = changeSet;
    }
    
    ChangeSet getChangeSet() {
        return changeSet;
    }
  
    void addMessage(String message) {
        messages.add(message);
    }
    
    List<String> getAsList() {
        return messages;
    }
    
    String getMessages(boolean html) {
        StringBuffer s = new StringBuffer();
        for (String message:messages) {
            if (html) {
                s.append(Util.convertToHtml(message)); 
            } else {
                s.append(message);
            }
        }
        return s.toString();
    }
    
    /**
     * converts messages to html string
     * @param activeMessage if true, adds indicator and highlighting for active message
     */
    String toHtml(boolean activeMessage) {
        if (messages.isEmpty()) {
            if (activeMessage) {
                return ("<span bgcolor=Yellow>" + ReportBuffer.ACTIVE_MESSAGE_INDICATOR + "</span>"
                        + NEWLINE_STRING);
            } else { 
                return null;
            }
        }

        StringBuffer s = new StringBuffer();
        boolean init = true;
        for (String message:messages) {
            message = Util.convertToHtml(message);
            if (init) {
                if (activeMessage) {
                    s.append("<span bgcolor=Yellow>" + ReportBuffer.ACTIVE_MESSAGE_INDICATOR) ;
                }
                s.append("<a href=http://rails:"  + changeSet + ">");
                s.append(message);
                s.append("</a>"); 
                if (activeMessage) {
                    s.append("</span>");
                }
                s.append(NEWLINE_STRING);
                init = false;
            } else {
                s.append(message + NEWLINE_STRING); // see above
            }
        }
        return s.toString();
    }
    
    public String toText() {
        StringBuffer s = new StringBuffer();
        for (String message:messages) {
            s.append(message + "\n");
        }
        return s.toString();
    }
    
    @Override
    public String toString() {
        return Objects.toStringHelper(this).addValue(changeSet).toString();
    }

}
