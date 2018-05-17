using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Game.State;
using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;

/**
 * ReportBuffer stores messages of the game progress.
 *
 * Also used for regression testing comparing the output of the report buffer.
 */

namespace GameLib.Net.Common
{
    public class ReportBuffer : RailsAbstractItem, IChangeReporter
    {
        private static Logger<ReportBuffer> log = new Logger<ReportBuffer>();
    
    /** Indicator string to find the active message position in the parsed html document */
    public static readonly string ACTIVE_MESSAGE_INDICATOR = "(**)";

        // static data
        private readonly Deque<ReportSet> pastReports = new Deque<ReportSet>();
        private readonly Deque<ReportSet> futureReports = new Deque<ReportSet>();

        // TODO: Remove waitQueue, see functions below
        private readonly Queue<string> waitQueue = new Queue<string>();

        private ChangeStack changeStack; // initialized via init()

        // dynamic data
        private ReportSet.Builder currentReportBuilder;
        private ReportBuffer.IObserver observer;


        private ReportBuffer(ReportManager parent, string id) : base(parent, id)
        {
            currentReportBuilder = ReportSet.GetBuilder();
        }

        public static ReportBuffer Create(ReportManager parent, string id)
        {
            ReportBuffer buffer = new ReportBuffer(parent, id);
            return buffer;
        }

        public void AddObserver(ReportBuffer.IObserver observer)
        {
            this.observer = observer;
        }

        public void RemoveObserver()
        {
            this.observer = null;
        }

        /**
         * Returns a list of all messages (of the past)
         * @return list of messages
         */
        public IReadOnlyCollection<string> GetAsList()
        {
            var list = new List<string>();
            foreach (ReportSet rs in pastReports)
            {
                list.AddRange(rs.GetAsList());
            }
            return new ReadOnlyCollection<string>(list);
        }

        private string GetAsHtml(ChangeSet currentChangeSet)
        {

            // FIXME (Rails2.0): Add comments back
            //     s.append("<span style='color:green;font-size:80%;font-style:italic;'>");

            StringBuilder s = new StringBuilder();
            s.Append("<html>");
            var reports = new List<ReportSet>(pastReports);
            reports.AddRange(futureReports);
            foreach (ReportSet rs in reports) //Iterables.concat(pastReports, futureReports))
            {
                string text = rs.GetAsHtml(currentChangeSet);
                if (text == null) continue;
                s.Append("<p>");
                if (text != null) s.Append(text);
                s.Append("</p>");
            }
            s.Append("</html>");

            return s.ToString();
        }

        /**
         * Returns all messages for the recent active player
         * @return full text
         */
        // FIXME (Rails2.0): Add implementation for this
        public string GetRecentPlayer()
        {
            return null;
        }

        public string GetCurrentText()
        {
            return GetAsHtml(changeStack.GetClosedChangeSet());
        }

        private void AddMessage(string message)
        {
            if (string.IsNullOrEmpty(message)) return;
            currentReportBuilder.AddMessage(message);
            log.Debug("ReportBuffer: " + message);
        }

        private void UpdateObserver()
        {
            if (observer != null)
            {
                observer.Update(GetCurrentText());
            }
        }

        // ChangeReport methods
        public void Init(ChangeStack changeStack)
        {
            this.changeStack = changeStack;
        }

    public void UpdateOnClose()
        {
            ChangeSet current = changeStack.GetClosedChangeSet();
            ReportSet currentSet = currentReportBuilder.Build(current);
            pastReports.AddToBack(currentSet);
            futureReports.Clear();

            // a new builder
            currentReportBuilder = ReportSet.GetBuilder();

            // update observer (ReportWindow)
            UpdateObserver();
        }

        public void InformOnUndo()
        {
            ReportSet undoSet = pastReports.RemoveFromBack();
            futureReports.AddToFront(undoSet);
        }

        public void InformOnRedo()
        {
            ReportSet redoSet = futureReports.RemoveFromFront();
            pastReports.AddToBack(redoSet);
        }

        public void UpdateAfterUndoRedo()
        {
            UpdateObserver();
        }

        /**
         * Shortcut to add a message to DisplayBuffer
         */
        public static void Add(IRailsItem item, string message)
        {
            item.GetRoot.ReportManager.ReportBuffer.AddMessage(message);
        }

        // FIXME: Rails 2.0 Is it possible to remove the only use case for 1856 escrow money?
        [Obsolete]
    public static void AddWaiting(IRailsItem item, string message)
        {
            item.GetRoot.ReportManager.ReportBuffer.waitQueue.Enqueue(message);
        }

        [Obsolete]
    public static void GetAllWaiting(IRailsItem item)
        {
            ReportBuffer reportBuffer = item.GetRoot.ReportManager.ReportBuffer;
            foreach (string message in reportBuffer.waitQueue)
            {
                reportBuffer.AddMessage(message);
            }
            reportBuffer.waitQueue.Clear();
        }

        public interface IObserver
        {

            void Append(string text);

            void Update(string newText);

        }
    }
}
