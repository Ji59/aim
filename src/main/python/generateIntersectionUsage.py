#! /usr/bin/env python3

import matplotlib.pyplot as plt
import networkx as nx



if __name__ == "__main__":
    import sys
    import loadDirectory
    directory = loadDirectory.get_directory(sys.argv)
    print(directory)
