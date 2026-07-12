package data.shipsystems.scripts.ai;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.FluxTrackerAPI;
import com.fs.starfarer.api.combat.MutableShipStatsAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAIScript;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.ShipwideAIFlags;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

import java.util.List;

/**
 * Base pattern for the weapon-range calc (ignore decorative/PD, take 90% of the shortest
 * ranged offensive weapon): WanzerMovementScript.java (Diable Avionics).
 */
public class Mistral_driftAI implements ShipSystemAIScript {

    private static final float HARD_FLUX_THRESHOLD = 0.7f;
    private static final float NO_SHIELDS_FLAG_DURATION = 1.5f;

    private ShipAPI ship;
    private ShipSystemAPI system;
    private ShipwideAIFlags flags;
    private CombatEngineAPI engine;

    private float weaponRange = Float.MAX_VALUE;

    private final IntervalUtil tickInterval = new IntervalUtil(0.4f, 0.6f);

    @Override
    public void init(ShipAPI ship, ShipSystemAPI system, ShipwideAIFlags flags, CombatEngineAPI engine) {
        this.ship = ship;
        this.system = system;
        this.flags = flags;
        this.engine = engine;

        for (WeaponAPI weapon : ship.getAllWeapons()) {
            if (weapon.isDecorative() || weapon.hasAIHint(WeaponAPI.AIHints.PD)) continue;
            if (weapon.getRange() < weaponRange) {
                weaponRange = weapon.getRange() * 0.9f;
            }
        }
    }

    @Override
    public void advance(float amount, Vector2f missileDangerDir, Vector2f collisionDangerDir, ShipAPI target) {
        if (engine.isPaused() || system.isActive()) return;

        tickInterval.advance(amount);
        if (!tickInterval.intervalElapsed()) return;

        if (!AIUtils.canUseSystemThisFrame(ship)) return;

        List<ShipAPI> enemiesInRange = AIUtils.getNearbyEnemies(ship, weaponRange);
        boolean noShipsInRange = enemiesInRange.isEmpty();
        boolean shipsInRange = !noShipsInRange;

        if (noShipsInRange
                && flags.hasFlag(ShipwideAIFlags.AIFlags.MOVEMENT_DEST)
                && system.getAmmo() >= system.getMaxAmmo()) {
            ship.useSystem();
            return;
        }

        //if (!shipsInRange) return;

        FluxTrackerAPI fluxTracker = ship.getFluxTracker();
        MutableShipStatsAPI stats = ship.getMutableStats();

        float softFlux = fluxTracker.getCurrFlux() - fluxTracker.getHardFlux();
        float fluxDissipation = stats.getFluxDissipation().getModifiedValue();
        //float timeMult = stats.getTimeMult().getModifiedValue();
        float timeMult = 2f;
        // "system duration" - drift has no separate active window (active/down are both 0 in
        // ship_systems.csv), so the buff is only actually present during the charge-up ramp
        //float systemDuration = system.getChargeUpDur();
        //float softFluxRegen = fluxDissipation * timeMult * systemDuration;
        float softFluxRegen = fluxDissipation * timeMult;
        if (softFlux > softFluxRegen) {
            ship.useSystem();
            return;
        }

        float hardFluxLevel = fluxTracker.getHardFlux() / fluxTracker.getMaxFlux();
        if (hardFluxLevel > HARD_FLUX_THRESHOLD) {
            // command the ship to drop shields and keep them down: force the immediate drop,
            // then refresh the flag every tick this branch fires so the ship's own AI doesn't
            // fight back and raise them again before the threat passes
            if (ship.getShield() != null && ship.getShield().isOn()) {
                ship.getShield().toggleOff();
            }
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS, NO_SHIELDS_FLAG_DURATION);

            ship.useSystem();

        } else if (system.getAmmo() >= system.getMaxAmmo()
                && fluxTracker.getHardFlux() > fluxDissipation * timeMult) {
            // charges are just sitting maxed out and hard flux is outpacing what dissipation
            // alone could clear in a second - use it now rather than let a full charge go idle
            if (ship.getShield() != null && ship.getShield().isOn()) {
                ship.getShield().toggleOff();
            }
            flags.setFlag(ShipwideAIFlags.AIFlags.DO_NOT_USE_SHIELDS, NO_SHIELDS_FLAG_DURATION);
            ship.useSystem();

        }
    }
}
