using GameLib.Net.Game;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;

/**
 * This class contains hints from the server (game engine) to the client (GUI)
 * about the preferred visibility of the various window types.
 * It is up to the GUI (and its user) to decide what to do with these hints,
 * but the current implementation should exactly follow these hints.
 * @author VosE
 *
 */
namespace GameLib.Net.Common
{
    // #serialize
    public sealed class GuiHints : RailsAbstractItem
    {
        public static long serialVersionUID = 1L;

        /** What round type is currently active in the engine? */
        private GenericState<Type/*IRoundFacade*/> currentRoundType;

        /** Which windows should be visible? */
        private List<VisibilityHint> visibilityHints;

        /** Which window type is active and should be on top? */
        private GenericState<GuiDef.Panel> activePanel;

        private GuiHints(IRailsItem parent, string id) : base(parent, id)
        {
            currentRoundType = GenericState<Type>.Create(this, "currentRoundType");
            activePanel = GenericState<GuiDef.Panel>.Create(this, "activePanel");
        }

        public static GuiHints Create(IRailsItem parent, string id)
        {
            return new GuiHints(parent, id);
        }

        public Type CurrentRoundType
        {
            get
            {
                return currentRoundType.Value;
            }
            set
            {
                currentRoundType.Set(value);
            }
        }

        public List<VisibilityHint> VisibilityHints
        {
            get
            {
                return visibilityHints;
            }
        }

        public void SetVisibilityHint(GuiDef.Panel type, bool visibility)
        {
            if (visibilityHints == null)
            {
                visibilityHints = new List<VisibilityHint>(4);
            }
            visibilityHints.Add(new VisibilityHint(type, visibility));
        }

        public void ClearVisibilityHints()
        {
            if (visibilityHints == null)
            {
                visibilityHints = new List<VisibilityHint>(4);
            }
            else
            {
                visibilityHints.Clear();
            }
        }

        public GuiDef.Panel ActivePanel
        {
            get
            {
                return activePanel.Value;
            }
            set
            {
                activePanel.Set(value);
            }
        }

        public class VisibilityHint
        {

            GuiDef.Panel type;
            bool visibility;

            public VisibilityHint(GuiDef.Panel type, bool visibility)
            {
                this.type = type;
                this.visibility = visibility;
            }

            public GuiDef.Panel PanelType
            {
                get
                {
                    return type;
                }
            }

            public bool Visibility
            {
                get
                {
                    return visibility;
                }
            }
        }
    }
}
