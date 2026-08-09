package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BeamAPI;
import com.fs.starfarer.api.combat.BeamEffectPlugin;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEntityAPI;
import com.fs.starfarer.api.combat.DamageAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.combat.listeners.DamageDealtModifier;
import org.lwjgl.util.vector.Vector2f;

public class Mistral_fasanHardFluxEffect implements BeamEffectPlugin {

    private boolean listenerAdded = false;

    @Override
    public void advance(float amount, CombatEngineAPI engine, BeamAPI beam) {
        if (listenerAdded) {
            return;
        }
        listenerAdded = true;

        ShipAPI source = beam.getSource();
        if (source != null && !source.hasListenerOfClass(Mistral_fasanHardFluxListener.class)) {
            source.addListener(new Mistral_fasanHardFluxListener());
        }
    }

    public static class Mistral_fasanHardFluxListener implements DamageDealtModifier {

        private static final String WEAPON_ID = "diableavionics_mistral_fasan";
        private static final float HARD_FLUX_FRACTION = 0.5f;
        private static final String MOD_ID = "mistral_fasan_hard_flux";

        @Override
        public String modifyDamageDealt(Object param, CombatEntityAPI target, DamageAPI damage,
                                         Vector2f point, boolean shieldHit) {
            if (!shieldHit) return null;
            if (!(param instanceof BeamAPI)) return null;
            if (!(target instanceof ShipAPI)) return null;
            if (damage.isForceHardFlux()) return null;

            BeamAPI beam = (BeamAPI) param;
            WeaponAPI weapon = beam.getWeapon();
            if (weapon == null || !WEAPON_ID.equals(weapon.getId())) return null;

            float dps = damage.getDpsDuration();
            if (dps <= 0f) return null;

            float dam = damage.getDamage() * damage.getDpsDuration() * HARD_FLUX_FRACTION;
            Global.getCombatEngine().applyDamage(target, point, dam, damage.getType(), 0f, false, false, beam.getSource());

            damage.getModifier().modifyMult(MOD_ID, 1f - HARD_FLUX_FRACTION);

            return MOD_ID;
        }
    }
}
