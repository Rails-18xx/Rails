using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

/**
 * Used to indicate if an player sold a share this round
 */

namespace GameLib.Net.Game.Model
{
    public class SoldThisRoundModel : ColorModel
    {
        public static readonly Color SOLD_COLOR = Color.Red;
        public const int SOLD_ALPHA = 64;

        private BooleanState state;

        private SoldThisRoundModel(IItem parent, string id) : base(parent, id)
        {
            state = BooleanState.Create(this, "state");
        }

        public static SoldThisRoundModel Create(Player parent, PublicCompany company)
        {
            return new SoldThisRoundModel(parent, "SoldThisRoundModel_" + company.Id);
        }

        public bool Value
        {
            get
            {
                return state.Value;
            }
        }

        public void Set(bool value)
        {
            state.Set(value);
        }

        override public Color Background
        {
            get
            {
                if (state.Value)
                {
                    return Color.FromArgb(SOLD_ALPHA, SOLD_COLOR); // (SOLD_COLOR.getRed(), SOLD_COLOR.getGreen(), SOLD_COLOR.getBlue(), SOLD_ALPHA);
                }
                else
                {
                    return Color.Empty;
                }
            }
        }

        override public Color Foreground
        {
            get
            {
                return Color.Empty;
            }
        }
    }
}
