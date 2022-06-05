#! /usr/bin/env python3
import os.path as path


def get_directory(argv: [str]):
	if len(argv) > 1:
		directory = argv[1]
	else:
		directory = path.expanduser("~/statistics")
	print(directory)
	assert path.isdir(directory)
	return directory


def get_agents(directory: str):
	import json

	agents_path = path.join(directory, "agents.json")
