package data.campaign.rulecmd;

import java.util.List;
import java.util.Map;

import com.fs.starfarer.api.Global;
import com.fs.starfarer.api.campaign.InteractionDialogAPI;
import com.fs.starfarer.api.campaign.rules.MemoryAPI;
import com.fs.starfarer.api.characters.ImportantPeopleAPI.PersonDataAPI;
import com.fs.starfarer.api.characters.PersonAPI;
import com.fs.starfarer.api.impl.campaign.rulecmd.BaseCommandPlugin;
import com.fs.starfarer.api.util.Misc.Token;

/**
 * Mistral_PersonRepAtLeast <person id> <int 0-100>
 *
 * Checks the player's individual reputation with a specific person against a raw 0-100
 * threshold, via person.getRelToPlayer().getRepInt(). Mistral_PersonRepIsAtWorst (named
 * RepLevel tiers) can't distinguish two thresholds that fall in the same tier - e.g. 75 and
 * 100 rep are both COOPERATIVE, the topmost tier - so this exists for gates that need an
 * exact number instead.
 */
public class Mistral_PersonRepAtLeast extends BaseCommandPlugin {

	public boolean execute(String ruleId, InteractionDialogAPI dialog, List<Token> params, Map<String, MemoryAPI> memoryMap) {
		if (params.size() < 2) return false;

		String personId = params.get(0).getString(memoryMap);
		int threshold = (int) params.get(1).getFloat(memoryMap);

		PersonDataAPI data = Global.getSector().getImportantPeople().getData(personId);
		if (data == null) return false;

		PersonAPI person = data.getPerson();
		if (person == null) return false;

		return person.getRelToPlayer().getRepInt() >= threshold;
	}

}
