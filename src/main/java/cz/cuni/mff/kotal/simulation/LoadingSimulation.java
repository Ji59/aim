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

	public LoadingSimulation(SimulationGraph intersectionGraph, Algorithm algorithm, SimulationAgents simulationAgents, String path) throws FileNotFoundException {
		super(intersectionGraph, algorithm, simulationAgents);
		sortedAgentsIterator = loadAgents(path);
	}

	@Override
	@Deprecated
	protected void start() {
		long msPeriod = period / 1_000_000;
		long delay = isRunning() ? msPeriod : 0L;
		startTime = System.nanoTime() - (isRunning() ? period : 0);

		timer = new Timer();
		timer.scheduleAtFixedRate(getTimerTask(), delay, msPeriod);
	}

	@Override
	protected Collection<Agent> loadAgents(long step) {
		if (sortedAgentsIterator.hasNext()) {
			List<Agent> newAgents = new ArrayList<>();
			Agent nextAgent = null;
			while (sortedAgentsIterator.hasNext() && (nextAgent = sortedAgentsIterator.next()).getArrivalTime() <= step) {
				newAgents.add(nextAgent);
			}
			if (nextAgent != null && nextAgent.getArrivalTime() > step && sortedAgentsIterator.hasPrevious()) {
				sortedAgentsIterator.previous();
			}
			return newAgents;
		} else {
			synchronized (delayedAgents) {
				if (delayedAgents.values().stream().allMatch(Collection::isEmpty)) {
					// FIXME refactor
					if (step > finalStep) {
						ended = true;
					}
				}
			}
		}
		return new ArrayList<>(0);
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

	@Deprecated
	private TimerTask getTimerTask() {
		return new TimerTask() {
			@Override
			public void run() {
				System.out.println(step);
				long currentStep = getStep(); // TODO

				loadAgents(currentStep);

				updateTotalAgents(currentStep);
				updateAgentsDelay(currentStep);
				updateRejectedAgents(currentStep);
				updateStatistics(allAgents.size());
				step++;
			}
		};
	}
}
