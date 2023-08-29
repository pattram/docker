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
from locust.contrib.fasthttp import FastHttpUser


class WebsiteUser(FastHttpUser):
    "A single user."
    #tasks = [fetch_random_url]
    wait_time = between(0.1, 5.0)
    network_timeout = float(os.environ.get("TIMEOUT", "60.0"))
    connection_timout = float(os.environ.get("TIMEOUT", "60.0"))

    @task
    def tei_get(self):
        url = "/archive/get/o:rem.2267/sdef:TEI/get"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            if "nachgetragen" not in response.text:
                logging.warning("'nachgetragen' not in response (%s)" % url)
                response.failure("Unexpected response")
            size = len(response.content)
            if not self.is_circa(441131, size):
                response.failure("Unexpected size: %d" % size)

    @task
    def datatstream_img(self):
        url = "/o:rollett.1877/IMG.10"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            size = len(response.content)
            if not self.is_circa(398805, size):
                response.failure("Unexpected size: %d (%s)" % (size, url))

    @task
    def context_datastream(self):
        url = "/context:fercan/FUNDORTE"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            if "Rijsbergen-Zundert" not in response.text:
                logging.warning("'Rijsbergen-Zundert' not in response (%s)" % url)
                response.failure("Unexpected response")

    @task
    def story(self):
        url = "/o:km.story"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            if "Wildererprojekt" not in response.text:
                logging.warning("'Wildererprojekt' not in response (%s)" % url)
                response.failure("Unexpected response")


    @task
    def iiif(self):
        url = "/iiif/o:numis.1151/IMAGE_1/full/!120,120/0/default.jpg"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            size = len(response.content)
            if not self.is_circa(4858, size):
                response.failure("Unexpected size: %d (%s)" % (size, url))


    @task
    def context_cantus(self):
        url = "/context:cantus"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            if "Libri Ordinarii" not in response.text:
                logging.warning("'Libri Ordinarii' not in response (%s)" % url)
                response.failure("Unexpected response")

    @task
    def getPDF(self):
        url = "/archive/get/o:stub.42/sdef:PDF/get"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            size = len(response.content)
            if not self.is_circa(78608, size):
                response.failure("Unexpected size: %d (%s)" % (size, url))


# TODO: Elisabeth fragen ob das ok ist
#    @task
    def simple_query(self):
        url = "/archive/objects/query:km.wildererkk/methods/sdef:Query/get?mode=query"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            if response.text != "nachgetragen" not in response.text:
                logging.warning("'nachgetragen' not in response")
                response.failure("Unexpected response")
            size = len(response.content)
            if not self.is_circa(441131, size):
                response.failure("Unexpected size: %d" % size)


    @task
    def font(self):
        url = "/fonts/DroidSerif/DroidSerif.woff2"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            size = len(response.content)
            if not self.is_circa(23436, size):
                response.failure("Unexpected size: %d (%s)" % (size, url))

# TODO: funktioniert aktuell nicht
#    @task
    def gsearch(self):
        url = "/search/gsearch?query=&hitPageSize=10&hitPageStart=1&pid=gm&search=simple&x2=http://gams.uni-graz.at%2Fgm%2Fgm-search.xsl"
        with self.client.get(url, catch_response=True) as response:
            if response.status_code >= 400:
                logging.warning('Responce code %d for %s' % (response.status_code, url))
            if response.text != "nachgetragen" not in response.text:
                logging.warning("'nachgetragen' not in response")
                response.failure("Unexpected response")
            size = len(response.content)
            if not self.is_circa(441131, size):
                response.failure("Unexpected size: %d" % size)


    @classmethod
    def is_circa(cls, expected, val, offset_pct=5):
        "Check if val close to expected (+-5%)"
        divisor = 100 // offset_pct	
        offset = expected // divisor 
        return val >= (expected - offset) and val <= (expected + offset)


