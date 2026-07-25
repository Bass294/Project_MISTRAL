package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileRenderDataAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.Color;

/**
 * Keeps the Ame's two hidden missile slots (WS0006, WS0007) fully invisible - both the
 * stowed-missile render hanging off the hull and the mounted weapon's own base sprites
 * (turret/hardpoint, under, barrel, glow). Only the fired projectiles should be seen. Same
 * approach as Mistral_NightingaleMissileHide. Attached to mistral_raindrop_center, a
 * built-in decorative weapon on this hull that was already scriptless.
 */
public class Mistral_AmeMissileHide implements EveryFrameWeaponEffectPlugin {

    private static final String[] MISSILE_SLOT_IDS = {"WS0006", "WS0007"};
    private static final Color HIDDEN = new Color(255, 255, 255, 0);

    private ShipAPI ship;
    private WeaponAPI[] missiles;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (ship == null) {
            ship = weapon.getShip();
        }
        if (ship == null) return;

        if (missiles == null) {
            missiles = findMissiles();
        }

        for (WeaponAPI missile : missiles) {
            if (missile == null) continue;

            if (missile.getMissileRenderData() != null) {
                for (MissileRenderDataAPI render : missile.getMissileRenderData()) {
                    render.getSprite().setColor(HIDDEN);
                }
            }

            if (missile.getSprite() != null) {
                missile.getSprite().setSize(0, 0);
                missile.getSprite().setColor(HIDDEN);
            }
            if (missile.getUnderSpriteAPI() != null) {
                missile.getUnderSpriteAPI().setColor(HIDDEN);
            }
            if (missile.getBarrelSpriteAPI() != null) {
                missile.getBarrelSpriteAPI().setColor(HIDDEN);
            }
            if (missile.getGlowSpriteAPI() != null) {
                missile.getGlowSpriteAPI().setColor(HIDDEN);
            }
        }
    }

    private WeaponAPI[] findMissiles() {
        WeaponAPI[] found = new WeaponAPI[MISSILE_SLOT_IDS.length];
        for (WeaponAPI w : ship.getAllWeapons()) {
            for (int i = 0; i < MISSILE_SLOT_IDS.length; i++) {
                if (found[i] == null && w.getSlot().getId().equals(MISSILE_SLOT_IDS[i])) {
                    found[i] = w;
                }
            }
        }
        return found;
    }
}
