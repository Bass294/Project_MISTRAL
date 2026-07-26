package data.scripts;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.BaseCampaignEventListener;
import com.fs.starfarer.api.campaign.CargoAPI;
import com.fs.starfarer.api.campaign.FleetEncounterContextPlugin;
import com.fs.starfarer.api.campaign.comm.CommMessageAPI.MessageClickAction;
import com.fs.starfarer.api.combat.ShipVariantAPI;
import com.fs.starfarer.api.fleet.FleetMemberAPI;
import com.fs.starfarer.api.impl.campaign.DModManager;
import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.impl.campaign.intel.MessageIntel;
import com.fs.starfarer.api.impl.campaign.skills.FieldRepairsScript;
import com.fs.starfarer.api.loading.HullModSpecAPI;
import com.fs.starfarer.api.util.Misc;
import com.fs.starfarer.api.util.WeightedRandomPicker;
import data.hullmods.mistral_continuousRepairs;

import java.util.Map;

/**
 * Java port of the ship-repair half of Second-in-Command's ContinuousRepairsListener
 * (second_in_command.skills.starfaring.ContinuousRepairs.kt), gated on the
 * mistral_continuousRepairs hullmod being present in the player fleet instead of a skill being
 * active. Every REQUIRED_DP worth of enemy deployment points defeated in a battle the player
 * wins (capital ships counting double) removes one random d-mod from one random eligible ship
 * in the fleet - can trigger multiple times per battle, and the running total carries over
 * between battles via SectorAPI.getPersistentData().
 *
 * The d-mod pick/removal/notification logic mirrors vanilla's own FieldRepairsScript
 * (pickNext()/pickNextNew()) rather than SIC's version directly, since it already does the
 * same thing as the reference script but additionally excludes built-in mods from the pick.
 */
public class Mistral_ContinuousRepairsListener extends BaseCampaignEventListener {

    public static final int REQUIRED_DP = 240;
    private static final String DP_KEY = "$mistral_continuousRepairsDP";

    public Mistral_ContinuousRepairsListener() {
        super(false);
    }

    @Override
    public void reportEncounterLootGenerated(FleetEncounterContextPlugin plugin, CargoAPI loot) {
        if (plugin == null) return;
        if (!mistral_continuousRepairs.isPresentInPlayerFleet()) return;
        if (!plugin.getBattle().isPlayerSide(plugin.getBattle().getSideFor(plugin.getWinner()))) return;

        Map<String, Object> persistentData = Global.getSector().getPersistentData();
        float dp = persistentData.containsKey(DP_KEY) ? (Float) persistentData.get(DP_KEY) : 0f;

        for (FleetEncounterContextPlugin.FleetMemberData casualty : plugin.getLoserData().getOwnCasualties()) {
            float cost = casualty.getMember().getDeploymentPointsCost();
            dp += cost;
            if (casualty.getMember().isCapital()) dp += cost;
        }

        while (dp >= REQUIRED_DP) {
            dp -= REQUIRED_DP;
            repairRandomDMod();
        }

        persistentData.put(DP_KEY, dp);
    }

    private void repairRandomDMod() {
        WeightedRandomPicker<FleetMemberAPI> picker = new WeightedRandomPicker<FleetMemberAPI>();
        for (FleetMemberAPI member : Global.getSector().getPlayerFleet().getFleetData().getMembersListCopy()) {
            if (member.isMothballed()) continue;
            if (!member.isFrigate() && !member.isDestroyer()) continue;
            if (member.getVariant() == null || !member.getVariant().hasDMods()) continue;
            if (member.getVariant().hasHullMod("rugged")) continue;
            if (member.getHullSpec().hasTag(Tags.HULL_UNRESTORABLE)) continue;
            if (member.getVariant().hasTag(Tags.VARIANT_UNRESTORABLE)) continue;
            picker.add(member);
        }

        FleetMemberAPI pick = picker.pick();
        if (pick == null) return;

        ShipVariantAPI variant = pick.getVariant();
        WeightedRandomPicker<String> dmodPicker = new WeightedRandomPicker<String>();
        for (String id : variant.getHullMods()) {
            HullModSpecAPI spec = DModManager.getMod(id);
            if (spec == null || !spec.hasTag(Tags.HULLMOD_DMOD)) continue;
            if (variant.getHullSpec().getBuiltInMods().contains(id)) continue;
            dmodPicker.add(id);
        }

        String dmodPick = dmodPicker.pick();
        if (dmodPick == null) return;

        DModManager.removeDMod(variant, dmodPick);

        HullModSpecAPI spec = DModManager.getMod(dmodPick);
        MessageIntel intel = new MessageIntel(pick.getShipName() + " - repaired " + spec.getDisplayName(), Misc.getBasePlayerColor());
        intel.setIcon(Global.getSettings().getSpriteName("intel", "repairs_finished"));
        Global.getSector().getCampaignUI().addMessage(intel, MessageClickAction.REFIT_TAB, pick);

        if (DModManager.getNumNonBuiltInDMods(variant) <= 0) {
            FieldRepairsScript.restoreToNonDHull(variant);
        }
    }

}
