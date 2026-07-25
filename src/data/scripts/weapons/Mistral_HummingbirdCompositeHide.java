package data.scripts.weapons;

import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileRenderDataAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;

import java.awt.Color;

/**
 * Keeps whatever weapon is mounted in the hummingbird's hidden composite slot (WS0005) fully
 * invisible - both the loaded-missile render (if any) and the mounted weapon's own base
 * sprites (turret/hardpoint, under, barrel, glow). Same approach as
 * Mistral_NightingaleMissileHide, just watching a single composite mount instead of two
 * missile slots. Attached to the R shoulder weapon rather than the head, since every other
 * built-in weapon on this hull already has its own everyFrameEffect script.
 */
public class Mistral_HummingbirdCompositeHide implements EveryFrameWeaponEffectPlugin {

    private static final String COMPOSITE_SLOT_ID = "WS0005";
    private static final Color HIDDEN = new Color(255, 255, 255, 0);

    private ShipAPI ship;
    private WeaponAPI composite;
    private boolean searched = false;

    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {
        if (ship == null) {
            ship = weapon.getShip();
        }
        if (ship == null) return;

        if (!searched) {
            composite = findComposite();
            searched = true;
        }
        if (composite == null) return;

        if (composite.getMissileRenderData() != null) {
            for (MissileRenderDataAPI render : composite.getMissileRenderData()) {
                render.getSprite().setColor(HIDDEN);
            }
        }

        if (composite.getSprite() != null) {
            composite.getSprite().setSize(0, 0);
            composite.getSprite().setColor(HIDDEN);
        }
        if (composite.getUnderSpriteAPI() != null) {
            composite.getUnderSpriteAPI().setColor(HIDDEN);
        }
        if (composite.getBarrelSpriteAPI() != null) {
            composite.getBarrelSpriteAPI().setColor(HIDDEN);
        }
        if (composite.getGlowSpriteAPI() != null) {
            composite.getGlowSpriteAPI().setColor(HIDDEN);
        }
    }

    private WeaponAPI findComposite() {
        for (WeaponAPI w : ship.getAllWeapons()) {
            if (w.getSlot().getId().equals(COMPOSITE_SLOT_ID)) {
                return w;
            }
        }
        return null;
    }
}
