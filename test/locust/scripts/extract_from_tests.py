"""Extract possible request enpoints from the java integration test code.

Usage:
    run extract_from_tests.py --help

Gunter Vasold, 2020-04-21    
"""
import argparse
import glob
import os
import re

INTEGRATION_TEST_PATH = "src/test/java/org/emile/cirilo"


def parse_args():
    parser = argparse.ArgumentParser(
        description="Extract URLs to test from java integration test code"
    )
    parser.add_argument(
        "testdir", metavar="TESTDIR", help="Path to intergrationtest (inc. dir name)"
    )
    parser.add_argument(
        "-o",
        "--output-file",
        metavar="OUTPUTFILE",
        default=None,
        help="Write paths to OUTPUT_FILE",
    )
    return parser.parse_args()


def process_testfile(filename):
    "Extract paths from a single test file."
    paths = []
    with open(filename) as fh:
        apattern = r'(assert\w+)\("https://HOSTNAME(.*?)",'
        for line in fh:
            m = re.search(apattern, line)
            if m:
                # print(m.group(1))
                paths.append(m.group(2))
    return paths


def collect(testdir):
    "Collect all paths from testurls from all integration tests."
    paths = []
    src_dir = os.path.join(testdir, INTEGRATION_TEST_PATH)
    for filename in glob.glob(os.path.join(src_dir, "*.java")):
        paths += process_testfile(filename)
    return sorted(set(paths))


def write_to_csv(data, filename):
    "Write output to filename."
    with open(filename, "w") as fh:
        for row in data:
            fh.write("{}\n".format(row))


if __name__ == "__main__":
    args = parse_args()
    if args.output_file is None:  # output to stdout
        for row in collect(args.testdir):
            print(row)
    else:
        write_to_csv(collect(args.testdir), args.output_file)
