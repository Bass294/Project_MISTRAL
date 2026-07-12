package data.hullmods;

import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;
import data.shipsystems.scripts.Mistral_priorityLinkSubsystem;
import org.magiclib.subsystems.MagicSubsystem;
import org.magiclib.subsystems.MagicSubsystemsManager;

import java.awt.Color;
import java.util.List;

import static data.scripts.util.Mistral_stringsManager.txt;

public class mistral_priorityLink extends BaseHullMod {

    private static final Color YELLOW = new Color(241, 199, 0);

    @Override
    public void applyEffectsAfterShipCreation(ShipAPI ship, String id) {
        if (hasSubsystem(ship)) {
            return;
        }
        MagicSubsystemsManager.addSubsystemToShip(ship, new Mistral_priorityLinkSubsystem(ship, 1));
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
        Mistral_priorityLinkSubsystem instance = Mistral_priorityLinkSubsystem.getAttachedInstance(ship);
        String keyText = instance != null ? instance.getKeyText() : MagicSubsystem.BLANK_KEY;

        tooltip.addSectionHeading(txt("hm_priorityLink_tabSubsystem"), Alignment.MID, 10);
        tooltip.addPara(txt("hm_priorityLink_bullet1"), 3, YELLOW, "•", keyText, "30 second cooldown");
        tooltip.addPara(txt("hm_priorityLink_bullet2_single"), 3, YELLOW, "•", "1500su");
        tooltip.addPara(txt("hm_priorityLink_bullet3"), 3, YELLOW, "•");
        tooltip.addPara(txt("hm_priorityLink_bullet4"), 3, YELLOW, "•");

        tooltip.addSectionHeading(txt("hm_priorityLink_tabBuff"), Alignment.MID, 10);
        tooltip.addPara(txt("hm_priorityLink_bullet5"), 3, Misc.getPositiveHighlightColor(), "•", "25%");
        tooltip.addPara(txt("hm_priorityLink_bullet6"), 3, Misc.getPositiveHighlightColor(), "•", "33%");
    }
}
