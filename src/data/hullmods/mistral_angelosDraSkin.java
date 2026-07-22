package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import org.magiclib.paintjobs.MagicPaintjobHullMod;
import org.magiclib.paintjobs.MagicPaintjobManager;
import org.magiclib.paintjobs.MagicPaintjobSpec;

import java.util.ArrayList;
import java.util.List;

/**
 * Applies the "DRA" family magic paintjob to whichever hull this mod is present on, driven
 * purely by hull id + the hardcoded family below - not by the ML_paintjob-* variant tag,
 * since that tag does not reliably survive the AI fleet-composition/auto-fit process (see
 * MagicLib's own MagicPaintjobShinyAdder.kt, which documents the same issue).
 *
 * If a different paintjob is already applied to the ship (i.e. the player picked one manually
 * through the normal Magic Paintjob refit UI), this mod backs off and leaves it alone rather
 * than overwriting the player's choice.
 *
 * This mod also keeps the ML_paintjob-* tag on the variant in sync with whatever it just applied.
 * That tag is what MagicLib's own ML_skinSwap hullmod (MagicPaintjobHullMod) reads via
 * getCurrentShipPaintjob() - and ML_skinSwap's advanceInCombat() is what actually repaints a
 * carrier's *launched fighter wings* to match, something this mod does not do on its own. So
 * hulls using this mod should also carry ML_skinSwap as a built-in mod: this mod paints the hull
 * itself and keeps the tag fresh (immune to the tag-erasure issue since it's re-set every frame),
 * and ML_skinSwap piggybacks off that tag to cascade the paintjob to deployed fighters.
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
        refreshPaintjobTag(ship.getVariant(), draSkin);
    }

    // Keeps the ML_paintjob-* tag in sync with the skin we just applied, so MagicLib's own
    // ML_skinSwap hullmod (also built into this hull) can find it via getCurrentShipPaintjob()
    // and cascade the paintjob to this ship's launched fighter wings.
    private void refreshPaintjobTag(ShipVariantAPI variant, MagicPaintjobSpec draSkin) {
        String correctTag = MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX + draSkin.getId();
        if (variant.hasTag(correctTag)) {
            return;
        }

        List<String> staleTags = new ArrayList<>();
        for (String tag : variant.getTags()) {
            if (tag.startsWith(MagicPaintjobHullMod.PAINTJOB_TAG_PREFIX)) {
                staleTags.add(tag);
            }
        }
        for (String staleTag : staleTags) {
            variant.removeTag(staleTag);
        }

        variant.addTag(correctTag);
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
