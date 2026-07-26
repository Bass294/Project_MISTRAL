package data.hullmods;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.CampaignFleetAPI;
import com.fs.starfarer.api.combat.BaseHullMod;
import com.fs.starfarer.api.combat.ShipAPI;
import com.fs.starfarer.api.combat.ShipAPI.HullSize;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.ui.Alignment;
import com.fs.starfarer.api.ui.TooltipMakerAPI;
import com.fs.starfarer.api.util.Misc;

import java.awt.Color;

/**
 * Marker hullmod for Mistral_WeaponStorageScript - carries no combat/campaign effects of its
 * own. As long as any non-mothballed ship in the player fleet has this hullmod installed, the
 * script keeps every weapon stack in fleet cargo at 0 storage space instead of its normal
 * size-based cost. See Mistral_WeaponStorageScript for the actual mechanism (adapted from
 * Second-in-Command's CompactStorageScript, which does the same thing but scales space down
 * by weapon size rather than zeroing it, and gates on a skill instead of a hullmod).
 */
public class mistral_zeroWeaponStorage extends BaseHullMod {

    public static final String ID = "mistral_zeroWeaponStorage";
    private static final String SIC_MOD_ID = "second_in_command";
    private static final Color YELLOW = new Color(241, 199, 0);

    public static boolean isPresentInPlayerFleet() {
        CampaignFleetAPI fleet = Global.getSector().getPlayerFleet();
        if (fleet == null) return false;

        for (FleetMemberAPI member : fleet.getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            if (member.getVariant() != null && member.getVariant().hasHullMod(ID)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void addPostDescriptionSection(TooltipMakerAPI tooltip, HullSize hullSize, ShipAPI ship, float width, boolean isForModSpec) {
        tooltip.addSectionHeading("Effect", Alignment.MID, 10f);
        tooltip.addPara("%s Weapons in your fleet's storage no longer take up cargo space.", 3f, YELLOW, "•");

        if (Global.getSettings().getModManager().isModEnabled(SIC_MOD_ID)) {
            tooltip.addPara("Effect incompatible with second in command", Misc.getNegativeHighlightColor(), 10f);
        }
    }

}
