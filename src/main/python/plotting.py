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


def convolute(data, conv: int = 8):
	import numpy as np

	length = len(data)
	convoluted_data = [0] * length
	for i, d in enumerate(data):
		min_ = max(i - conv, 0)
		max_ = min(i + conv + 1, length)
		for j in range(min_, max_):
			convoluted_data[j] += d * (conv - abs(i - j) + 1) / (conv + 1)
			if j == i - conv:
				convoluted_data[j] /= min(2 * conv + 1, i)
	min_ = max(length - 2 * conv, 0)
	for i, j in enumerate(range(max(length - conv, 0), length)):
		convoluted_data[j] /= length - min_ - i
	return convoluted_data
