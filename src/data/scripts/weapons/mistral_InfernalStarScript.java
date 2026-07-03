package data.scripts.weapons;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.DamagingProjectileAPI;
import com.fs.starfarer.api.combat.OnFireEffectPlugin;
import com.fs.starfarer.api.combat.WeaponAPI;
import data.scripts.util.mistral_Diableavionics_graphicLibEffects;
import java.awt.Color;
import org.lazywizard.lazylib.MathUtils;
import org.lwjgl.util.vector.Vector2f;

//Author: Nia Tahl

public class mistral_InfernalStarScript implements OnFireEffectPlugin {

    @Override
    public void onFire(DamagingProjectileAPI projectile, WeaponAPI weapon, CombatEngineAPI engine) {
        // Splinters
        Vector2f loc = projectile.getLocation();
        Vector2f vel = weapon.getShip().getVelocity();
        int splinterCount = MathUtils.getRandomNumberInRange(4,6);
        for (int j = 0; j < splinterCount; j++) {
            Vector2f randomVel = MathUtils.getRandomPointOnCircumference(null, MathUtils.getRandomNumberInRange(20f, 60f));
            randomVel.x += vel.x;
            randomVel.y += vel.y;

            engine.spawnProjectile(projectile.getSource(), projectile.getWeapon(), weapon.getId() + "_dummy", loc, projectile.getFacing(), randomVel);
        }

        // Muzzle flash particles
        engine.addHitParticle(loc, vel, 25, 0.5f, 1f, Color.blue);
        engine.addHitParticle(loc, vel, 37, 1f, 0.3f, Color.red);
        engine.addSmoothParticle(loc, vel, 50, 2f, 0.15f, Color.white);
        engine.addSmoothParticle(loc, vel, 62, 2f, 0.1f, Color.white);

        /*
        engine.addHitParticle(loc, vel, 100, 0.5f, 1f, Color.blue);
        engine.addHitParticle(loc, vel, 150, 1f, 0.3f, Color.red);
        engine.addSmoothParticle(loc, vel, 200, 2f, 0.15f, Color.white);
        engine.addSmoothParticle(loc, vel, 250, 2f, 0.1f, Color.white);
*/

        // ShaderLib ripple distortion (if enabled)
        if (Global.getSettings().getModManager().isModEnabled("shaderLib")) {
            mistral_Diableavionics_graphicLibEffects.CustomRippleDistortion(
                    loc,
                    vel,
                    50,
                    2,
                    false,
                    0,
                    360,
                    0,
                    0.1f,
                    0.15f,
                    0.25f,
                    0.5f,
                    0f
            );
            // Visuals
            for (int i = 0; i<3; i++) {
                Vector2f vel2 = new Vector2f(weapon.getShip().getVelocity());
                Vector2f.add(vel2,new Vector2f(MathUtils.getRandomNumberInRange(-30f, 30f), MathUtils.getRandomNumberInRange(-30f, 30f)),vel2);
                engine.addNebulaParticle(projectile.getLocation(), vel2, MathUtils.getRandomNumberInRange(10f, 15f),
                        0.5f, 0f, 0.5f, MathUtils.getRandomNumberInRange(0.5f, 1.2f), new Color(140,120,120,100));
            }
        }
    }
}