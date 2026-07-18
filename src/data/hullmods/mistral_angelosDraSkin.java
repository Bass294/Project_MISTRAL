package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

/**
 * Applies the "DRA" family magic paintjob to whichever hull this mod is present on, driven
 * purely by hull id + the hardcoded family below - not by the ML_paintjob-* variant tag,
 * since that tag does not reliably survive the AI fleet-composition/auto-fit process (see
 * MagicLib's own MagicPaintjobShinyAdder.kt, which documents the same issue).
 *
 * If a different paintjob is already applied to the ship (i.e. the player picked one manually
 * through the normal Magic Paintjob refit UI), this mod backs off and leaves it alone rather
 * than overwriting the player's choice.
 */
public class mistral_angelosDraSkin extends BaseHullMod {

    private static final String PAINTJOB_FAMILY = "DRA";

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        applyDraSkin(ship);
    }

    @Override
    public void advanceInCombat(ShipAPI ship, float amount) {
        applyDraSkin(ship);
    }

    private void applyDraSkin(ShipAPI ship) {
        if (ship == null || ship.getVariant() == null || ship.getHullSpec() == null) {
            return;
        }

        MagicPaintjobSpec draSkin = findDraSkinForHull(ship.getHullSpec().getBaseHullId());
        if (draSkin == null) {
            return;
        }

        MagicPaintjobSpec currentPaintjob = MagicPaintjobManager.getCurrentShipPaintjob(ship.getVariant());
        if (currentPaintjob != null && !currentPaintjob.getId().equals(draSkin.getId())) {
            // Something else (most likely the player, via the normal paintjob picker) already
            // set a different skin on this ship - respect that instead of overriding it.
            return;
        }

        MagicPaintjobManager.applyPaintjob(ship, draSkin);
    }

    private MagicPaintjobSpec findDraSkinForHull(String hullId) {
        for (MagicPaintjobSpec spec : MagicPaintjobManager.getPaintjobsForHull(hullId, false)) {
            if (PAINTJOB_FAMILY.equals(spec.getPaintjobFamily())) {
                return spec;
            }
        }
        return null;
    }
}
