#!python

import pandas
import sys


def parse_file(fname: str):
    results = []
    with open(fname, 'r') as fp:
        graph, graph_result = '', dict()
        while line := fp.readline():
            s_line = line.strip('\n')
            if s_line.startswith("graphs"):
                # flush previous graph's results
                if graph_result:
                    results.append((graph, graph_result))
                # start new graph_result
                graph = s_line.split('/')[-1].split('.')[0]
                graph_result = {}
            elif s_line != "":
                parts = s_line.split('|')
                attr = parts[0].strip()
                for pair in parts[1].split(','):
                    method, value = pair.replace(' ', '').split(':')
                    if method in graph_result:
                        graph_result[method][attr] = value
                    else:
                        graph_result[method] = {attr: value}
        # flush last graph result
        if graph_result:
            results.append((graph, graph_result))
    return results


def results2csv(results: []):
    for fname, data in results:
        0


if __name__ == "__main__":
    file = "ppResults_2app.txt"
    if len(sys.argv) != 2:
        print("Usage: ./parseResults.py results.txt")
        print("resorting to default file")
    else:
        file = sys.argv[1]
    print(parse_file(file)[0])
