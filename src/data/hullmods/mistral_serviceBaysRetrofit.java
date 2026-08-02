package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;

import java.awt.Color;

public class mistral_serviceBaysRetrofit extends BaseHullMod {

    private static final String DA_MANUFACTURER = "Diable Avionics";
    private static final String SERVICE_BAYS_ID = "armaa_serviceBays";
    private static final String SPARE_CHASSIS_STORAGE_ID = "armaa_spare_chassis_storage";
    private static final String UNIVERSAL_DECKS_EXTRA_ID = "diableavionics_universaldecksExtra";

    private static final Color YELLOW = new Color(241, 199, 0);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (!ship.getVariant().hasHullMod(SERVICE_BAYS_ID)) {
            ship.getVariant().addPermaMod(SERVICE_BAYS_ID);
        }

        if (ship.getVariant().hasHullMod(UNIVERSAL_DECKS_EXTRA_ID)
                && !ship.getVariant().hasHullMod(SPARE_CHASSIS_STORAGE_ID)) {
            ship.getVariant().addPermaMod(SPARE_CHASSIS_STORAGE_ID);
        }
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Effect", Alignment.MID, 10f);
        tooltip.addPara("%s Grants the Specialized Service Bays hullmod as a built-in mod, if not already present.",
                3f, YELLOW, "•");
        tooltip.addPara("%s If this ship also has the Wanzer Servicing Gantry hullmod, grants the Spare Chassis Storage hullmod as a built-in mod as well.",
                3f, YELLOW, "•");
    }

    @Override
    public boolean isApplicableToShip(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null || ship.getVariant() == null) return false;
        if (!DA_MANUFACTURER.equals(ship.getHullSpec().getManufacturer())) return false;
        if (ship.getHullSpec().getFighterBays() <= 0) return false;
        if (ship.getVariant().hasHullMod(SERVICE_BAYS_ID)) return false;
        return !ship.getVariant().hasHullMod(SPARE_CHASSIS_STORAGE_ID);
    }

    @Override
    public String getUnapplicableReason(ShipAPI ship) {
        if (ship == null || ship.getHullSpec() == null) {
            return "";
        }
        if (ship.getVariant() != null && ship.getVariant().hasHullMod(SERVICE_BAYS_ID)) {
            return "Already has the Specialized Service Bays hullmod.";
        }
        if (ship.getVariant() != null && ship.getVariant().hasHullMod(SPARE_CHASSIS_STORAGE_ID)) {
            return "Already has the Spare Chassis Storage hullmod.";
        }
        if (!DA_MANUFACTURER.equals(ship.getHullSpec().getManufacturer())) {
            return "Only installable on Diable Avionics hulls.";
        }
        return "Only installable on hulls with fighter bays.";
    }
}
