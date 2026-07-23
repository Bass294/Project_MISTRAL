package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.RepLevel;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Mistral_PersonRepIsAtWorst <person id> <RepLevel>
 *
 * Same idea as the vanilla RepIsAtWorst command, except it checks the player's individual
 * standing with a specific person (person.getRelToPlayer()) instead of the player's faction
 * -wide reputation. Vanilla's own RepIsAtWorst only ever resolves against a factionId.
 */
public class Mistral_PersonRepIsAtWorst extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (params.size() < 2) return false;

		String personId = params.get(0).getString(memoryMap);
		String repLevelStr = params.get(1).getString(memoryMap);

		PersonDataAPI data = Global.getSector().getImportantPeople().getData(personId);
		if (data == null) return false;

		PersonAPI person = data.getPerson();
		if (person == null) return false;

		RepLevel repLevel = RepLevel.valueOf(repLevelStr);
		return person.getRelToPlayer().isAtWorst(repLevel);
	}

}
