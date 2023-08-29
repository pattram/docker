#!/usr/bin/env python3
"""Extract subset of gams.csv for one ore more projects.
"""
import argparse
import csv
import re
import sys

def parse_args():
    "Parse command line arguments."
    argparser = argparse.ArgumentParser(description=
            "Extract url subset from gams.csv to test single or multiple projects.")
    argparser.add_argument('projects', metavar='PROJECT', nargs='+',
            help=("One or more project pids (like 'wms'). Multiple projects "
                  "can be delimited by space or '|'"))
    argparser.add_argument('-o', '--output-file', metavar="FILENAME", default=None,
            help=("Write output to file FILENAME"))
    args = argparser.parse_args()
    splitted_args = []
    for arg in args.projects:
        splitted_args += [a.strip() for a in arg.split('|')]
    args.projects = splitted_args
    return args


def create_subset(projects):
    """Yield all test lines for one or more projects.

    :param projects: An iterable with project names
    """
    objtypes = ('container', 'collection', 'o', 'context', 'query', 'cirilo', 'podcast')
    with open('gams.csv') as fh:
        pattern1 = '(' + '|'.join(objtypes) + ')'
        reader=csv.reader(fh, delimiter=',') 
        for row in reader:
            # some static files
            if re.match(r'^/(fonts|oaiprovider|lido)/', row[0]):
                yield row
                continue
            # g3search
            for proj in projects:
                if re.search(r'{}:{}'.format(pattern1, proj), row[0]):
                    yield row
                    break
                # short forms
                if re.match(r'^/{}/?$'.format(proj), row[0]) or \
                        re.match(r'/{}/.+'.format(proj), row[0]):    
                    yield row
                    break
                # search for project pid
                if row[0].startswith('/search') and \
                    re.search(r'pid={}(&|$)'.format(proj), row[0]):
                    yield row
                    break

if __name__ == '__main__':
    args = parse_args()
    if args.output_file is None:
        fh = sys.stdout
    else:
        fh = open(args.output_file, 'w')
    writer = csv.writer(fh)
    for row in create_subset(args.projects):
        writer.writerow(row)
    if args.output_file is not None:
        fh.close()
        
