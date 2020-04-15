package fr.keyser.wonderfull.world;

import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.keyser.fsm.InstanceId;

public class GameConfiguration {

	private final List<String> dictionaries;

	private final List<EmpireConfiguration> empires;

	@JsonCreator
	public GameConfiguration(@JsonProperty("dictionaries") List<String> dictionaries,
			@JsonProperty("empires") List<EmpireConfiguration> empires) {
		this.dictionaries = dictionaries;
		this.empires = empires;
	}

	public PlayerGameDescription asDescription(String user, boolean terminated) {
		String id = empires.stream().filter(e -> e.getUser().equals(user)).map(EmpireConfiguration::getExternalId)
				.findFirst().orElse(null);
		return new PlayerGameDescription(id, terminated, dictionaries, usersList());

	}

	public ActiveGameDescription asGameDescription(InstanceId id) {
		return new ActiveGameDescription(id, dictionaries, usersList());
	}

	public List<String> getDictionaries() {
		return dictionaries;
	}

	public List<EmpireConfiguration> getEmpires() {
		return empires;
	}

	@JsonIgnore
	public int getPlayerCount() {
		return empires.size();
	}

	@Override
	public String toString() {
		return String.format("[empires=%s, dictionaries=%s]", empires, dictionaries);
	}

	private List<String> usersList() {
		return empires.stream().map(EmpireConfiguration::getUser).collect(Collectors.toList());
	}

	public GameConfiguration withRandomUUID() {
		return new GameConfiguration(dictionaries,
				empires.stream().map(EmpireConfiguration::withRandomUUID).collect(Collectors.toList()));
	}

}
