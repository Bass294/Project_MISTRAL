package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.combat.listeners.AdvanceableListener;
import com.fs.starfarer.api.impl.campaign.ids.Stats;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import com.fs.starfarer.api.util.Misc;

/**
 * Zero-flux tracking (ZeroFluxTracker) ported from Diable Avionics' Advanced Avionics
 * (data.hullmods.DiableAvionicsUpgrade.AvionicsTracker) - flux generation is sampled once per
 * second; whenever the gain over that window stays under the threshold, zero-flux speed boost is
 * forced active (getZeroFluxMinimumFluxLevel modifyFlat 2f, i.e. always-on) until generation spikes
 * back above it. Simplified: no engine glow/trail visuals, just the flux-gen gating itself.
 */
public class mistral_angeloscarriermod extends BaseHullMod {

    private static final float ENGAGEMENT_RANGE_REDUCTION = 0.4f;
    private static final float FIGHTER_OP_COST_REDUCTION = 8f;
    private static final float ZERO_FLUX_GEN_THRESHOLD = 0.01f;
    private static final float FIGHTER_DAMAGE_TAKEN_MULT = 0.8f;
    private static final float FIGHTER_CREW_LOSS_MULT = 0.5f;

    @Override
    public void applyEffectsBeforeShipCreation(HullSize hullSize, MutableShipStatsAPI stats, String id) {
        stats.getFighterWingRange().modifyMult(id, 1f - ENGAGEMENT_RANGE_REDUCTION);

        // ALL_FIGHTER_COST_MOD alone doesn't reduce the OP cost shown/charged for fitting a wing -
        // getOpCost() reads the role-specific mod (BOMBER/FIGHTER/INTERCEPTOR/SUPPORT_COST_MOD)
        // instead. Set all five, matching the pattern used by SEEKER's plagueBearer/plagueCultist.
        stats.getDynamic().getMod(Stats.ALL_FIGHTER_COST_MOD).modifyFlat(id, -FIGHTER_OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.BOMBER_COST_MOD).modifyFlat(id, -FIGHTER_OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.FIGHTER_COST_MOD).modifyFlat(id, -FIGHTER_OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.INTERCEPTOR_COST_MOD).modifyFlat(id, -FIGHTER_OP_COST_REDUCTION);
        stats.getDynamic().getMod(Stats.SUPPORT_COST_MOD).modifyFlat(id, -FIGHTER_OP_COST_REDUCTION);

        stats.getDynamic().getStat(Stats.FIGHTER_CREW_LOSS_MULT).modifyMult(id, FIGHTER_CREW_LOSS_MULT);
    }

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        ship.addListener(new ZeroFluxTracker(ship, id));
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
        tooltip.addPara("%s Fighter engagement range is reduced by %s.",
                3f, Misc.getNegativeHighlightColor(), "•", Math.round(ENGAGEMENT_RANGE_REDUCTION * 100f) + "%");
        tooltip.addPara("%s Reduces the OP cost of fighter wings by %s.",
                3f, Misc.getPositiveHighlightColor(), "•", Math.round(FIGHTER_OP_COST_REDUCTION) + " OP");
        tooltip.addPara("%s Zero-flux speed boost stays active as long as flux generation stays under %s of max flux per second.",
                3f, Misc.getPositiveHighlightColor(), "•", Math.round(ZERO_FLUX_GEN_THRESHOLD * 100f) + "%");
        tooltip.addPara("%s Fighter damage taken is reduced by %s.",
                3f, Misc.getPositiveHighlightColor(), "•", Math.round((1f - FIGHTER_DAMAGE_TAKEN_MULT) * 100f) + "%");
        tooltip.addPara("%s Crew lost due to fighter losses in combat is reduced by %s.",
                3f, Misc.getPositiveHighlightColor(), "•", Math.round((1f - FIGHTER_CREW_LOSS_MULT) * 100f) + "%");
    }

    @Override
    public boolean affectsOPCosts() {
        return true;
    }

    private static class ZeroFluxTracker implements AdvanceableListener {
        private final ShipAPI ship;
        private final String id;
        private final IntervalUtil interval = new IntervalUtil(1f, 1f);
        private float lastFluxLevel = 0f;
        private boolean boostActive = false;

        public ZeroFluxTracker(ShipAPI ship, String id) {
            this.ship = ship;
            this.id = id;
        }

        @Override
        public void advance(float amount) {
            interval.advance(amount);
            if (interval.intervalElapsed()) {
                interval.setInterval(1f, 1f);

                float newFluxLevel = ship.getCurrFlux() / ship.getMaxFlux();
                if (boostActive) {
                    if (newFluxLevel - lastFluxLevel >= ZERO_FLUX_GEN_THRESHOLD) {
                        interval.setInterval(5f, 5f);
                        boostActive = false;
                        ship.getMutableStats().getZeroFluxMinimumFluxLevel().unmodify(id);
                    }
                } else {
                    if (newFluxLevel - lastFluxLevel < ZERO_FLUX_GEN_THRESHOLD) {
                        boostActive = true;
                        // set to two, meaning boost is always on
                        ship.getMutableStats().getZeroFluxMinimumFluxLevel().modifyFlat(id, 2f);
                    }
                }

                lastFluxLevel = newFluxLevel;
            }
        }
    }
}
