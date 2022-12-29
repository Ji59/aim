#! /usr/bin/env python3

import numpy as np
import plotting
import loadDirectory
import generateIntersection


def steps_planned_agents(last_step: int, agents: [loadDirectory.Agent]) -> (int, [int]):
	agents_planned = [0] * last_step
	not_planned = 0
	for agent in agents:
		planned_time = agent.plannedTime
		if planned_time >= 0:
			agents_planned[planned_time] += 1
		else:
			not_planned += 1
	return not_planned, agents_planned


def steps_travelling_agents(last_step: int, agents: [loadDirectory.Agent]) -> [int]:
	travelling_agents = [0] * last_step
	for agent in agents:
		planned_step = agent.plannedTime
		traveling_time = len(agent.path)
		if planned_step >= 0:
			assert planned_step + traveling_time <= last_step
			for s in range(planned_step, planned_step + traveling_time):
				travelling_agents[s] += 1
	return travelling_agents


def vertices_visits(vertices: int, agents: [loadDirectory.Agent]) -> [int]:
	visits = [0] * vertices
	for agent in agents:
		for vertexID in agent.path:
			visits[vertexID] += 1
	return visits


if __name__ == "__main__":
	import sys
	import os.path as path

	directories = loadDirectory.get_directories(sys.argv)

	for directory in directories:
		agents_data = {}
		last_step = 0
		graph = 0
		for subdir in loadDirectory.get_subdirectories(directory):
			subdir_path = path.join(directory, subdir)
			agents = loadDirectory.get_agents(subdir_path)
			parameters = loadDirectory.get_parameters(subdir_path)

			simulation_last_step = parameters.steps
			last_step = max(int(np.ceil(simulation_last_step)), last_step)

			not_planned, agents_planned = steps_planned_agents(last_step, agents)

			travelling_agents = steps_travelling_agents(last_step, agents)

			agents_data[subdir] = (not_planned, agents_planned, travelling_agents)

			print(f"{subdir}: {not_planned}, {np.mean(agents_planned): .2f}, {np.var(agents_planned): .2f}"
						f", {np.mean(travelling_agents): .2f}, {np.var(travelling_agents): .2f}")

			graph = parameters.graph
			vertices_visits = vertices_visits(len(graph.vertices), agents)
			vertices_occupation = [visits / simulation_last_step for visits in vertices_visits]
			intersection, locations, entries, exits = generateIntersection.generate_intersection(graph)
			plotting.plot_vertices_usage(intersection, locations, vertices_occupation)

		travelling_data = {}
		for subdir, data in agents_data.items():
			travelling_data[subdir] = data[2] + [0] * (last_step - len(data[2]))
		# travelling_data[subdir + "_p"] = data[1] + [0] * (last_step - len(data[1]))

		plotting.plot_ints(travelling_data)
