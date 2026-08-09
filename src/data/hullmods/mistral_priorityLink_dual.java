package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.loading.FighterWingSpecAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.scripts.Mistral_priorityLinkSubsystem;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import static data.scripts.util.Mistral_stringsManager.txt;

public class mistral_priorityLink_dual extends BaseHullMod {

    private static final Color YELLOW = new Color(241, 199, 0);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (hasSubsystem(ship)) {
            return;
        }
        MagicSubsystemsManager.addSubsystemToShip(ship, new Mistral_priorityLinkSubsystem(ship, 2));
    }

    private boolean hasSubsystem(ShipAPI ship) {
        List<MagicSubsystem> subsystems = MagicSubsystemsManager.getSubsystemsForShipCopy(ship);
        if (subsystems == null) {
            return false;
        }
        for (MagicSubsystem subsystem : subsystems) {
            if (subsystem instanceof Mistral_priorityLinkSubsystem) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading(txt("hm_priorityLink_tabSubsystem"), Alignment.MID, 10);
        tooltip.addPara(txt("hm_priorityLink_bullet1"), 3, YELLOW, "•", "30 seconds");
        tooltip.addPara(txt("hm_priorityLink_bullet2_dual"), 3, YELLOW, "•", "frigates", "2000su");
        tooltip.addPara(txt("hm_priorityLink_bullet3"), 3, YELLOW, "•", "single-fighter");
        tooltip.addPara(txt("hm_priorityLink_bullet4"), 3, YELLOW, "•");

        tooltip.addSectionHeading(txt("hm_priorityLink_tabBuff"), Alignment.MID, 10);
        tooltip.addPara(txt("hm_priorityLink_bullet5"), 3, Misc.getPositiveHighlightColor(), "•", "25%");
        tooltip.addPara(txt("hm_priorityLink_bullet6"), 3, Misc.getPositiveHighlightColor(), "•", "20%");

        tooltip.addSectionHeading(txt("hm_priorityLink_tabEligibleWings"), Alignment.MID, 10);
        if (ship != null && ship.getVariant() != null) {
            List<String> eligible = eligibleWings(ship);
            if (eligible.isEmpty()) {
                tooltip.addPara(txt("hm_priorityLink_noEligibleWings"), 3, Misc.getNegativeHighlightColor());
            } else {
                tooltip.setBulletedListMode("    - ");
                for (String w : eligible) {
                    tooltip.addPara(w, 3);
                }
                tooltip.setBulletedListMode(null);
            }
        }
    }

    // single-fighter wings (getNumFighters() == 1) - the same eligibility filter
    // Mistral_priorityLinkSubsystem.findCarrierFighters() uses for its own targeting
    private List<String> eligibleWings(ShipAPI ship) {
        List<String> result = new ArrayList<>();
        for (String w : ship.getVariant().getFittedWings()) {
            FighterWingSpecAPI spec = Global.getSettings().getFighterWingSpec(w);
            if (spec != null && spec.getNumFighters() == 1) {
                result.add(spec.getWingName() + " " + spec.getRoleDesc());
            }
        }
        return result;
    }
}
