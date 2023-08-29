# GAMS

Set up and run a local GAMS environment.

**!! This project is still under development -- do not use it for production !!** 

** Important: 
If you have installed GAMS before 2021-05-21, you must delete all existing data,
because we have re-organized how objects are stored in Fedora.
Follow the instructtions below under "Resetting data"**


## Hardware requirements

The hardware requirements depend on the number and size of objects and datastreams you want to store and the number of request per second you want to serve.

For a local testing and development setup we recommend:

* RAM: GAMS will use at least 8GB of RAM. As your operating system and other programs also will need memory we suggest a machine with at least 16 GB of RAM. If you plan to run GAMS in a virtual machine, give a least 10 GB to this virtual machine. If you plan to store big data streams like videos (not suggested) or RTI images, you will need extra RAM.
* CPUS: 4 Cores should be ok. It will run on 2 cores, but it will be slow.
* Storage: As you have to store Docker images and data, we suggest at least 25 GB of free disk space.
 

## Setup

### Install docker and docker-compose

Make sure you have docker and docker-compose installed.

You can find more information on installing docker here:

	https://docs.docker.com/get-docker/

If docker-compose does not install `docker-compose` automatically,
you must install it separately. 
To test if ``docker-compose`` ist installed, run

```bash
docker-compose --version
```


You can download `docker-compose` from 
https://docs.docker.com/compose/install/

### Run setup.sh

``./setup.sh`` 

or

``bash setup.sh``

This script will ask a few question and will create a
working configuration for your installation. It can be run 
multiple times if needed. The script will respect an 
existing configuration and willi suggest existing
config values as default valueis. (Not 100% true for
docker-compose.override.yaml, but even this will work
in most cases).

``setup.sh`` will create or modifiy 2 files:

* ``.env``
* ``docker-compose.override.yaml``

It will also create a ``secrets`` folder with some more files,
containing sensitive data like passwords.

If necessary you can edit ``.env`` and ``docker-compose.override.yaml``
by hand. See `configuration files` below for details.

You can also edit the files under ``secrets``, if necessary. 
Take special care with password files: If you change a 
password secret after starting GAMS for at least
once, the changed passwort will not be changed in the database system 
automatically. It's your responsibility to change database password in 
the database server by hand. Normally there is no need to change
existing passwords.

## Running GAMS

To start all necessary containers, enter:

``` 
./start.sh
```

or

```bash
docker-compose up -d
```

`starts.sh` allows to switch to the test realm without
changing the configuration. This is normally only
necessary if you want to run the integretion tests,
which can be found under `tests`.

To stop GAMS:

```bash
docker-compose down
```

or 

```
stop.sh
```

Both commands behave identical.

## Updating

We will provide new versions from time to time. 
At the moment the only way to update you installation is
to update this folder from the repository:

```
git pull
```

It's not always necessary, but to be sure you should always
run `setup.sh` after updating the repository.



## Configuration files

### .env

This file contains some environment variables, which values can be changed 
to fit you needs. After running ``setup.py``, you should have
a working configuration.


### docker.compose.override.yaml

This file contains local modifications of ``docker-compose.yaml``.
Under normal circumstances the only reason to edit this file
is to comment or uncomment ``ports:`` entries to open or close
service ports.
Background: By default no container but apache will be accessible 
directly from your computer or network, which is nice because
you do not have to worry about security (firewalls etc.)
But sometimes (mostly for debugging) you might want to make a service 
acessible directly via a specific port. To do so, open
``docker-compose.override.yaml``, find the desired service definition 
and remove the comments in front of ``ports:`` entries. Eg:

```yaml
  blazegraph:
#     ports:
#       - "4040:8080"
```
After restarting with ``docker-compose up -d``, you now can
interact with blazegraph via http://localhost:4040/

**Warning: Do not open these ports in a production setup! You might get compromized.**

## Resetting data

The simpliest way to get rid of existing data is to delete the volumes 
(which will be recreated on next startup automatically).

1. Shut down GAMS: ``./stop.sh`` or ``docker-compose down``.
2. List existing volume: ``docker volume ls``
3. Remove GAMS volumes. These are (*prefix* is normally the name of 
   the directory where docker-compose.yaml is located):

  - prefix_activemq-vol
  - prefix_blazegraph-vol
  - prefix_fcrepo-vol
  - prefix_loriscache-vol
  - prefix_postgres-vol
  - prefix_proai-vol
  - prefix_rdf4j-vol
  - prefix_solr-vol
  - prefix_triplestore-vol

  You can use ``docker volume prune`` to delete all unused volumes. Make sure
  that there are no volumes from other projects which might be deleted as well
   (if GAMS is your only docker project, you are safe). Or you can delete
  the GAMS volumes by using the ``docker volume rm`` command on each volume 
   listed above. (This is what the ``bin/delete_all_volumes.sh`` script does.
4. Start GAMS with ``./start.sh`` oder ``docker-compose up -d``

