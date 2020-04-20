package fr.keyser.wonderfull.world.game;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import fr.keyser.wonderfull.world.CurrentDraft;
import fr.keyser.wonderfull.world.DraftableCard;
import fr.keyser.wonderfull.world.DraftedCard;
import fr.keyser.wonderfull.world.Empire;
import fr.keyser.wonderfull.world.Token;
import fr.keyser.wonderfull.world.Tokens;
import fr.keyser.wonderfull.world.action.ActionAffectToProduction;
import fr.keyser.wonderfull.world.action.ActionConvert;
import fr.keyser.wonderfull.world.action.ActionDraft;
import fr.keyser.wonderfull.world.action.ActionMoveDraftedToProduction;
import fr.keyser.wonderfull.world.action.ActionPass;
import fr.keyser.wonderfull.world.action.ActionRecycleDrafted;
import fr.keyser.wonderfull.world.action.ActionRecycleDraftedToProduction;
import fr.keyser.wonderfull.world.action.ActionRecycleProduction;
import fr.keyser.wonderfull.world.action.ActionSupremacy;
import fr.keyser.wonderfull.world.action.EmpireAction;
import fr.keyser.wonderfull.world.dto.ArgumentLessAction;
import fr.keyser.wonderfull.world.dto.CardPossibleActions;
import fr.keyser.wonderfull.world.dto.FullPlayerEmpireDTO;
import fr.keyser.wonderfull.world.dto.PlayerActionsDTO;
import fr.keyser.wonderfull.world.dto.PossibleAction;
import fr.keyser.wonderfull.world.dto.PossibleAffectations;
import fr.keyser.wonderfull.world.dto.PossibleRecycleToProduction;
import fr.keyser.wonderfull.world.event.EmpireEvent;
import fr.keyser.wonderfull.world.event.PassedEvent;

public class PlayerEmpire {

	private final Empire empire;

	private final EmpireDraftWrapper draft;

	private final EmpirePlanningWrapper planning;

	private final EmpireProductionWrapper production;

	public PlayerEmpire(Empire empire) {
		this(empire, null, null, null);
	}

	@JsonCreator
	public PlayerEmpire(@JsonProperty("draft") EmpireDraftWrapper draft,
			@JsonProperty("planning") EmpirePlanningWrapper planning,
			@JsonProperty("production") EmpireProductionWrapper production) {
		this(null, draft, planning, production);
	}

	PlayerEmpire(Empire empire, EmpireDraftWrapper draft, EmpirePlanningWrapper planning,
			EmpireProductionWrapper production) {
		this.empire = empire;
		this.draft = draft;
		this.planning = planning;
		this.production = production;
	}

	public PlayerEmpire dispatch(EmpireAction action, Consumer<EmpireEvent> consumer) {
		if (action instanceof ActionDraft)
			return draft((ActionDraft) action, consumer);
		else if (action instanceof ActionMoveDraftedToProduction)
			return moveToProduction((ActionMoveDraftedToProduction) action, consumer);
		else if (action instanceof ActionRecycleDraftedToProduction)
			return recycleToProduction((ActionRecycleDraftedToProduction) action, consumer);
		else if (action instanceof ActionRecycleDrafted)
			return recycleDrafted((ActionRecycleDrafted) action, consumer);
		else if (action instanceof ActionSupremacy)
			return supremacy((ActionSupremacy) action, consumer);
		else if (action instanceof ActionRecycleProduction)
			return recycleProduction((ActionRecycleProduction) action, consumer);
		else if (action instanceof ActionAffectToProduction)
			return affectToProduction((ActionAffectToProduction) action, consumer);
		else if (action instanceof ActionConvert) {
			return convert(consumer);
		} else if (action instanceof ActionPass) {
			// no op
			consumer.accept(PassedEvent.PASS);
			return this;
		}

		throw new IllegalActionException("unknownAction");
	}

	public PlayerEmpire draft(List<DraftableCard> inHand) {

		List<DraftedCard> drafteds = Collections.emptyList();
		if (draft != null)
			drafteds = draft.getDrafteds();

		return new PlayerEmpire(new EmpireDraftWrapper(resolveEmpire(), new CurrentDraft(drafteds, inHand)), null,
				null);
	}

	public PlayerEmpire draftToPlanning() {
		if (draft == null)
			throw new IllegalActionException("draftToPlanning");

		return new PlayerEmpire(null, new EmpirePlanningWrapper(resolveEmpire(), draft.getDrafteds()), null);
	}

	public PlayerEmpire startProductionStep(Token step) {
		return new PlayerEmpire(null, null, new EmpireProductionWrapper(resolveEmpire(), step));
	}

	public Empire resolveEmpire() {
		if (draft != null)
			return draft.getEmpire();
		if (planning != null)
			return planning.getEmpire();
		if (production != null)
			return production.getEmpire();

		return empire;
	}

	public PlayerEmpire draft(ActionDraft action, Consumer<EmpireEvent> consumer) {
		if (draft == null)
			throw new IllegalActionException("draft");
		return new PlayerEmpire(draft.draft(action, consumer), null, null);
	}

	public PlayerEmpire moveToProduction(ActionMoveDraftedToProduction action, Consumer<EmpireEvent> consumer) {
		if (planning == null)
			throw new IllegalActionException("moveToProduction");

		return new PlayerEmpire(null, planning.moveToProduction(action, consumer), null);
	}

