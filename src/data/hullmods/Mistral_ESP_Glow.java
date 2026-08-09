package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.*;
import com.fs.starfarer.api.graphics.SpriteAPI;
import com.fs.starfarer.api.util.Misc;

import data.scripts.util.DA_ESP_ColorData;

import org.lwjgl.opengl.GL11;
import org.lwjgl.util.vector.Vector2f;
import org.magiclib.util.MagicRender;

import java.util.Map;
import java.util.WeakHashMap;



public class Mistral_ESP_Glow extends BaseHullMod {

    // Phase Grazer Core (AC) (mistral_drift) has a 3-second chargeup, so tying the glow boost to
    // getEffectLevel() made it crawl up over that whole window - well behind the system's own
    // (instant) sound/visual cues. Instead this is a one-shot flash timed off the moment the system
    // starts being used (isOn() going false -> true), independent of the system's own chargeup
    // length or how long it stays on afterward.
    private static final float FLASH_RAMP_UP_DURATION = 0.02f;
    private static final float FLASH_RAMP_DOWN_DURATION = 0.1f;
    private static final Map<ShipAPI, Boolean> driftWasOn = new WeakHashMap<>();
    private static final Map<ShipAPI, Float> driftFlashElapsed = new WeakHashMap<>();

    public static String txt(String id) {
        return Global.getSettings().getString("hullmods", id);
    }


    public void advanceInCombat(ShipAPI ship, float amount) {
        String hullId = ship.getHullSpec().getHullId();

        //Sun shower ring
        if (hullId.equals("diableavionics_mistral_sunshower_DA")){
            if (ship.getSystem().isActive()){

                //claude nonsense
                float fluxLevel = ship.getFluxTracker().getFluxLevel();
                int minOpacity = 1;
                int maxOpacity = 10;
                int opacity = (int) (minOpacity + (maxOpacity - minOpacity) * Math.min(1f, fluxLevel / 0.5f));

                //int opacity = (int) Math.min(10, ship.getFluxTracker().getFluxLevel() / 5 * 100);
                SpriteAPI effect = Global.getSettings().getSprite("fx", "mistral_sunshower_DA_glow1");
                Vector2f size = new Vector2f(effect.getWidth(), effect.getHeight());


            /*
            boolean systemActive = ship.getSystem() != null && ship.getSystem().isActive();

            float maxOpacity = systemActive ? 6f : 10f;
            float fluxFactor = ship.getFluxTracker().getFluxLevel();
            //int opacity = (int) Math.min(10, ship.getFluxTracker().getFluxLevel()/5*100);
            int opacity = Math.round(Math.min(maxOpacity, fluxFactor * maxOpacity));

            SpriteAPI effect = Global.getSettings().getSprite("fx", "tai_yang_yu_fp_da");
            Vector2f size = new Vector2f(effect.getWidth(), effect.getHeight());
            */

                renderGlow(ship, effect, size, opacity);
            }
        }
        //Sunrain ring - always active (not gated on system state), flashes once when the system activates
        else if (hullId.equals("diableavionics_mistral_sunrain")){
            float fluxLevel = ship.getFluxTracker().getFluxLevel();
            int minOpacity = 0;
            int maxOpacity = 10;
            float opacity = minOpacity + (maxOpacity - minOpacity) * Math.min(1f, fluxLevel / 0.5f);

            boolean onNow = ship.getSystem().isOn();
            if (onNow && !Boolean.TRUE.equals(driftWasOn.get(ship))) {
                driftFlashElapsed.put(ship, 0f);
            }
            driftWasOn.put(ship, onNow);

            Float flashElapsed = driftFlashElapsed.get(ship);
            if (flashElapsed != null) {
                flashElapsed += amount;

                float flashDuration = FLASH_RAMP_UP_DURATION + FLASH_RAMP_DOWN_DURATION;
                if (flashElapsed >= flashDuration) {
                    driftFlashElapsed.remove(ship);
                } else {
                    driftFlashElapsed.put(ship, flashElapsed);

                    float flashRatio;
                    if (flashElapsed <= FLASH_RAMP_UP_DURATION) {
                        flashRatio = flashElapsed / FLASH_RAMP_UP_DURATION;
                    } else {
                        flashRatio = 1f - (flashElapsed - FLASH_RAMP_UP_DURATION) / FLASH_RAMP_DOWN_DURATION;
                    }
                    opacity += 10f * flashRatio;
                }
            }
            opacity = Math.min(opacity, maxOpacity);

            SpriteAPI effect = Global.getSettings().getSprite("fx", "mistral_sunshower_DA_glow1");
            Vector2f size = new Vector2f(effect.getWidth(), effect.getHeight());

            renderGlow(ship, effect, size, (int) opacity);
        }
    }

    private void renderGlow(ShipAPI ship, SpriteAPI effect, Vector2f size, int opacity) {
        MagicRender.objectspace(
                effect,
                ship,
                new Vector2f(),
                new Vector2f(),
                size,
                ship.getRenderOffset(),
                -180f,
                0f,
                true,
                Misc.setAlpha(DA_ESP_ColorData.DA_blue, opacity),
                4,
                0f,
                1f,
                1f,
                0f,
                0.3f,
                0.3f,
                0.4f,
                true,
                CombatEngineLayers.ABOVE_SHIPS_LAYER,
                GL11.GL_SRC_ALPHA, GL11.GL_ONE
        );
    }
}
