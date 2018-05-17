using GameLib.Net.Game;
using System;
using System.Collections.Generic;


/**
 * ReportManager controls the (non-action) communication with the client. 
 * 
 * Specific task:
 * Handling of the ReportBuffer and the DisplayBuffer
 */

namespace GameLib.Net.Common
{
    public class ReportManager : RailsManager
    {
        private readonly DisplayBuffer displayBuffer;
        private readonly ReportBuffer reportBuffer;


    private ReportManager(RailsRoot parent, string id) : base(parent, id)
        {
            displayBuffer = DisplayBuffer.Create(this, "displayBuffer");
            reportBuffer = ReportBuffer.Create(this, "reportBuffer");

            parent.StateManager.ChangeStack.AddChangeReporter(reportBuffer);
        }

        public static ReportManager Create(RailsRoot parent, string id)
        {
            return new ReportManager(parent, id);
        }

        public DisplayBuffer DisplayBuffer
        {
            get
            {
                return displayBuffer;
            }
        }

        public ReportBuffer ReportBuffer
        {
            get
            {
                return reportBuffer;
            }
        }
    }
}
