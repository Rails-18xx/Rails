using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Correct
{
    abstract public class CorrectionManager : RailsAbstractItem, IEquatable<CorrectionManager>
    {
        private CorrectionType correctionType;
        private BooleanState active;


        protected static Logger<CorrectionManager> log = new Logger<CorrectionManager>();

    protected CorrectionManager(GameManager parent, CorrectionType ct) : base(parent, ct.Name)
        {
            active = BooleanState.Create(this, "active");
            correctionType = ct;
        }

        new public GameManager Parent
        {
            get
            {
                return (GameManager)base.Parent;
            }
        }

        public CorrectionType CorrectionType
        {
            get
            {
                return correctionType;
            }
        }

        public bool IsActive()
        {
            return active.Value;
        }

        virtual public List<CorrectionAction> CreateCorrections()
        {

            List<CorrectionAction> actions = new List<CorrectionAction>();
            actions.Add(new CorrectionModeAction(CorrectionType, IsActive()));

            return actions;
        }

        /** calls all executeAction */
        virtual public bool ExecuteCorrection(CorrectionAction action)
        {
            if (action is CorrectionModeAction)
            {
                return Execute((CorrectionModeAction)action);
            }
            else
            {
                log.Debug("This correction action is not registered.");
                return false;
            }
        }

        private bool Execute(CorrectionModeAction action)
        {


            if (!IsActive())
            {
                string text = LocalText.GetText("CorrectionModeActivate",
                        GetRoot.PlayerManager.CurrentPlayer.Id,
                        LocalText.GetText(CorrectionType.Name)
                );
                ReportBuffer.Add(this, text);
                DisplayBuffer.Add(this, text);
            }
            else
            {
                ReportBuffer.Add(this, LocalText.GetText("CorrectionModeDeactivate",
                        GetRoot.PlayerManager.CurrentPlayer.Id,
                        LocalText.GetText(CorrectionType.Name)
                ));
            }
            active.Set(!active.Value);

            return true;
        }

        /* dummy to capture the non-supported actions */
        virtual protected bool Execute(CorrectionAction action)
        {
            log.Debug("The chosen action is not implemented in the registered manager");
            return false;
        }

        public override int GetHashCode()
        {
            return correctionType.GetHashCode();
        }

        public bool Equals(CorrectionManager cm)
        {
            return (this.Parent == cm.Parent
                    && this.correctionType == cm.correctionType);
        }
    }
}
