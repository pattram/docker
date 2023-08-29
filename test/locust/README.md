# Locust for Gams4

This image can be used to generate traffic on the server.

## Quickstart

To create traffic on a development system on localhost:

1. Run the integration tests first to create test objects
2. Start a locust container from the locust directory with
   `docker-compose up -d`
3. Point your browser to the URL and port of your Locust instance: 
   (possibly http://localhost:24141)
4. Fill in the required values (e.g. `10`, `3`, `<address of your gams installation>`>)
    and click on 'start swarming'. 
    Hint: using https://localhost/ **will not work** here! To work around this:

   1. Use either the ip-address or name of your docker host
   2. or run the locust container in the docker network gams runs in and use 
      https://apache/ or http://apache:82/ as value for `host`. There is a 
      detailed description how to do so at the end of this document.

## Configuration

### Test Sets

At the moment we provide these test sets:

1. `test` This uses the default set of URLs to test for a repository which only 
   contains test objects. This test set is used by default.
2. `gams` This uses the 10 000 most popular request against Gams3 from February 2020.
   Use this set to simulate users on a repository containing the real gams data.
3. 'run.csv' and 'run-N.csv'. These only contain request usable with the according 
   test data sets made by Johannes (run.sh, run-1.sh etc.)
4. `benchmark` This is a small subset of gams, which can be used to find out how the 
   number of users affects response times. This set might be removed in future, as 
   benchmark.py does the same but also handles errors better.

#### Using the default test set

The default test set uses urls extracted automatically from the integration test 
code (see `scripts/extract_from_tests.py`) and stored as `test.csv`.

To run locust against this test set, start the container with environment variable 
`DATASET=test` (or without setting `DATASET`):

~~~bash
docker-compose up -d
~~~

or

~~~bash
DATASET=test docker-compose up -d
~~~

#### Using the gams test set

The gams test set uses the 10000 most popular requests made on gams.uni-graz.at during 
February 2020. The data is stored in gams.csv by using this script: 
`script/extract_from/gamslogs.py`.

To run locust against this test set, start the container with environment variable `DATASET=gams`:

~~~bash
DATASET=gams docker-compose up -d
~~~

#### Modifying the url data set

You can always add or remove lines in the csv files. Remember to rebuild the image i
afterwards by running

~~~bash
docker-compose build locust
~~~


### Running benchmark.py

benmark.py ist a special Locust file with tests a small set of endpoints. As it does 
not only check for status_codes, but also for the correctness of resonses, it is a 
better alternative for the benchmark.csv test set.

To run this benchmark, uncomment this line in docker-compose.yaml:

```
    command: -f /home/locust/benchmark.py --reset-stats
```

Do not forget to add the comment sign afterwards, otherwise the test sets will not work.

#### Adding a data set

To create a new url data set, create a new csv-file. For simple cases put one path 
(absolute on the server) in each line. You can also add a number (separated by a 
comma) as second value, which defines the weight of this path. A weight of `2` means 
that this path (url) is used twice as often as a path with weight `1`.

So if you do not need weights, the file should look like in `test.csv`:

~~~text
/archive/objects/context:emile/datastreams/QUERY/content
/archive/objects/corpus:emile/datastreams/CMDI/content
~~~

if you want weights, use something like

~~~text
/context:fercan/PROVINZ,9
/archive/objects/query:km.wildererkk/methods/sdef:Query/get?mode=query,6
/archive/objects/context:ges/methods/sdef:Context/get?mode=collection,4
/archive/objects/context:km/methods/sdef:Context/get?mode=project,1
~~~

To use the new data set, run the server like

~~~bash
DATASET=foo docker-compose up -d
~~~

where `foo` ist the name of the csv file without `.csv`.

Open `http://localhost:24141/` in your browser and set the desired values before 
starting the run.

If you want to run it directly on it030021, you scan either use a ssh tunnel or start 
locust in headless mode.

## Generating load

By default, the locust server listens on port 24141 (this can be changed in
`docker-compose.yaml`). So point your browser to `<hostname>:24141/` and fill
out the form fields:

### Number of total users to simulate

A single User generates low traffic (might be up to 5 seconds between request).
To stress the server use a relatively high number here.

### Hatch rate

Users created per second.

### Host

Something like `https://it030021.uni-graz.at/`

Using `localhost` will not work here, as localhost points the to locust container. 
To circumvent this restriction:

* Point to the name or ip address of your docker host
* Put the locust container in the same docker network as your apache container 
  following the steps listed below

#### Running locust and apache in the same docker network

This is only necessary if your GAMS installations runs on localhost.

1. Find out in which network your apache container runs:

   ~~~bash
   docker inspect apache | grep NetworkMode
   ~~~

    The name of the network (so the value of 'NetworkNode' will be refered as 
    `<NetworkName>` below)

2. Add this to docker-compose.yml:

   ~~~
      ...
      ports:
        - "24141:8089"
      # new configuration starts here
      networks:
        - <NetworkName>

    networks:
      <NetworkName>:
        external: true
   ~~~

3. (Re-)Start the locust container via `docker-compose up`
4. Enter `https://apache/

## Changing timeout

Locust uses a default timeout of 60 seconds. This can be change by setting the
`TIMEOUT=` variable, e.g.:

~~~bash
TIMEOUT=10;DATASET=gams docker-compose up -d
~~~

## Generate really high traffic

Running a single locust container should be enough for most use cases.
But you can start any number of of locust container with a single master.
For details see 
https://docs.locust.io/en/stable/running-locust-docker.html#environment-variables
