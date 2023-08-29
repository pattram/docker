"""
Extract data from logfiles which can be used as input data for locust
"""
import argparse
import csv
import gzip
import io
import re
import sys
from collections import Counter

from apachelogfileparser import ApacheLogFileParser


def parse_args():
    parser = argparse.ArgumentParser(description="Collect most common requests from logfiles")
    parser.add_argument("logfiles", nargs='+', 
        help="The logfiles to use for URL collection.")
    parser.add_argument(
        "-o",
        "--output-file",
        metavar="OUTPUTFILE",
        default=None,
        help="Write output to file OUTPUTFILE",
    )
    parser.add_argument(
        "-s",
        "--stop_after",
        type=int,
        default=0,
        metavar="NUM_OF_LOG_LINES",
        help="Stop after reading NUM_OF_LOG_LINES. Default is 0, which "
        + "means read all lines.",
    )
    parser.add_argument(
        "-c",
        "--most-common",
        type=int,
        default=1000,
        metavar="NUM_OF_URLS",
        help="Print the NUM_OF_URLS most common URLs to stdout",
    )
    return parser.parse_args()


def get_line(logfile):
    "Yield a parsed log file line as namedtuple."
    with gzip.open(logfile, "rb") as fh:
        for line in fh:
            yield line.decode("utf-8")


def ignore_resource(row):
    "Return True if line is not of interest."
    res = row.get("resource", "")
    if row.get('status') > 399:
        return True
    if row.get('method') != 'GET':
        return True
    if '/wp-' in res:
        return True
    if '/.env' in res:
        return True
    if '/archive/desribe' in res:
        return True
    if 'emile' in res:
        return True
    if '/docs' in res:
        return True
    if '/doku' in res:
        return True
    if res.startswith('/archive/management'):
        return True
    if res.startswith('/archive/services'):
        return True
    if res.startswith('/services/'):
        return True
    if res.startswith('/.well-known'):
        return True
    if res.startswith('/archive/risearch'):
        return True
    if res.startswith('/cocoon/toHTM'):
        return True
    if res.startswith('/bigdata'):
        return True
    if res is None:
        return True
    if re.match(r"/(fedora|podcasts|rss|robots).*", res):
        return True
    if re.match(r"/iiif", res):
        return False
    if re.search(r".*(css|js|png|jpg|gif|ico)", res):
        return True
    return False


def collect_data(logfiles, stop_after):
    data = []
    parser = ApacheLogFileParser("raw")
    line_counter = 0
    for logfile in logfiles:
        print('processing %s' % logfile)
        for line in get_line(logfile):
            if stop_after > 0 and line_counter-1 == stop_after:
                break
            row = parser.parse(line)
            if not ignore_resource(row):
                data.append(row["resource"])
    return Counter(data)


def write_output(counter, fh, most_common):
    writer = csv.writer(fh)
    for url, c in counter.most_common(most_common):
        writer.writerow((url, c))


if __name__ == "__main__":
    args = parse_args()
    counter = collect_data(args.logfiles, args.stop_after)
    if args.output_file is None:
        fh = io.StringIO()
        write_output(counter, fh, args.most_common)
        print(fh.getvalue())
    else:
        with open(args.output_file, "w") as fh:
            write_output(counter, fh, args.most_common)
