using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

/**
 * ReportSet contains all messages that reference one ChangeSet
 */

namespace GameLib.Net.Common
{
    public class ReportSet
    {
        /** Newline string
            * &#10; is the linefeed character to induce line feed on copy & paste
            */
        private static readonly string NEWLINE_STRING = "<br>&#10;";

        private readonly ChangeSet changeSet;
        private readonly IReadOnlyCollection<string> messages;
        private readonly string htmlText;
        private readonly string htmlTextActive;

        public ReportSet(ChangeSet changeSet, IReadOnlyCollection<string> messages)
        {
            this.changeSet = changeSet;
            this.messages = messages;
            this.htmlText = ToHtml(false);
            this.htmlTextActive = ToHtml(true);
        }

        public string GetAsHtml(ChangeSet currentChangeSet)
        {
            if (currentChangeSet == changeSet)
            {
                return htmlTextActive;
            }
            else
            {
                return htmlText;
            }
        }

        public IReadOnlyCollection<string> GetAsList()
        {
            return messages;
        }

        /**
         * converts messages to html string
         * @param activeMessage if true, adds indicator and highlighting for active message
         */
        private string ToHtml(bool activeMessage)
        {
            if (messages.Count == 0)
            {
                if (activeMessage)
                {
                    return ("<span bgcolor=Yellow>" + ReportBuffer.ACTIVE_MESSAGE_INDICATOR + "</span>"
                            + NEWLINE_STRING);
                }
                else
                {
                    return null;
                }
            }

            StringBuilder s = new StringBuilder();
            bool init = true;
            foreach (string message in messages)
            {
                string msg = Util.Util.ConvertToHtml(message);
                if (init)
                {
                    if (activeMessage)
                    {
                        s.Append("<span bgcolor=Yellow>" + ReportBuffer.ACTIVE_MESSAGE_INDICATOR);
                    }
                    s.Append("<a href=http://rails:" + changeSet.Index + ">");
                    s.Append(msg);
                    s.Append("</a>");
                    if (activeMessage)
                    {
                        s.Append("</span>");
                    }
                    s.Append(NEWLINE_STRING);
                    init = false;
                }
                else
                {
                    s.Append(message + NEWLINE_STRING); // see above
                }
            }
            return s.ToString();
        }

        override public string ToString()
        {
            return $"{{changeset}}{changeSet.ToString()}"; //RailsObjects.GetStringHelper(this).AddValue(changeSet).ToString();
        }

        public static Builder GetBuilder()
        {
            return new Builder();
        }

        public class Builder
        {

            private readonly List<string> messageBuilder = new List<string>();

            public Builder() { }

            public void AddMessage(string message)
            {
                messageBuilder.Add(message);
            }

            public ReportSet Build(ChangeSet changeSet)
            {
                return new ReportSet(changeSet, new ReadOnlyCollection<string>(messageBuilder));
            }

        }
    }
}
