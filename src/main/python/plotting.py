import matplotlib.pyplot as plt
import networkx as nx


def plot_ints(planning_times: {str: [int]}):
	for run, data in planning_times.items():
		plt.plot(data, label=run)
	plt.legend()
	plt.show()


def plot_vertices_usage(intersection: nx.DiGraph, locations: dict[int, (float, float)], usage: [float]) -> None:
	colors = [(u, u, u) for u in usage]
	nx.draw_networkx(intersection, locations, node_color=colors, node_shape='o', labels={}, label="Graph")
	plt.show()
