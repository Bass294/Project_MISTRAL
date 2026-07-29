package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

public class mistral_sunshowermounts extends BaseHullMod {

    protected static final float COST_REDUCTION_SMALL = 2f;
    protected static final float COST_REDUCTION_MEDIUM = 4f;
    protected static final float COST_REDUCTION_LARGE = 8f;
    protected static final float RECOIL_REDUCTION = -30f;

    private static final Color YELLOW = new Color(241, 199, 0);

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getDynamic().getMod(Stats.SMALL_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_SMALL);
        stats.getDynamic().getMod(Stats.MEDIUM_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_MEDIUM);
        stats.getDynamic().getMod(Stats.LARGE_BALLISTIC_MOD).modifyFlat(id, -COST_REDUCTION_LARGE);
        stats.getDynamic().getMod(Stats.SMALL_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_SMALL);
        stats.getDynamic().getMod(Stats.MEDIUM_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_MEDIUM);
        stats.getDynamic().getMod(Stats.LARGE_ENERGY_MOD).modifyFlat(id, -COST_REDUCTION_LARGE);

        stats.getRecoilPerShotMult().modifyPercent(id, RECOIL_REDUCTION);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Effect", Alignment.MID, 10f);
        tooltip.addPara("%s Reduces the cost of ballistic and energy weapons by %s based on size.",
                3f, YELLOW, "•", Math.round(COST_REDUCTION_SMALL) + "/" + Math.round(COST_REDUCTION_MEDIUM) + "/" + Math.round(COST_REDUCTION_LARGE) + " OP");
        tooltip.addPara("%s Reduces recoil by %s.",
                3f, YELLOW, "•", Math.round(-RECOIL_REDUCTION) + "%");
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    //Built-in only
    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        return false;
    }
}
