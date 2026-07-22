package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.SectorEntityToken;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.intel.contacts.ContactIntel;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Mistral_AddPotentialContact <person id>
 *
 * Copy of the vanilla AddPotentialContact rule command, except it anchors the resulting
 * ContactIntel to the person's own home market (person.getMarket()) instead of
 * dialog.getInteractionTarget().getMarket() - i.e. wherever the bar event happened to fire.
 * Vanilla's version is meant for people met "in the field" with no fixed home, so it binds
 * the contact to the interaction location; that breaks persistent, already-placed NPCs like
 * Ramiel (home market set via addAngelosPersons in Mistral_ModPlugin) by relocating them to
 * whichever market the bar event fired at once the contact is developed.
 */
public class Mistral_AddPotentialContact extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {

		SectorEntityToken entity = dialog.getInteractionTarget();
		if (entity == null) return false;

		PersonAPI person = null;
		if (params.size() > 0) {
			String personId = params.get(0).getString(memoryMap);
			PersonDataAPI data = Global.getSector().getImportantPeople().getData(personId);
			if (data != null) {
				person = data.getPerson();
			}
		}

		if (person == null) {
			person = entity.getActivePerson();
		}
		if (person == null) return false;

		com.fs.starfarer.api.campaign.econ.MarketAPI homeMarket = person.getMarket();
		if (homeMarket == null) {
			homeMarket = entity.getMarket();
		}

		ContactIntel.addPotentialContact(1f, person, homeMarket, dialog.getTextPanel());
		return true;
	}

}
