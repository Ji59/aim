#! /usr/bin/env python3

import os.path as path
import json


def get_directory(argv: [str]):
	if len(argv) > 1:
		directory = argv[1]
	else:
		directory = path.expanduser("~/statistics")
	print(directory)
	assert path.isdir(directory)
	return directory


def get_intersection(directory: str):
	with open(path.join(directory, "Parameters.json"), 'r') as parameters_file:
		parameters = json.load(parameters_file)
		graph = parameters.get("graph")
	return graph


def get_agents(directory: str):
	agents_path = path.join(directory, "agents.json")
