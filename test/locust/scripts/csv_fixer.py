"""Read a failure log file and ask for each entry if is should be kept in gams.csv.

Usage:

    rund csv_fixer.py --help

Gunter Vasold, 2020-05-05
"""
import argparse
import csv
import re
import sys

def parse_args():
    parser = argparse.ArgumentParser(
        description=('Remove unwanted paths from gams.csv. '
                    'Reads a failure.csv file generated by locust '
                    'and asks for each line if it should be remove from gams.csv')
        )
    parser.add_argument('ffile', 
        metavar='FAILURE FILE',
        help='The csv file containing the failures logged by locust')
    parser.add_argument('-s', '--status_code', 
        default=None, metavar="STATUS CODE", type=int,
        help=('Show only failures with this status code. '
              'If not set, alls failures will be shown'))
    parser.add_argument('-d', '--data-file', 
        default='gams.csv', metavar='DATA FILE',
        help="csv file containing the paths. Default is 'gams.csv'")
    parser.add_argument('-o', '--output-file', 
        metavar="OUTPUT FILE", default=None,
        help=("Name of the output file. If not set name of datafile "
             "with suffix '.clean' will be used"))
    args = parser.parse_args()
    if args.output_file is None:
        args.output_file = args.data_file + '.clean'
    return args

def filter_data(filename, paths_to_remove):
    """Return clean version of `filename`.

    Remove all entries contained in paths_to_remove.
    """
    rv = []
    with open(filename) as fh:
        reader = csv.reader(fh)
        for row in reader:
            if row[0] not in paths_to_remove:
                rv.append(row)
    return rv


def write_data(data, filename):
    "Write data to csv file `filename`."
    with open(filename, 'w') as fh:
        writer = csv.writer(fh)
        writer = writer.writerows(data)


def read_failures(filename):
    """Read failures from `filename`.

    Extract status code and sort by status code.
    """
    with open(filename) as fh:
        failures = []
        reader = csv.DictReader(fh)
        for row in reader:
            row['Code'] = int(re.search(r' code=(\d+)$', row['Error']).group(1))
            del row['Error']
            del row['Method']
            failures.append(row)
    failures.sort(key=lambda x: x['Code'])
    return failures


def ask(question, default_answer):
    "Ask question. Only accept 'k' or 'r' as answer."
    while True:
        answer = input(question)
        if answer in ('kr'):
            return answer
        if answer == '':
            return default_answer

def main(args):
    to_remove = []
    for row in read_failures(args.ffile):
        if args.status_code is None or args.status_code == row['Code']:
            print("{} ({}x) {}".format(row['Code'], row['Occurrences'], row['Name']))
            if ask('(k)eep or (r)emove? [k]', 'k') == 'r':
                to_remove = row['Name']
        else:
            print(type(args.status_code), type(row['Code']))
    write_data(filter_data(args.data_file, to_remove), args.output_file)


if __name__ == '__main__':
    main(parse_args())
