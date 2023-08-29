#!/usr/bin/env bash

set -e
shopt -s extglob

# this version must match the version used in docker-compose
COMPOSE_VERSION="3.4"

# These environment variables must be set (in .env)
# we cannot use associatve arrays because osx uses bash 3 
REQUIRED_DEVICE_VARS=(
	ACTIVEMQ_DATA_DEVICE:activemq
	APACHE_CONFIG_DEVICE:apache_config
	APACHE_DATA_DEVICE:apache
	BLAZEGRAPH_DATA_DEVICE:blazegraph
	COCOON_CONFIG_DEVICE:cocoon
	FCREPO_DATA_DEVICE:fcrepo
	KEYCLOAK_DATA_DEVICE:keycloak
	LORIS_CACHE_DEVICE:loriscache
	RDF4J_DATA_DEVICE:rdf4j
	POSTGRES_DATA_DEVICE:postgres
	PROAI_DATA_DEVICE:proai
	SOLR_DATA_DEVICE:solr
	TRIPLESTORE_DATA_DEVICE:triplestore
	FCREPO_OBJECTSTORE_DEVICE:objectstore
	MINIO_DATA_DEVICE:minio
)

# which device var is used in specific volume definitions?
VOL_DEVICE_MAP=(
	activemq-vol:ACTIVEMQ_DATA_DEVICE
	apache-volume:APACHE_DATA_DEVICE
	apache-config-volume:APACHE_CONFIG_DEVICE
	blazegraph-vol:BLAZEGRAPH_DATA_DEVICE
	cocoon-config-volume:COCOON_CONFIG_DEVICE
	fcrepo-vol:FCREPO_DATA_DEVICE
	loriscache-vol:LORIS_CACHE_DEVICE
	rdf4j-vol:RDF4J_DATA_DEVICE
	postgres-vol:POSTGRES_DATA_DEVICE
	proai-vol:PROAI_DATA_DEVICE
	solr-vol:SOLR_DATA_DEVICE
	triplestore-vol:TRIPLESTORE_DATA_DEVICE
	minio-vol:MINIO_DATA_DEVICE
)


# These volumes should not be put under '/var/lib/docker/volumes'
EXTRA_VOLUMES=(
	apache-volume
	apache-config-volume
	cocoon-config-volume
)

# List of pre-defined values for zim users
ZIM_DEFAULTS=(
	KEYCLOAK_HOST:gams.uni-graz.at
	KEYCLOAK_REALM:Gams
	KEYCLOAK_CLIENT:Cirilo
	GEONAMES_ACCOUNT:acdh
	HANDLE_PREFIX:11471
)

APACHE_DIRNAME_FILE="/tmp/apache_data_dir$RANDOM"
COCOON_DIRNAME_FILE="/tmp/cocoon_dir$RANDOM"

# check if a device var name like "APACHE_DEVICE" is used
# by a volume defined in EXTRA_VOLUMES
is_extra_vol_device() {
	local searched_dev_var=${1}
	local rv=0
	for pair in ${VOL_DEVICE_MAP[@]}; do
		local volname="${pair%%:*}"
		local devvar="${pair##*:}"
		if [[ "${searched_dev_var}" == "${devvar}" ]]; then
			for evn in ${EXTRA_VOLUMES[@]}; do
				if [[ "${volname}" == "${evn}" ]];  then
					rv=1
					break
				fi
			done
		fi
	done
	echo ${rv}
}


# Returns an array of volumes-entries for specific services.
# Add additional volumes here.
# At the moment we have to to specify volumes differently for WSL,
# because using the volume name does not work here
# TODO When WSL supports volumes like in else, the conditions can be removed,
# possibly the whole volumes entries can be set statically in 
# templates/docker-compose-override
function get_service_volumes_for() {
	local service_name="${1}"
	local data_root="${2}"
	local volumes=""

	case ${service_name} in
		apache)
			if [[ "${os_used}" == "wsl" ]]; then
				# only for wsl: for non wsl it is set in function 
				# set_default_volume_dirs / set_none_default_volume_dirs
				# echo "${data_root}/apache" > ${APACHE_DIRNAME_FILE}
				APACHE_DATA_DIR="${data_root}/apache"
				volumes=(
				   "${data_root}/apache:/var/www/html"
				   "${data_root}/apache_config:/etc/apache2/extra-sites:ro"
				)
			else
				volumes=(
				   "apache-volume:/var/www/html"
				   "apache-config-volume:/etc/apache2/extra-sites:ro"  
				)
			fi
			;;
		cocoon)
			if [[ "${os_used}" == "wsl" ]]; then
				volumes=(
				   "${data_root}/cocoon:/data"
				)
			else
				volumes=(
				   "cocoon-config-volume:/data"
				)
			fi
			;;
		activemq)
			volumes=("activemq-vol:/opt/activemq/data");;
		blazegraph)
			volumes=("blazegraph-vol:/shared/blazegraph");;
		fcgate)
			if [[ "${os_used}" == "wsl" ]]; then
				volumes=(
				     "${data_root}/apache:/var/apache"
				)
			else 
				volumes=(
				     "apache-volume:/var/apache"
				)
			fi;;
		fcrepo4)
			volumes=("fcrepo-vol:/var/lib/fcrepo");;
		minio)
			volumes=("minio-vol:/data");;
		loris)
			volumes=("loriscache-vol:/var/cache/loris");;
		rdf4j)
			volumes=("rdf4j-vol:/data");;
		postgres)
			volumes=("postgres-vol:/var/lib/postgresql/data");;
		proai)
			volumes=("proai-vol:/var/proai");;
		triplestore)
			volumes=("triplestore-vol:/shared/blazegraph");;
		solr)
			volumes=("solr-vol:/var/solr");;

		*)
			echo "Unknown service name: ${service_name}" > /dev/stderr
			exit 1
			;;
	esac
	echo $(IFS=, ; echo "${volumes[*]}")
}


