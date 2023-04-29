import loadDirectory

if __name__ == "__main__":
	import plotting
	import plannedAgentsData
	import sys
	import numpy as np

	directories = loadDirectory.get_directories(sys.argv)
	for directory in directories:
		agents = loadDirectory.get_agents(directory)
		parameters = loadDirectory.get_parameters(directory)

		planning_times = parameters.simulatingTime
		times = plotting.convolute(planning_times, len(planning_times) // 2 ** 7)

		last_step = int(np.ceil(parameters.steps))
		travelling_agents, _ = plannedAgentsData.steps_travelling_agents(last_step, agents)
		agents = plotting.convolute(travelling_agents, len(travelling_agents) // 2 ** 7)

		plotting.plot_time_vs_agents(times, agents)
