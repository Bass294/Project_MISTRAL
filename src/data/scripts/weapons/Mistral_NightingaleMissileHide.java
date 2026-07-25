package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileRenderDataAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.Color;

/**
 * Keeps the nightingale's two hidden missile slots (WS0004, WS0005) fully invisible - both
 * the stowed-missile render hanging off the hull and the mounted weapon's own base sprites
 * (turret/hardpoint, under, barrel, glow). Only the fired projectiles should be seen. Same
 * technique as Arma Armatura's armaa_guarDualEffect2 (used to hide Zwei's wing missiles
 * mid-transform) for the missile render, and the same pattern used by the mod's own decorative
 * dummy weapons for hiding a weapon's base sprites - no centerX/centerY repositioning, since
 * this hull has no transform state to animate through and everything should just always be
 * hidden.
 */
public class Mistral_NightingaleMissileHide implements EveryFrameWeaponEffectPlugin {

    private static final String[] MISSILE_SLOT_IDS = {"WS0004", "WS0005"};
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
