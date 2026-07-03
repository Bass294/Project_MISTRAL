package data.shipsystems.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.impl.combat.BaseShipSystemScript;
import com.fs.starfarer.api.util.Misc;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.plugins.MagicTrailPlugin;

import java.awt.*;
import java.util.HashMap;
import java.util.Map;


public class Mistral_overLoadStats extends BaseShipSystemScript {
    public static final float MOBILE_BONUS_PERCENT = 50f;
    public static final float ROF_BONUS = 0.5f;
    public static final float FLUX_REDUCTION = 33f;
    private boolean system_used = false;
    private ShipAPI ship;
    private Map<ShipEngineControllerAPI.ShipEngineAPI, Float> trailIDMap = new HashMap<>();
    private SpriteAPI trailSprite = Global.getSettings().getSprite("fx", "beamRough2Core");
    private float trailid=0f;
    private float jitterTimer = 0f;
    public void apply(MutableShipStatsAPI stats, String id, State state, float effectLevel) {

        if (stats.getEntity() instanceof ShipAPI) {
            ship = (ShipAPI) stats.getEntity();
        } else {
            return;
        }

        float amount = Global.getCombatEngine().getElapsedInLastFrame();
        float systemDuration = ship.getSystem().getChargeActiveDur(); // total active duration in seconds


        if (state == State.IN || state == State.ACTIVE) {
            jitterTimer += amount;
        }

        /*else if (state == State.OUT) {
            //jitterTimer = 0f; // reset during fade-out phase
        }*/
        // Goes from 1.0 down to 0.0 over the system duration
        float jitterOpacity = Math.max(0f, 1f - (jitterTimer / systemDuration));

        ship.setJitter(ship,
                Color.RED,
                0.4f * effectLevel * jitterOpacity,
                3,
                (4 + 5f * effectLevel) * jitterOpacity,
                (7 + 10f * effectLevel) * jitterOpacity
        );



        system_used=true;

        float mult = 1f + ROF_BONUS * effectLevel;

        stats.getEnergyWeaponDamageMult().modifyMult(id, mult);
        stats.getBallisticRoFMult().modifyMult(id, mult);
        stats.getBallisticAmmoRegenMult().modifyMult(id, mult);
        stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - (FLUX_REDUCTION * 0.01f));


        stats.getTurnAcceleration().modifyPercent(id,MOBILE_BONUS_PERCENT);
        stats.getAcceleration().modifyPercent(id,MOBILE_BONUS_PERCENT);
        stats.getDeceleration().modifyPercent(id,MOBILE_BONUS_PERCENT);
        stats.getMaxSpeed().modifyPercent(id,MOBILE_BONUS_PERCENT);

        /*
    stats.getEnergyWeaponDamageMult().modifyMult(id, DMG_BUFF);
	stats.getBallisticRoFMult().modifyMult(id, ROF_BONUS);
    stats.getBallisticAmmoRegenMult().modifyMult(id, ROF_BONUS);
	stats.getBallisticWeaponFluxCostMod().modifyMult(id, 1f - FLUX_REDUCTION);
         */

    }
    public void unapply(MutableShipStatsAPI stats, String id) {
        stats.getEnergyWeaponDamageMult().unmodify(id);
        stats.getBallisticRoFMult().unmodify(id);
        stats.getBallisticWeaponFluxCostMod().unmodify(id);
        stats.getBallisticAmmoRegenMult().unmodify(id);
        stats.getMaxSpeed().unmodify(id);
        stats.getDeceleration().unmodify(id);
        stats.getAcceleration().unmodify(id);
        stats.getTurnAcceleration().unmodify(id);
        jitterTimer = 0f;
    }

    public StatusData getStatusData(int index, State state, float effectLevel) {
        float mult = 1f + ROF_BONUS * effectLevel;
        float bonusPercent = (int) ((mult - 1f) * 100f);
        if (index == 0) {
            return new StatusData("energy weapon damage +" + (int) bonusPercent + "%", false);
        }
        if (index == 1) {
            return new StatusData("Ballistic weapon rate of fire +" + (int) bonusPercent + "%", false);
        }
        if (index == 2) {
            return new StatusData("ballistic flux use -" + (int) FLUX_REDUCTION + "%", false);
        }
        if (index == 3) {
            return new StatusData("mobility +" + (int) MOBILE_BONUS_PERCENT + "%", false);
        }
        if (index == 4) {
            return new StatusData("+" + (int) MOBILE_BONUS_PERCENT + "% top speed", false);
        }
        return null;
    }
}