# this is a hack because of missing hashtables in bash 3 (osx)
# it will return the value for key 'key' in array 'arr' or "" if key is 
# not found 
# usage: get_value_for_key $key array[@]
get_value_for_key() {
	local val=""
	declare -a map=("${!2}")

	for entry in "${map[@]}"; do
		if [[ "${entry%%:*}" == "${1}" ]]; then
			val="${entry#*:}"
			break
		fi
	done
	echo $val
}

# OSX has no realpath function. As a workaround this should be ok for our purpose.
real_path () {
	# $1 is an absolute path
        if [[ $1 == /* ]]; then
                echo $1
        else
                if [[ $1 == ~* ]]; then
                        echo ${HOME}/${1:2}
                else
                        if [[ $1 == .* ]]; then
                                echo ${PWD}/${1:2}
                        else
                                echo ${PWD}/$1
                        fi
                fi
        fi
}


# Helper function: Prompt user with default value
default_prompt() {
	local question="${1}"
	local default_answer="${2}"
        read -p "==> ${question} [${default_answer}] "
	if [[ -z "${REPLY}" ]]; then
           	echo $default_answer
        else
		echo ${REPLY}
	fi
}

# Helper function: Ask for an absolute path
absolute_path_prompt() {
	local question="${1}"
	local answer=""
	while :; do
		read -p "==> ${1}"
		if [[ "${REPLY}" == /* ]]; then
			answer="${REPLY}"
			break
		fi
	done
	echo "${answer}"
}


# as for a path and check if it starts with an /
default_absolute_path_prompt() {
	local question="${1}"
	local answer="${2}"
	while :; do
		read -p "==> ${question}: [${answer}] "
		if [[ -z "${REPLY}" ]]; then
			break
		else
			if [[ "${REPLY}" == /* ]]; then
				answer="${REPLY}"
				break
			fi
		fi  
	done
	echo "${answer}"
}

# Helper function: Prompt for y/n with default value
# Allows only Enter (default answer), 'yY' or 'nN' as input
default_yes_no_prompt() {
	local question="${1}"
	local answer="${2}"
	while :; do
		read -p "==> ${question}"
		if [[ -z "${REPLY}" ]]; then
			break # use default answer
		fi
		if [[ ${REPLY} == "y" || ${REPLY} == "Y" ]]; then
			answer="y"
			break
		fi
		if [[ ${REPLY} == "n" || ${REPLY} == "N" ]]; then
			answer="n"
			break
		fi
	done
	echo ${answer}
}

# Return a random string of length $1
make_random_string() {
	local s_length=${1:-12}
	echo $(tr -dc A-Za-z0-9\-!%\(\)\$ </dev/urandom | head -c ${s_length})
}

# Extract value for environment variable $1 from env file $2
# if file does not exist, $3 is used as default value
extract_value() {
	local varname=${1}
	local filename="${2}"
	local default_value="${3}"

	if [[ -f ${filename} ]]; then
		local line=$(grep "^${varname}" $filename)
		local rv="$(echo $line | cut -d'=' -f 2)"
	else
		local rv="${default_value}"
	fi
	
	echo ${rv}
}

# return a default value for variable $1
# The default value is extracted from templates/env.
# If user is zim-user and value is set in ZIM-defaults, this
# value will be used.
# If a .env file exists (=user has run this script before),
# the value from this file is prompted as default value.
get_default_value() {
	default_val=$(extract_value "${1}" "templates/env")
	env_val=$(extract_value "${1}" ".env.bak~" "${default_val}")
	# if both values are equal, this might be the initial run
	# in this case we can check for zimgams as special value
	if [[ "${env_val}" == "${default_val}" ]]; then
		local zim_val=$(get_value_for_key ${1} ZIM_DEFAULTS[@])
		if [[ ! -z "${zim_val}" ]] && [[ "${IS_ZIM_USER}" == "y" ]]; then
			echo "${zim_val}"
		else
			echo "${env_val}"
		fi
	else # .env has been changed: we use this value
		echo "${env_val}"
	fi
}

# Helper function: Guess if we are on OSX, Windows or Linux 
# Returns 'osx', 'wsl' or 'linux'
detect_os() {
	o=$(tr '[:upper:]' '[:lower:]' <<< $(uname -a))
	local rv=""
	if [[ "${o}" =~ "Darwin" ]]; then
		rv="osx"
	else
		if [[ ${o} =~ "microsoft" ]]; then
			rv="wsl"
		else
			rv="linux"
		fi
	fi
	echo ${rv}
}

# ask user for value of PUBLIC_HOST
# sets the hostname in .env and returns the entered name
set_public_host() {
	if [[ ${QUIET_MODE} == 0 ]]; then
		reset
		cat <<- EOF > /dev/stderr
			Configure host name
			-------------------

			Set the public host name. e.g. 'myrepo.example.com' or 'localhost'.
			This is the name under which you will access your installation.
			Important: This name must be resolvable via DNS. This is why
			you cannot just make up a name.
			

			EOF
		local answer=$(default_prompt "Public host name" "$(get_default_value "PUBLIC_HOST")")
	else
		local answer=$(get_default_value "PUBLIC_HOST")
	fi
	sed -i"" "s/PUBLIC_HOST=.*/PUBLIC_HOST=${answer}/" .env 
	echo ${answer}
}


# Ask user for value of KEYCLOAK_HOST
# TODO: Describe how to activate and configure the keycloak server?
set_keycloak_host() {
	if [[ "${QUIET_MODE}" == 0 ]]; then
		reset
		cat <<- EOF > /dev/stderr
			Configuring keycloak
			--------------------

			For authentication you need to access a keycloak server with a realm 
			configured like described in the documentation. 

			If your institution does not have such a server, you could use the one 
			provided in this package. To do so remove the comment from the keycloak 
			stanzas in 'docker-compose.yaml' and 'docker-compose.override.yaml'

			EOF

		local keycloak_host=$(default_prompt "Keycloak host:" "$(get_default_value "KEYCLOAK_HOST")")
	else
		local keycloak_host=$(get_default_value "KEYCLOAK_HOST")
	fi
	sed -i"" -e "s/KEYCLOAK_HOST=.*/KEYCLOAK_HOST=${keycloak_host}/" .env 
}

# Ask user for value of GEONAMES_ACCOUNT
set_geonames_account() {
	if [[ ${QUIET_MODE} == 0 ]]; then
		reset
		cat <<- EOF > /dev/stderr
			Configuring geonames account
			----------------------------

			Set your geonames.org account name.
			If you do not have an account yet, register (for free) at 
			https://www.geonames.org/login

			EOF

		local answer=$(default_prompt "Geonames account:" "$(get_default_value "GEONAMES_ACCOUNT")")
	else
		local answer="$(get_default_value "GEONAMES_ACCOUNT")"
	fi

	sed -i"" -e "s/GEONAMES_ACCOUNT=.*/GEONAMES_ACCOUNT=${answer}/" .env 
}


# Ask user for value of HANDLE_PREFIX
set_handle_prefix() {


	if [[ ${QUIET_MODE} == 0 ]]; then
		ask_if_handle
		reset
		cat <<- EOF > /dev/stderr
			Configuring handle
			------------------

			If you would like to use handles (persistent identifiers), enter
			your handle prefix here. You can optain a handle prefix from
			https://www.handle.net/prefix.html

			You should not publish handles of non productive setups to the
			handle system, so settings handles in a non productive environment 
			does not make much sense. 
			
			You might want to skip this step unless you are configuring a 
			production server.

			If you do not want to use handles, leave this empty.
			EOF

		local answer=$(default_prompt " prefix:" "$(get_default_value "HANDLE_PREFIX")")
	else
		local answer=$(get_default_value "HANDLE_PREFIX")
	fi
	sed -i"" -e "s/HANDLE_PREFIX=.*/HANDLE_PREFIX=${answer}/" .env 
}

ask_if_handle() {
	reset
	cat <<- EOF > /dev/stderr
		Use handles?
		------------
			
		Answer 'y' if you would like to let GAMS set handles for you.

		Handles are persistent identifiers provided by the handle system
		(http://handle.net/). Please note, that using handles requires

		    * an active handle prefix (which you have to pay for)
		    * write access to a handle server (normally hosted by you or your 
		      organisation)

		It is really bad practice to publish handles from non productive setups,
		so you might want to answer with 'n' unless you are configuring a 
		production server and have write access to a handle server.

		EOF


	local answer=$(default_yes_no_prompt "Use the handle system? (y/N):" "n")
	if [[ "${answer}" == "y" ]]; then
		cat <<- EOF >> /dev/stderr

			Remember to copy hour handle key file, which you will get from your handle
			administrator to the secrets folder. 
			The name of this file must be "hdladmpriv.bin". It must contain a valid 
			handle key, otherwise authentication against the handle server 
			will not work!

			EOF

		read -p "Press any key to continue"
	fi

	echo "${answer}"
}

create_handle_secret() {
	# make sure the handle key secret exists
	if [[ ! -f secrets/hdladmpriv.bin ]]; then 
		echo "THIS IS A DUMMY HANDLY KEY FILE" > secrets/hdladmpriv.bin
		echo "Replace this file with the one you got from your handle admin!" >> secrets/hdladmpriv.bin
	fi
}

# write paths for all volumes to .env
# this function is used if use_default_volumes == 'y'
set_default_volume_dirs() {
	local root_dir="${1}"
	local buffer=""
	for pair in ${REQUIRED_DEVICE_VARS[*]}; do
		local varname="${pair%%:*}"
		local value="${pair##*:}"
		local fulldir="${root_dir}/${value}"
		# We need APACHE_DATA_DIR as global var
		if [[ ${varname} == "APACHE_DATA_DEVICE" ]]; then
			#echo ${fulldir} > ${APACHE_DIRNAME_FILE}
			APACHE_DATA_DIR=${fulldir}
		fi
		# WE NEED COCOON_DATA_DIR as global var
		if [[ ${varname} == "COCOON_CONFIG_DEVICE" ]]; then
			#echo ${fulldir} > ${COCOON_DIRNAME_FILE}
			COCOON_CONFIG_DIR=${fulldir}
		fi
		if [[ "$(is_extra_vol_device ${varname})" == "1" ]]; then
			mkdir -p "${fulldir}"
			buffer="${buffer}\n${varname}=${fulldir}"
		fi
	done
	sed -i"" -e "s,\[\[VOLUMES\]\],${buffer}," .env
}


# write paths for all volumes to .env
# this function is used if use_default_volumes == 'n'
set_none_default_volume_dirs() {
	local root_dir="${1}"
	local buffer=""
	for pair in ${REQUIRED_DEVICE_VARS[*]}; do
		local varname="${pair%%:*}"
		local value="${pair##*:}"
		local fulldir="${root_dir}/${value}"
		# We need APACHE_DATA_DIR as global var
		if [[ ${varname} == "APACHE_DATA_DEVICE" ]]; then
			#echo ${fulldir} > ${APACHE_DIRNAME_FILE}
			APACHE_DATA_DIR="${fulldir}"
		fi
		# We need COOCON_DATA_DIR as global var
		if [[ ${varname} == "COCOON_CONFIG_DEVICE" ]]; then
			#echo ${fulldir} > ${COCOON_DIRNAME_FILE}
			COCOON_CONFIG_DIR="${fulldir}"
		fi
		mkdir -p "${fulldir}"
		buffer="${buffer}\n${varname}=${fulldir}"
	done
	sed -i"" -e "s,\[\[VOLUMES\]\],${buffer}," .env
}


# ask if default volumes should be used
ask_if_default_volumes() {
	# For now there is an issue with WSL 2 which forces WSL users to use
	# default volumes.
	if [[ "${os_used}" == "wsl" ]]; then
		echo "y"
	else
		reset
		cat <<- EOF > /dev/stderr
			Configuring volumes
			-------------------

			For running GAMS in a local test or development setup, we suggest 
			to keep volumes in the default location (/var/lib/docker/volumes).

			Please note: Even if you choose to use default volumes, a few volumes
			will be put in a 'data' directory relative to your 'docker-compose.yaml'
			file. This is to facilitate access for you.

			EOF

		local use_default_volumes=$(default_yes_no_prompt "Use the default volume configuration? (Y/n)" "y")	
		echo ${use_default_volumes}
	fi
}

ask_for_common_data_root() {
	local default_root=${1}
	cat <<- EOF > /dev/stderr

	Enter the path to a directory under which volume data will be stored.
	If this directory does not exist, it will be created.

	EOF
	local vol_path=$(default_absolute_path_prompt "Enter the abolute path to your data dir" "${default_root}")
	mkdir -p ${vol_path}
	echo ${vol_path}
}

ask_if_open_ports() {
	# TODO not optimal, but it's urgent
#	if [[ ${QUIET_MODE} == 0 ]]; then
		reset
		cat <<- EOF > /dev/stderr
			Configuring open ports
			----------------------

			For debugging purposes it might come in handy if some services
			are accessible directly via a specific port. 

			This requires to open some ports on your computer, which gives unrestricted 
			access to these services (and your data) for anyone who can access your 
			computer over a network. Be aware that Docker will even automatically open 
			these ports on your (Linux) firewall.
			
			You should never open these ports on a server.  Even on local installations
			you should consider to keep them closed and only open them selectively 
			for debugging by editing the 'docker-compose.override.yaml' file.

			In short: You might want to answer with 'n'.

			EOF

		local open_ports=$(default_yes_no_prompt "Make services directly accessible? (y/N)" "n")
#	else
#		local open_ports="n"
#	fi
	echo ${open_ports}
}


# make a backup of docker-compose.override.yaml if it exists
backup_override() {
	if [[ -f docker-compose.override.yaml ]]; then
		mv docker-compose.override.yaml docker-compose.override.yaml.bak
		if [[ ${QUIET_MODE} == 0 ]]; then
			reset
			echo "Made a backup of existing 'docker-compose.override.yaml':"
			echo "'docker-compose.override.yaml.bak'"
			echo ""
			read -p "Press any key to continue "
		fi
	fi
}


# Create docker-compose.override.yaml
create_override() {
	#spaces/tabs per indention level (MUST be consistent through levels!)
	local INDENTION=2

	local use_default_volumes=${1}
	local open_ports=${2}
	local data_root=${3}

	backup_override

	# hierarchy levels are represented by an array
	# so services: apache: ports: becomes (services apache ports)
	local contexts=()
	
	local in_ports=0
	while IFS= read -r line; do
		# empty lines comment lines do not need processing
		if [[ -z ${line} || ${line} =~ ^[[:space:]]*#.* ]]; then
			echo "" >> docker-compose.override.yaml
		else
			# update contexts
			if [[ $line =~ ^([[:space:]]*)([a-zA-Z0-9\-]+):[[:space:]]*$ ]]; then
				indent=${#BASH_REMATCH[1]}
				c_index=$(($indent / $INDENTION))
				# remove all array entries below current c_index
				local buf=()
				for i in "${!contexts[@]}"; do
					if [ $i -le ${c_index} ]; then
						buf[$i]="${contexts[$i]}"
					fi
				done	
				contexts=("${buf[@]}")
				# add new/replace existing array entry
				contexts[$c_index]=${BASH_REMATCH[2]}
			fi
			# commment out ports config if open_port == 'n'
			if [[ ${open_ports} == "n" && "${contexts[0]}" == "services" && "${contexts[2]}" == "ports" ]]; then
				# port 443:443 must not be commented for normal installs; also not the ports directive
				if [[ $line =~ ^[[:space:]]+-[[:space:]]\"443:443\"[[:space:]]*$ ]] || [[ "${contexts[1]}" == "apache" ]]; then
					echo "$line" >> docker-compose.override.yaml
				else
					echo "# $line" >> docker-compose.override.yaml
				fi
				continue
			fi
			# create custom volume entries in service definitions
			# these are the volumes like apache_data
			# TODO: this can be moved to docker-compose.override.yaml, as soon as WSL issue is solved
			if [[ "${contexts[0]}" == "services" && "${contexts[2]}" == "volumes" ]]; then
				# get_service_volumes_for return a string with values separated by ','
				local volumes_str=$(get_service_volumes_for ${contexts[1]} ${data_root})
				local volumes=$(echo ${volumes_str} | tr "," "\n")
				echo "    volumes:" >> docker-compose.override.yaml
				for volume in ${volumes}; do
					echo "      - ${volume}" >> docker-compose.override.yaml
				done
				continue
			fi
			if [[ ${use_default_volumes} == "n" ]]; then
				# insert custom volume creation code for each defined volume in the
				# volumes section
				if [[ "${contexts[0]}" == "volumes" && ! -z ${contexts[1]} ]]; then
					#vol_device=${VOL_DEVICE_MAP[${contexts[1]}]}
					vol_device=$(get_value_for_key ${contexts[1]} VOL_DEVICE_MAP[@])
					#echo "$line ${contexts[1]} ${vol_device}"
					echo "$line" >> docker-compose.override.yaml
					echo "    driver_opts:" >> docker-compose.override.yaml
					echo "      type: none" >> docker-compose.override.yaml
					echo "      device: \${${vol_device}}" >> docker-compose.override.yaml
					echo "      o: bind" >> docker-compose.override.yaml
					echo "" >> docker-compose.override.yaml
					continue
				fi
			fi
			# default
			echo "$line" >> docker-compose.override.yaml
		fi
	done < templates/docker-compose.override.yaml

	# When user selected non default volumes we have to add some extra volume
	# But only if not in WLS, because in WLS we had to add these directly in 
	# service.servive.volumes
	if [[ "${os_used}" != "wsl" ]]; then
		for volname in ${EXTRA_VOLUMES[*]}; do
			local voldevice=$(get_value_for_key ${volname} VOL_DEVICE_MAP[@])
			echo "" >> docker-compose.override.yaml
			echo "  ${volname}:" >> docker-compose.override.yaml
			echo "    driver_opts:" >> docker-compose.override.yaml
			echo "      type: none" >> docker-compose.override.yaml
			echo "      device: \${${voldevice}}" >> docker-compose.override.yaml
			echo "      o: bind" >> docker-compose.override.yaml
		done
	fi
}


# prompt for the postgres_pw secret if does not exist
create_pg_secret() {
	if [[ ! -f secrets/postgres_pw.txt ]]; then 
		mkdir -p secrets
		reset
		cat <<- EOF >> /dev/stderr
			Configuring postgresql
			----------------------

			Set the password for accessing the postgres server.
			You can enter a custom password or or use a autogenerated one.
			Normally you will never have to deal with this password, so
			using the autogenerated will be fine.

			EOF
		local default_pw=$(make_random_string 14)
		local pg_pw=$(default_prompt "Choose a postgres password" "${default_pw}")
		echo "${pg_pw}" > secrets/postgres_pw.txt
	else
		if [[ ${QUIET_MODE} == 0 ]]; then 
			reset
			cat <<- EOF >> /dev/stderr
				Configuring postgresql
				----------------------

				echo "'secrets/postgres_pw.txt' exists. Leaving it untouched." > /dev/stderr
				echo ""
				read -p "Press Enter to proceed"

				EOF
		fi
	fi
}

configure_idc() {
	#TODO: Mabe set this use_idc to 0 and only print
        # info that it can be configured?
	echo 'foo'
}


# prompt for the keykloak_pw secret if does not exist
create_keycloak_secret() {
	if [[ ! -f secrets/keycloak_pw.txt ]]; then 
		mkdir -p secrets
		reset
		cat <<- EOF >/dev/stderr
			Configuring keycloak
			--------------------
				
			EOF
		local default_pw=$(make_random_string 14)
		local pg_pw=$(default_prompt "Enter the keycloak admin password (or accept the auto generated" "${default_pw}")
		echo "${pg_pw}" > secrets/keycloak_pw.txt
	else
		if [[ ${QUIET_MODE}  == 0 ]]; then
			reset
			cat <<- EOF >/dev/stderr
				Configuring keycloak
				--------------------

				'secrets/keycloak_pw.txt' exists. Leaving it untouched."

				Press Enter to proceed

				EOF
		fi
	fi
}



create_apache_keycloak_secret() {
	if [[ ! -f secrets/apache_keycloak_secret.txt ]]; then
		mkdir -p secrets
		reset
		cat <<- EOF >> /dev/stderr
			Configuring the keycloak secret for apache 
			------------------------------------------

			On production systems some endpoint should only be accessible for logged 
			in users with specific roles. This is based on tokens provided by the 
			keycloak server. This secret will allow apache to communicate with the 
			keycloak server. 
			For a test or development setup (this is a setup where 'USE_IDC' is set 
			to '0') this secret will not be used.

			I will create a dummy secret 'secrets/apache_keycloak_secret.txt', which 
			must be replaced for production systems by the secret you can get from 
			your keycloak admin. This is as easy as replacing the file by a new one.


			EOF
		read -p "Press Enter to continue"

	else
		if [[ ${QUIET_MODE} == 0 ]]; then
			reset
			cat <<- EOF > /dev/stderr
				Configuring the keycloak secret for apache 
				------------------------------------------

				'secrets/apache_keycloak_secret.txt' already exists. Leaving it untouched."

				EOF
			read -p "Press Enter to continue"
		fi
	fi


	if [[ ! -f secrets/apache_keycloak_secret.txt ]]; then
		cat <<- EOF > secrets/apache_keycloak_secret.txt
			THIS IS A DUMMY FILE. 
			Replace it with a valid secret if you set 'USE_IDC' ro '1'.
			Ask your keycloak admin for it.
			It can be found on the keycloak server under 
			clients -> Apache -> credentials

			EOF
	fi
}

create_oidc_passphrase_secret() {
	if [[ ! -f secrets/oidc_passphrase.txt ]]; then
		mkdir -p secrets
		reset
		cat <<- EOF > /dev/stderr
			Configuring the oicd passphrase secret 
			--------------------------------------

			Set a server specific unique phrase for communication between
			apache and keycloak.
			You can enter a custom phrase or or use a autogenerated one.
			Normally you will never have to deal with this phrase, so
			using the autogenerated will be fine.

			EOF

			local default_pw=$(make_random_string 128)
			local oicd_phrase=$(default_prompt "Choose a passphrase for OICD" "${default_pw}")
			echo "${oicd_phrase}" > secrets/oidc_passphrase.txt
	else
		if [[ ${QUIET_MODE} == 0 ]]; then
			reset
			cat <<- EOF > /dev/stderr
				Configuring the oicd passphrase secret 
				--------------------------------------

				'secrets/oicd_passphrase.txt' exists. Leaving it untouched." > /dev/stderr

				EOF
			read -p "Press Enter to proceed"
		fi
	fi
}


set_keycloak_realm() {
	if [[ ${QUIET_MODE} == 0 ]]; then
		reset
		cat <<- EOF > /dev/stderr
			Configuring keycloak realm
			--------------------------

			EOF

		cat <<- EOF > /dev/stderr
			For authentication you have to provide the keycloak realm to use.
			This is something which has to be configured on your keycloak server.

			If you do not know the keycloak realm yet, you can set the correct 
			value later in .env" 

			EOF
		local realm=$(default_prompt "Enter the realm" "$(get_default_value "KEYCLOAK_REALM")")
	else
		local realm="$(get_default_value "KEYCLOAK_REALM")"
	fi
	sed -i"" -e "s/KEYCLOAK_REALM=.*/KEYCLOAK_REALM=${realm}/" .env 
}

set_keycloak_client() {
	if [[ ${QUIET_MODE} == 0 ]]; then
		reset
		cat <<- EOF > /dev/stderr
			Configuring keycloak client
			---------------------------

			EOF

		cat <<- EOF > /dev/stderr
			For authentication you have to provide the keycloak client 
			(also known as 'resource') to use.
			This is something which has to be configured on your keycloak server.

			If you do not know the keycloak client yet, you can set the correct 
			value later in .env" 

			EOF
		local client=$(default_prompt "Enter the client name" "$(get_default_value "KEYCLOAK_CLIENT")")
	else
		local client="$(get_default_value "KEYCLOAK_CLIENT")"
	fi
	sed -i"" -e "s/KEYCLOAK_CLIENT=.*$/KEYCLOAK_CLIENT=${client}/" .env 
}

# Generate a ssl certificate
create_certificate() {
	local hostname="${1}"
	if [[ ! -f secrets/server.crt ]] && [[ ! -f secrets/server.key ]]; then
		mkdir -p secrets
		reset
		cat <<- EOF >> /dev/stderr
			Creating TLS Certificate
			------------------------

			Accessing the server via https requires a certificate.

			For development and testing we can use a self signed certificate.
			Remember that you have to accept this certificate once in your browser.
			(Your browser will tell you, that it is unsecure, but this is ok for 
			this use case). 

			DO NOT USE A SELF SIGNED CERTIFICATE IN A PRODUCTION ENVIRONMENT!

			If you want to use an officially recogniced certificate, you have to 
			copy the cert and key files you created/got from your certificate
			authority to the secrets folder. The filenames must match the 
			existing file names (server.key, server.crt, rootCA.crt), so
			rename the files if necessary.

			EOF

		create_cert=$(default_yes_no_prompt "Create a self signed certificate for '${hostname}'? (Y/n) " "y")
		if [[ ${create_cert} == "y" ]]; then
			echo "creating certificate for '${hostname}'"
			openssl req -x509 -out secrets/server.crt -keyout secrets/server.key \
			  -newkey rsa:2048 -nodes -sha256 -days 3650 \
			  -subj "/CN=${hostname}" -extensions EXT -config <( \
			  printf "[dn]\nCN=${hostname}\n[req]\ndistinguished_name = dn\n[EXT]\nsubjectAltName=DNS:${hostname}\nkeyUsage=digitalSignature\nextendedKeyUsage=serverAuth")
		else 
			echo "Did not create certificates. You must copy your certificate to the secrets folder by hand!"
		fi
	else
		if [[ ${QUIET_MODE} == 0 ]]; then
			reset
			cat <<- EOF >> /dev/stderr
				Creating TLS Certificate
				------------------------

				'secrets/server.crt' and 'secrets/server.key' already exist. Leaving them untouched."

				EOF
			read -p "Press Enter to proceed"
		fi
	fi

	if [[ ! -f secrets/rootCA.crt ]]; then
		echo "NOT USED" > secrets/rootCA.crt
		local msg="Creating a dummy rootCA.crt"
	else
		local msg="secrets/rootCA.crt exists. Leaving in untouched."
	fi
	if [[ ${QUIET_MODE} == 0 ]]; then
		echo ${msg}
	fi
}

# ZIM users have some different default values
# This will set a global IS_ZIM_USER to 'y' or 'n'
ask_for_zim_user() {
	reset
	cat <<- EOF > /dev/stderr
		Create a configuration for GAMS
		-------------------------------

		This script will generate an (initial) configuration for GAMS.

		It will create/modify 2 files:

		* .env
		* docker-compose.override.yaml

		Both files can be edited afterwards with a text editor, if necessary.


		For internal use we have added a ZIM-staff option, which will autoconfigure
		some settings.  Do note use this feature, if you are not working at ZIM. 
		It will not work for you!

		Again: Do NOT answer the next question with 'y' if you are not running the 
		installation in a ZIM context!

		EOF

	IS_ZIM_USER=$(default_yes_no_prompt "Are you configuring a ZIM setup? (y/N) " "n")
}


# Make changes to existing .env before running the script
# This is the place where existing things can be fixed. Eg.:
#    - changed/defunct environment parameter names 
#    - defunct secrets
function update_hook() {
	# marmotta has been replaced
	if [[ -f "secrets/marmotta_pw.txt" ]]; then
		rm secrets/marmotta_pw.txt
	fi
	# apache http dir now must be writeable from fcgate
	echo $APACHE_DATA_DEVICE
}

function show_help() {

	cat <<- EOF >> /dev/stderr
		Create or update configuration für gams4.

		Usage: setup.sh [OPTIONS]

		Options:
		  -h, --help     Show this help text
		  -q, --quiet    Run in quiet mode (using default values)
		  -z, --zim      Use default configuration for ZIM specific setup

		EOF
	  exit 0
}


# helper function: clone/pull a repo
function clone_or_update() {
	local target_dir=${1}
	local git_url=${2}
	if [[ ! -d ${target_dir} ]]; then
		git clone ${git_url} ${target_dir}
	else
		# in case target_dir is not empty but not a git repo
		if [[ ! -d "${target_dir}/.git"  ]]; then
			mv ${target_dir} ${target_dir}.bak
			git clone ${git_url} ${target_dir}
		else
			# repo exists, but might need a pull
			(cd ${target_dir} && git pull)
		fi
	fi
}

# Zim users will need the lib folder for development, so we fetch it via git
function update_libs() {
	local cocoon_config_url="https://zimlab.uni-graz.at/gams/backend/cocoon-assets.git"
	local zim_assets_url="https://zimlab.uni-graz.at/gams/frontend/assets/gams-assets.git"


	local gams_asset_dir="${APACHE_DATA_DIR}/assets" 

	clone_or_update ${gams_asset_dir} ${zim_assets_url}
	clone_or_update ${COCOON_CONFIG_DIR} ${cocoon_config_url}

}

## main

update_hook

mkdir -p secrets

if [[ -f .env ]]; then
	mv .env .env.bak~
fi
cp templates/env .env

QUIET_MODE=0
IS_ZIM_USER="n"
# parse command line args
for flag in "$@"; 
do
	case "${flag}" in
		-z|--zim) IS_ZIM_USER="y";;
		-q|--quiet) QUIET_MODE=1;;
		-h|--help) show_help;;
	esac
done

os_used=$(detect_os)

if [[ ${IS_ZIM_USER} == "n" ]]; then
	ask_for_zim_user
fi

use_default_volumes=$(ask_if_default_volumes) 
if [[ ${IS_ZIM_USER} == "y" ]]; then
	zim_data_root="$(cd "$(dirname ../gams-data)"; pwd -P)/$(basename "gams-data")"
	data_root=$(ask_for_common_data_root "${zim_data_root}")
else
	data_root=$(ask_for_common_data_root "${PWD}/data")
fi
mkdir -p ${data_root}

if [[ "${use_default_volumes}" == "n" ]]; then
	set_none_default_volume_dirs "${data_root}" 
else 
	if [[ x${os_used} != x"wsl" ]]; then
		set_default_volume_dirs "${data_root}"
	else
	    # IF WSL2 env the volume dirs must be created in this way
	    APACHE_DATA_DIR=${data_root}/apache
		COCOON_CONFIG_DIR=${data_root}/cocoon
		mkdir -p ${APACHE_DATA_DIR}
		mkdir -p ${data_root}/apache_config
		mkdir -p ${COCOON_CONFIG_DIR}
		sed -i -e "s,\[\[VOLUMES\]\],," .env
	fi
fi

public_host=$(set_public_host)
set_keycloak_host
set_keycloak_realm
set_keycloak_client

set_geonames_account

set_handle_prefix

use_open_ports=$(ask_if_open_ports)
create_override "${use_default_volumes}" "${use_open_ports}" "${data_root}"
create_pg_secret
create_keycloak_secret
create_apache_keycloak_secret
create_oidc_passphrase_secret
create_handle_secret
create_certificate "${public_host}"
if [[ ${IS_ZIM_USER} == "y" ]]; then
	update_libs
fi

if [[ ${os_used} == "wsl" ]]; then
	sed -i"" -e "s,\[\[VOLUMES\]\],," .env
fi

cp templates/apache_readme.txt ${APACHE_DATA_DIR}/README.txt

rm -f ${PWD}/.env.bak~
## this might cause issues on osx (and propably WSL),
## but secrets should be kept secret
#chmod 700 secrets
##if [[ $EUID -eq 0 ]]; then
##    chown root:root secrets
##fi
echo ""
echo "Done."
echo "You can start GAMS with 'start.sh' or 'docker-compose up -d'."
echo "Your configuration can be changed in .env and docker-compose.override.yaml."
echo "Add your custom static files to ${APACHE_DATA_DIR}"
