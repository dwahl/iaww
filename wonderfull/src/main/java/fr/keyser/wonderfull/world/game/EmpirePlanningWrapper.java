package fr.keyser.wonderfull.world.game;

import java.util.List;
import java.util.function.Consumer;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.keyser.wonderfull.world.CurrentPlanning;
import fr.keyser.wonderfull.world.CurrentPlanning.DraftedAndPlanning;
import fr.keyser.wonderfull.world.DraftedCard;
import fr.keyser.wonderfull.world.Empire;
import fr.keyser.wonderfull.world.Tokens;
import fr.keyser.wonderfull.world.action.ActionAffectToProduction;
import fr.keyser.wonderfull.world.action.ActionMoveDraftedToProduction;
import fr.keyser.wonderfull.world.action.ActionRecycleDrafted;
import fr.keyser.wonderfull.world.action.ActionRecycleDraftedToProduction;
import fr.keyser.wonderfull.world.action.ActionRecycleProduction;
import fr.keyser.wonderfull.world.event.AffectProductionEvent;
import fr.keyser.wonderfull.world.event.EmpireEvent;
import fr.keyser.wonderfull.world.event.MoveToProductionEvent;
import fr.keyser.wonderfull.world.event.RecycleEvent;
import fr.keyser.wonderfull.world.event.RecycleInProductionEvent;

public class EmpirePlanningWrapper extends EmpireWrapper {

	private final CurrentPlanning planning;

	@JsonCreator
	public EmpirePlanningWrapper(@JsonProperty("empire") Empire empire,
			@JsonProperty("drafteds") List<DraftedCard> drafteds) {
		this(empire, new CurrentPlanning(drafteds));
	}

	private EmpirePlanningWrapper(Empire empire, CurrentPlanning planning) {
		super(empire);
		this.planning = planning;
	}

	/**
	 * Recyle a card in the production line
	 * 
	 * @param action
	 * @param consumer
	 * @return
	 */
	public EmpirePlanningWrapper recycleProduction(ActionRecycleProduction action, Consumer<EmpireEvent> consumer) {

		RecycleInProductionEvent event = empire.recycleProduction(action.getTargetId());

		consumer.accept(event);

		return new EmpirePlanningWrapper(empire.apply(event), planning);
	}

	/**
	 * Affect some currently produced resources to a card in production
	 * 
	 * @param consumer
	 * @param action
	 * @return
	 */
	public EmpirePlanningWrapper affectToProduction(ActionAffectToProduction action, Consumer<EmpireEvent> consumer) {

		Tokens consumed = action.getConsumed();
		Tokens available = empire.getOnEmpire();
		// check affectation
		Tokens remaining = available.subtract(consumed);
		if (remaining.entrySet().stream().anyMatch(e -> e.getValue() < 0))
			throw new IllegalAffectationException();

		AffectProductionEvent event = empire.affectToProduction(action.getTargetId(), action.getSlots());

		consumer.accept(event);

		Empire newEmpire = empire.apply(event);
		return new EmpirePlanningWrapper(newEmpire, planning);
	}

	/**
	 * Add a drafted card to the production line
	 * 
	 * @param consumer
	 * @param action
	 * @return
	 */
	public EmpirePlanningWrapper moveToProduction(ActionMoveDraftedToProduction action,
			Consumer<EmpireEvent> consumer) {
		DraftedAndPlanning dab = planning.draft(action.getTargetId());
		MoveToProductionEvent event = empire.moveToProduction(dab.getDrafted());

		consumer.accept(event);

		Empire newEmpire = empire.apply(event);
		return wrapper(newEmpire, dab);
	}

	/**
	 * Check if there is still something to do or not
	 * 
	 * @return
	 */
	@JsonIgnore
	public boolean isEmpty() {
		return planning.isEmpty();
	}

	/**
	 * Recyle a drafted card to a production card
	 * 
	 * @param action
	 * @param consumer
	 * @return
	 */
	public EmpirePlanningWrapper recycleToProduction(ActionRecycleDraftedToProduction action,
			Consumer<EmpireEvent> consumer) {
		DraftedAndPlanning dab = planning.draft(action.getTargetId());
		AffectProductionEvent event = empire.recycleToProduction(action.getProductionId(), dab.getDrafted());

		consumer.accept(event);

		Empire newEmpire = empire.apply(event);
		return wrapper(newEmpire, dab);
	}

	/**
	 * Recycle a card to the empire resource
	 * 
	 * @param action
	 * @param consumer
	 * @return
	 */
	public EmpirePlanningWrapper recycleDrafted(ActionRecycleDrafted action, Consumer<EmpireEvent> consumer) {
		DraftedAndPlanning dab = planning.draft(action.getTargetId());
		RecycleEvent event = empire.recycle(dab.getDrafted());

		consumer.accept(event);

		Empire newEmpire = empire.apply(event);
		return wrapper(newEmpire, dab);
	}

	private EmpirePlanningWrapper wrapper(Empire newEmpire, DraftedAndPlanning dab) {
		return new EmpirePlanningWrapper(newEmpire, dab.getPlanning());
	}

	public List<DraftedCard> getDrafteds() {
		return planning.getDrafteds();
	}

}
