package net.sf.rails.common;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableList;
import net.sf.rails.game.state.ChangeSet;
import net.sf.rails.util.Util;


/**
 * ReportSet contains all messages that reference one ChangeSet
 */

class ReportSet {

    /**
     * Newline string
     * &#10; is the linefeed character to induce line feed on copy & paste
     */
    private static final String NEWLINE_STRING = "<br>&#10;";

    private final ChangeSet changeSet;
    private final ImmutableList<String> messages;
    private final String htmlText;
    private final String htmlTextActive;

    ReportSet(ChangeSet changeSet, ImmutableList<String> messages) {
        this.changeSet = changeSet;
        this.messages = messages;
        this.htmlText = toHtml(false);
        this.htmlTextActive = toHtml(true);
    }

    String getAsHtml(ChangeSet currentChangeSet) {
        if (currentChangeSet == changeSet) {
            return htmlTextActive;
        } else {
            return htmlText;
        }
    }

    ImmutableList<String> getAsList() {
        return messages;
    }

    /**
     * converts messages to html string
     *
     * @param activeMessage if true, adds indicator and highlighting for active message
     */
    private String toHtml(boolean activeMessage) {
        if (messages.isEmpty()) {
            if (activeMessage) {
                return ("<span bgcolor=Yellow>" + ReportBuffer.ACTIVE_MESSAGE_INDICATOR + "</span>"
                        + NEWLINE_STRING);
            } else {
                return null;
            }
        }

        StringBuilder s = new StringBuilder();
        boolean init = true;
        for (String message : messages) {
            message = Util.convertToHtml(message);
            if (init) {
                if (activeMessage) {
                    s.append("<span bgcolor=Yellow>" + ReportBuffer.ACTIVE_MESSAGE_INDICATOR);
                }
                s.append("<a href=http://rails:").append(changeSet.getIndex()).append(">");
                s.append(message);
                s.append("</a>");
                if (activeMessage) {
                    s.append("</span>");
                }
                s.append(NEWLINE_STRING);
                init = false;
            } else {
                s.append(message).append(NEWLINE_STRING); // see above
            }
        }
        return s.toString();
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .addValue(changeSet)
                .toString();
    }

    static Builder builder() {
        return new Builder();
    }

    static class Builder {

        private final ImmutableList.Builder<String> messageBuilder = ImmutableList.builder();

        private Builder() {
        }

        void addMessage(String message) {
            messageBuilder.add(message);
        }

        ReportSet build(ChangeSet changeSet) {
            return new ReportSet(changeSet, messageBuilder.build());
        }

    }

}
