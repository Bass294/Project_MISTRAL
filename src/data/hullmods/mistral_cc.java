package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

public class mistral_cc extends BaseHullMod {

    private static final float FRIGATE_DAMAGE_TAKEN_MULT = 0.95f;
    private static final float DESTROYER_DAMAGE_TAKEN_MULT = 0.97f;
    private static final float FIGHTER_DAMAGE_TAKEN_MULT = 0.9f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        float mult;
        if (hullSize == HullSize.FRIGATE) {
            mult = FRIGATE_DAMAGE_TAKEN_MULT;
        } else if (hullSize == HullSize.DESTROYER) {
            mult = DESTROYER_DAMAGE_TAKEN_MULT;
        } else {
            return;
        }

        stats.getHullDamageTakenMult().modifyMult(id, mult);
        stats.getArmorDamageTakenMult().modifyMult(id, mult);
        stats.getShieldDamageTakenMult().modifyMult(id, mult);
        stats.getEmpDamageTakenMult().modifyMult(id, mult);
    }

    @Override
    public void applyEffectsToFighterSpawnedByShip(ShipAPI fighter, ShipAPI ship, String id) {
        MutableShipStatsAPI stats = fighter.getMutableStats();
        stats.getHullDamageTakenMult().modifyMult(id, FIGHTER_DAMAGE_TAKEN_MULT);
        stats.getArmorDamageTakenMult().modifyMult(id, FIGHTER_DAMAGE_TAKEN_MULT);
        stats.getShieldDamageTakenMult().modifyMult(id, FIGHTER_DAMAGE_TAKEN_MULT);
        stats.getEmpDamageTakenMult().modifyMult(id, FIGHTER_DAMAGE_TAKEN_MULT);
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Effect", Alignment.MID, 10f);
        tooltip.addPara("%s Reduces damage taken by frigates by %s and destroyers by %s.",
                3f, Misc.getPositiveHighlightColor(), "•",
                Math.round((1f - FRIGATE_DAMAGE_TAKEN_MULT) * 100f) + "%",
                Math.round((1f - DESTROYER_DAMAGE_TAKEN_MULT) * 100f) + "%");
        tooltip.addPara("%s Reduces damage taken by this ship's fighters by %s.",
                3f, Misc.getPositiveHighlightColor(), "•", Math.round((1f - FIGHTER_DAMAGE_TAKEN_MULT) * 100f) + "%");
    }
}
