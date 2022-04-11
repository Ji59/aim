package cz.cuni.mff.kotal.simulation;


import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import cz.cuni.mff.kotal.backend.algorithm.Algorithm;
import cz.cuni.mff.kotal.frontend.simulation.SimulationAgents;
import cz.cuni.mff.kotal.simulation.graph.SimulationGraph;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;


public class LoadingSimulation extends Simulation {

	private Timer timer;
	private final ListIterator<Agent> sortedAgentsIterator;
	private long finalStep = 0;

	public LoadingSimulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents, String path) throws FileNotFoundException {
		super(intersectionGraph, algorithm, simulationAgents);
		sortedAgentsIterator = loadAgents(path);
	}

	@Override
	protected void start() {
		long msPeriod = period / 1_000_000;
		long delay = isRunning() ? msPeriod : 0L;
		startTime = System.nanoTime() - (isRunning() ? period : 0);

		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, msPeriod);
	}

	@Override
	protected boolean loadAndUpdateStepAgents(long step) {
		if (sortedAgentsIterator.hasNext()) {
			List<Agent> newAgents = new ArrayList<>();
			Agent nextAgent = null;
			while (sortedAgentsIterator.hasNext() && (nextAgent = sortedAgentsIterator.next()).getArrivalTime() <= step) {
				newAgents.add(nextAgent);
			}
			if (nextAgent != null && nextAgent.getArrivalTime() > step && sortedAgentsIterator.hasPrevious()) {
				sortedAgentsIterator.previous();
			}

			synchronized (delayedAgents) {
				newAgents.forEach(agent -> delayedAgents.get(agent.getEntry()).add(agent));
			}

			synchronized (createdAgentsQueue) {
				createdAgentsQueue.addAll(newAgents);
			}
		} else {
			synchronized (delayedAgents) {
				if (delayedAgents.values().stream().allMatch(Collection::isEmpty)) {
					// FIXME refactor
					if (step > finalStep) {
						ended = true;
					} else {
						updateStatistics(sortedAgentsIterator.nextIndex());
					}
					return false;
				}
			}
		}

		List<Agent> entriesAgents;
		synchronized (delayedAgents) {
			entriesAgents = delayedAgents.values().stream().map(entryList -> entryList.stream().findFirst().orElse(null)).filter(Objects::nonNull).toList();
		}

		Collection<Agent> plannedAgents = algorithm.planAgents(entriesAgents, step);
		plannedAgents.forEach(agent -> {
			simulationAgents.addAgent(agent);
			agent.setPlannedTime(step);
			long leavingTime = agent.getPath().size() + step;
			if (leavingTime > finalStep) {
				finalStep = leavingTime;
			}
		});
		synchronized (plannedAgentsQueue) {
			plannedAgentsQueue.addAll(plannedAgents);
		}

		Set<Agent> rejectedAgents;
		synchronized (delayedAgents) {
			delayedAgents.values().parallelStream().forEach(entryList -> entryList.removeAll(plannedAgents));

			// TODO replace with iterator
			rejectedAgents = delayedAgents.values().parallelStream().flatMap(Collection::parallelStream).filter(agent -> agent.getArrivalTime() < getStep() - maximumDelay).collect(Collectors.toSet());
			delayedAgents.values().parallelStream().forEach(entryList -> entryList.removeAll(rejectedAgents));
		}

		synchronized (rejectedAgentsQueue) {
			rejectedAgentsQueue.addAll(rejectedAgents);
		}

		updateStatistics(sortedAgentsIterator.nextIndex());

		return false;
	}

	private ListIterator<Agent> loadAgents(String path) throws FileNotFoundException {
		FileReader reader = new FileReader(path);
		Gson gson = new Gson();
		List<Agent> agents = gson.fromJson(reader, new TypeToken<List<Agent>>() {
		}.getType());
		agents.sort(Comparator.comparingDouble(Agent::getArrivalTime));
		allAgents.putAll(agents.stream().collect(Collectors.toMap(Agent::getId, Function.identity())));
		return agents.listIterator();
	}

	@Override
	protected void stopSimulation() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	@Override
	protected void resetSimulation() {
		if (timer != null) {
			timer.cancel();
			timer = null;
		}
	}

	private TimerTask getTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				System.out.println(step);
				long currentStep = getStep(); // TODO

				loadAndUpdateStepAgents(currentStep);

				updateTotalAgents(currentStep);
				updateAgentsDelay(currentStep);
				updateRejectedAgents(currentStep);
				updateStatistics(allAgents.size());
				step++;
			}
		};
	}
}
