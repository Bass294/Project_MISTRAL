package data.scripts;

import com.fs.starfarer.api.BaseModPlugin;
import com.fs.starfarer.api.PluginPick;
import com.fs.starfarer.api.campaign.*;
import com.fs.starfarer.api.combat.MissileAIPlugin;
import com.fs.starfarer.api.combat.MissileAPI;
import com.fs.starfarer.api.combat.ShipAPI;
import data.scripts.weapons.DA_ESP_kestrelmirv;
import com.fs.starfarer.api.Global;
import java.util.Random;
//import exerelin.campaign.SectorManager;
import data.scripts.world.MistralGen;

import com.fs.starfarer.api.impl.campaign.ids.Tags;
import com.fs.starfarer.api.campaign.econ.MarketAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.characters.FullName;

import org.magiclib.util.MagicCampaign; // <-- new package path


public class Mistral_ModPlugin extends BaseModPlugin {

    // Missile AI Content Retrieval
    @Override
    public PluginPick<MissileAIPlugin> pickMissileAI(MissileAPI missile, ShipAPI launchingShip) {
        switch (missile.getProjectileSpecId()) {
            case "diableavionics_esp_kestrelpod":
                return new PluginPick<MissileAIPlugin>(new DA_ESP_kestrelmirv(missile), CampaignPlugin.PickPriority.MOD_SPECIFIC);
            default:
        }
        return null;
    }
	//sector gen stuff
	    @Override
    public void onNewGame() {
        super.onNewGame();

        // The code below requires that Nexerelin is added as a library (not a dependency, it's only needed to compile the mod).
        boolean isNexerelinEnabled = Global.getSettings().getModManager().isModEnabled("nexerelin");

      //if (!isNexerelinEnabled || SectorManager.getManager().isCorvusMode()) {
                    new MistralGen().generate(Global.getSector());
       //}
    }
    @Override
    public void onNewGameAfterEconomyLoad() {
        MarketAPI Iskandar = Global.getSector().getEconomy().getMarket("Iskandar");
        if (Iskandar == null) return; // market doesn't exist, bail safely

        PersonAPI angelosleader = MagicCampaign.addCustomPerson(
                Iskandar,
                "Kazuki",
                "Azrael",
                "angelosleader1",
                FullName.Gender.MALE,
                "angelos",
                "factionLeader",
                "factionLeader",
                false,
                0,
                0
        );
        angelosleader.addTag(Tags.CONTACT_MILITARY);
        angelosleader.addTag(Tags.CONTACT_UNDERWORLD);
        angelosleader.setImportanceAndVoice(PersonImportance.VERY_HIGH, new Random());

        PersonAPI angelospilot = MagicCampaign.addCustomPerson(
                Iskandar,
                "Mizuki",
                "Raziel",
                "angelospilot1",
                FullName.Gender.FEMALE,
                "angelos",
                "angelospilot",
                "angelospilot",
                false,
                0,
                1
        );
        angelospilot.addTag(Tags.CONTACT_MILITARY);
        angelospilot.addTag(Tags.CONTACT_TRADE);
        angelospilot.setImportanceAndVoice(PersonImportance.HIGH, new Random());

        PersonAPI agnelosdamsel = MagicCampaign.addCustomPerson(
                Iskandar,
                "Atra",
                "Sariel",
                "angelosdamsel1",
                FullName.Gender.FEMALE,
                "angelos",
                "angelosdamsel",
                "angelosdamsel",
                false,
                0,
                2
        );


        PersonAPI angelosspy = MagicCampaign.addCustomPerson(
                Iskandar,
                "Izuki",
                "Harut",
                "angelosspy1",
                FullName.Gender.MALE,
                "angelos",
                "angelosspy",
                "angelosspy",
                false,
                0,
                3
        );

        //Iskandar.getCommDirectory().getEntryForPerson(angelosleader).setHidden(true);
        //Iskandar.getCommDirectory().getEntryForPerson(angelospilot).setHidden(true);
        //Iskandar.getCommDirectory().removePerson(agnelosdamsel);
        //Iskandar.getCommDirectory().removePerson(angelosspy);
        Iskandar.getCommDirectory().getEntryForPerson(agnelosdamsel).setHidden(true);
        Iskandar.getCommDirectory().getEntryForPerson(angelosspy).setHidden(true);
    }
}
