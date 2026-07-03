package data.scripts.weapons;

import com.fs.starfarer.api.AnimationAPI;
import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.CombatEngineAPI;
import com.fs.starfarer.api.combat.CombatEngineLayers;
import com.fs.starfarer.api.combat.EveryFrameWeaponEffectPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipSystemAPI;
import com.fs.starfarer.api.combat.WeaponAPI;
import com.fs.starfarer.api.util.IntervalUtil;
//import data.scripts.DAModPlugin;
import org.magiclib.util.MagicRender;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import org.lazywizard.lazylib.combat.AIUtils;
import org.lwjgl.util.vector.Vector2f;

public class Mistral_derechoEffect implements EveryFrameWeaponEffectPlugin {

    private boolean runOnce = false, animIsOn = true;
    private ShipAPI theShip;
    private ShipSystemAPI theSystem;
    private AnimationAPI theAnim;
    private float animation = 0, fade = 0, rim = 0, axis = 0;
    private final float ANIM = 0.05f, RIMTICK = 0.5f;
    private int LENGTH, frame = 0;
    private final IntervalUtil timer = new IntervalUtil(0.1f, 0.1f);
    private final String stripeSprite = "areaStripes";

    @Override
    public void advance(float amount, CombatEngineAPI engine, WeaponAPI weapon) {

        if (!runOnce) {
            runOnce = true;
            theShip = weapon.getShip();
            theSystem = theShip.getSystem();
            LENGTH = weapon.getAnimation().getNumFrames() - 1;
            theAnim = weapon.getAnimation();
        }

        if (engine.isPaused()) {
            return;
        }

        if (theSystem.isOn()) {
            if (!animIsOn) {
                engine.addSmoothParticle(weapon.getLocation(), weapon.getShip().getVelocity(), 3000, 1f, 0.5f, new Color(0.1f, 0f, 0.15f));
                animIsOn = true;
            }

            timer.advance(amount);

            // Here used to be the derecho`s missile intercept code
            // but it had moved into the quantumimpulseStats under shipssystem file,for a better code organization.

        } else {
            animIsOn = false;
        }

        if (animIsOn || fade > 0) {
            animation += amount;
            if (animation > ANIM) {
                animation -= ANIM;

                frame++;
                if (frame > LENGTH) {
                    frame = 1;
                }
                theAnim.setFrame(frame);

                if (animIsOn) {
                    fade = Math.min(fade + 0.02f, 1);
                } else {
                    fade = Math.max(fade - 0.02f, 0);
                }
                theAnim.setAlphaMult(fade);
            }
        }
    }
}
    