	public PlayerEmpire recycleToProduction(ActionRecycleDraftedToProduction action, Consumer<EmpireEvent> consumer) {
		if (planning == null)
			throw new IllegalActionException("recycleToProduction");
		return new PlayerEmpire(null, planning.recycleToProduction(action, consumer), null);
	}

	public PlayerEmpire recycleDrafted(ActionRecycleDrafted action, Consumer<EmpireEvent> consumer) {
		if (planning == null)
			throw new IllegalActionException("recycleDrafted");
		return new PlayerEmpire(null, planning.recycleDrafted(action, consumer), null);
	}

	public PlayerEmpire supremacy(ActionSupremacy action, Consumer<EmpireEvent> consumer) {
		if (production == null)
			throw new IllegalActionException("supremacy");
		return new PlayerEmpire(null, null, production.supremacy(action, consumer));
	}

	public PlayerEmpire recycleProduction(ActionRecycleProduction action, Consumer<EmpireEvent> consumer) {
		if (production != null)
			return new PlayerEmpire(null, null, production.recycleProduction(action, consumer));

		if (planning != null)
			return new PlayerEmpire(null, planning.recycleProduction(action, consumer), null);

		throw new IllegalActionException("recycleProduction");
	}

	public PlayerEmpire affectToProduction(ActionAffectToProduction action, Consumer<EmpireEvent> consumer) {

		if (production != null)
			return new PlayerEmpire(null, null, production.affectToProduction(action, consumer));

		if (planning != null)
			return new PlayerEmpire(null, planning.affectToProduction(action, consumer), null);

		throw new IllegalActionException("affectToProduction");
	}

	public PlayerEmpire convert(Consumer<EmpireEvent> consumer) {

		if (production != null)
			return new PlayerEmpire(null, null, production.convert(consumer));

		throw new IllegalActionException("convert");
	}

	public PlayerActionsDTO asPlayerAction() {
		PlayerActionsDTO action = null;

		if (draft != null) {
			action = new PlayerActionsDTO();
			action.setCards(draft.getInHand().stream()
					.map(d -> new CardPossibleActions(d.getId(), asList(ArgumentLessAction.DRAFT))).collect(toList()));
		} else if (planning != null) {
			action = new PlayerActionsDTO();
			Empire emp = planning.getEmpire();
			List<CardPossibleActions> draftedsActions = planning.getDrafteds().stream().map(d -> {
				List<PossibleAction> actions = asList(ArgumentLessAction.MOVE_TO_PRODUCTION,
						ArgumentLessAction.RECYLE_DRAFT);

				List<Integer> targets = emp.getTargetsForDirectRecycle(d.getRecycle());
				if (!targets.isEmpty()) {
					actions = new ArrayList<>(actions);
					actions.add(new PossibleRecycleToProduction(targets));
				}

				return new CardPossibleActions(d.getId(), actions);
			}).collect(toList());

			List<CardPossibleActions> inProductionActions = getInProductionActions(emp, emp.getOnEmpire());
			List<CardPossibleActions> all = new ArrayList<>(draftedsActions.size() + inProductionActions.size());
			all.addAll(draftedsActions);
			all.addAll(inProductionActions);

			action.setCards(all);
			action.setPass(draftedsActions.isEmpty());

		} else if (production != null) {
			action = new PlayerActionsDTO();
			action.setPass(true);
			Empire emp = production.getEmpire();
			Tokens available = production.getAvailable();

			action.setConvert(available.has(production.getStep()));
			action.setCards(getInProductionActions(emp, available));

		}

		return action;
	}

	private List<CardPossibleActions> getInProductionActions(Empire emp, Tokens available) {
		return emp.getInProduction().stream().map(inp -> {

			Map<Integer, List<Token>> affectables = inp.affectablesSlots(available);

			List<PossibleAction> actions = new ArrayList<>();
			if (!affectables.isEmpty()) {
				actions.add(new PossibleAffectations(affectables));
			}
			actions.add(ArgumentLessAction.RECYLE_PRODUCTION);

			return new CardPossibleActions(inp.getId(), actions);
		}).collect(Collectors.toList());
	}

	public FullPlayerEmpireDTO asDTO(boolean done, boolean anon) {
		FullPlayerEmpireDTO dto = new FullPlayerEmpireDTO();
		Empire emp = resolveEmpire();
		dto.setEmpire(emp);
		dto.setScore(emp.score());
		dto.setDone(done);
		if (planning != null) {
			dto.setDrafteds(planning.getDrafteds());
			dto.setAvailable(emp.getOnEmpire().asMap());
		}

		if (draft != null) {
			List<DraftedCard> drafteds = draft.getDrafteds();
			dto.setDrafteds(drafteds);

			if (anon) {
				if (done && !drafteds.isEmpty())
					dto.setDrafteds(drafteds.subList(0, drafteds.size() - 1));
			} else if (!done) {
				dto.setInHand(draft.getInHand());
			}
		}

		if (production != null) {
			dto.setAvailable(production.getAvailable().asMap());
		}

		return dto;

	}

	public PlayerEmpire endProductionStep(Consumer<EmpireEvent> consumer) {
		if (production == null)
			throw new IllegalActionException("production");

		return new PlayerEmpire(production.done(consumer));
	}

	public EmpireDraftWrapper getDraft() {
		return draft;
	}

	public EmpirePlanningWrapper getPlanning() {
		return planning;
	}

	public EmpireProductionWrapper getProduction() {
		return production;
	}

}
