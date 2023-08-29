"""Run Locust with random requests.

All requests paths (incl. query strings) are fetched from
a file called urls.csv.

This file can contain single lines like

/foo?bar=1

or

contain the weight as second column:

/foo?bar=1,2

A weight of 2 means that this path is used twice as often than a path with
weight 1. Weight must be an integer!

GV, 2020-04-21
"""



import csv
import os
import random
import logging

from locust import User, between, task
#from locust.contrib.fasthttp import FastHttpUser
from locust import HttpUser

url_cache = []

class URLSource:

    urls = []

    @classmethod
    def get_random_url(cls):
        if not cls.urls:
            urls_csv_file = os.environ.get("DATASET", "test") + ".csv"
            logging.info("Using dataset for '{}'".format(urls_csv_file))
            cls.urls = cls.load_urls(urls_csv_file)
        random_url = random.choice(cls.urls)
        logging.debug(random_url)
        return random_url

    @classmethod
    def load_urls(cls, csv_file):
        paths = []
        with open(csv_file) as fh:
            reader = csv.reader(fh)
            for row in reader:
                if row:
                    if row[0].startswith('#'):
                        continue
                    if len(row) == 1:
                        weight = 1
                    else:
                        weight = int(row[1])
                    for _ in range(weight):
                        paths.append(row[0])
        random.shuffle(paths)
        return paths


def fetch_random_url(user):
    "Callback for WebsiteUser: requests a random url."
    url = URLSource.get_random_url()
    r = user.client.get(url)
    if r.status_code >= 400:
        logging.info('Bad request: {} for {}'.format(r.status_code, url))



class WebsiteUser(HttpUser):
    "A single user."
    tasks = [fetch_random_url]
    wait_time = between(0.1, 5.0)
    network_timeout = float(os.environ.get("TIMEOUT", "60.0"))
    connection_timout = float(os.environ.get("TIMEOUT", "60.0"))
    print('Setting timeout to {:.1f} seconds'.format(network_timeout))



#class Ingester(FastHttpUser):
#    "Simulate a single ingester."
#
#    def on_start(self):
#        # how do we get data to ingest?
#        self.urls = []
#
#    @task(1)
#    def ingest(self):
#        ur = random.choice(self.urls)
#        r = self.client.post() # FIXME: see api
#        if r.status_code >= 400:
#            print('Bad request: {} for {}'.format(r.status_code, url))



