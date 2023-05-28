#! /usr/bin/env python3

import matplotlib.pyplot as plt


def plot_times(planning_times: {str: [int]}):
	for run, data in planning_times.items():
		plt.plot(data, label=run)
	plt.legend()
	plt.show()


if __name__ == "__main__":
	import sys
	import loadDirectory
	import os.path as path

	directories = loadDirectory.get_directories(sys.argv)

	planning_times = {}

	for directory in directories:
		for subdir in loadDirectory.get_subdirectories(directory):
			planning_times[subdir] = loadDirectory.get_planning_times(path.join(directory, subdir))

	plot_times(planning_times)
