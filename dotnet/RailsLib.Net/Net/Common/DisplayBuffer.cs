using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Linq;


/**
 * DisplayBuffer stores messages of the current action.
 */

namespace GameLib.Net.Common
{
    public class DisplayBuffer : RailsModel
    {
        private static Logger<DisplayBuffer> log = new Logger<DisplayBuffer>();

        private readonly ListState<string> buffer;

        private readonly BooleanState autoDisplay;

    private DisplayBuffer(ReportManager parent, string id) : base(parent, id)
        {
            buffer = ListState<string>.Create(this, "buffer");
            autoDisplay = BooleanState.Create(this, "autoDisplay");
        }

        public static DisplayBuffer Create(ReportManager parent, string id)
        {
            return new DisplayBuffer(parent, id);
        }

        new public ReportManager Parent
        {
            get
            {
                return (ReportManager)base.Parent;
            }
        }

        /**
         * Add a message to DisplayBuffer
        */
        public void Add(string message)
        {
            Add(message, true);
        }

        /**
         * Add a message to DisplayBuffer
         */
        // TODO (Rails2.0): What is the purpose of autoDisplay
        public void Add(string message, bool autoDisplay)
        {
            this.autoDisplay.Set(autoDisplay);
            if (!string.IsNullOrEmpty(message))
            {
                buffer.Add(message);
                log.Debug("To display: " + message);
            }
        }

        /** Get the current message buffer, and clear it */
        // TODO: (Rails2.0): Refactor this a little bit (use Model facilities)
        public string[] Get()
        {
            if (buffer.Count > 0)
            {
                string[] message = buffer.ToArray();// .view().toArray(new String[0]);
                buffer.Clear();
                return message;
            }
            else
            {
                return null;
            }
        }

        public int Count
        {
            get
            {
                return buffer.Count;
            }
        }

        public bool AutoDisplay
        {
            get
            {
                return autoDisplay.Value;
            }
        }

        public void Clear()
        {
            buffer.Clear();
        }

        /**
         * Shortcut to add a message to DisplayBuffer
         */
        public static void Add(IRailsItem item, string message)
        {
            item.GetRoot.ReportManager.DisplayBuffer.Add(message, true);
        }

        /**
         * Shortcut to add a message to DisplayBuffer
         */
        public static void Add(IRailsItem item, string message, bool autoDisplay)
        {
            item.GetRoot.ReportManager.DisplayBuffer.Add(message, autoDisplay);
        }
    }
}
