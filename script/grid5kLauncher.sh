#!/bin/bash

if [ ${#} -lt 3 ]
then
  echo 'grid5kLaucher: usage: this script need at least three parameters: cluster name, number of server and number of clients, ex ./grid5kLauncher.sh nancy 1 1 grenoble 1 2 lille 2 2'
  exit
fi

trap "stopExecution"  SIGINT SIGTERM

path=$(pwd)

function stopExecution(){
        
	echo "grid5kLaucher: grid5kLaucher stop. Deleting jobs..."
	oargriddel $RES_ID
        echo "grid5kLaucher: done"

        exit
}

sed -ie "s#^source.*#source $path/configuration.sh#g" clauncher.sh
sed -ie "s#^source.*#source $path/configuration.sh#g" client.sh 
sed -ie "s#^source.*#source $path/configuration.sh#g" experience.sh
sed -ie "s#^source.*#source $path/configuration.sh#g" jessy.sh
sed -ie "s#^source.*#source $path/configuration.sh#g" launcher.sh
sed -ie "s#^scriptdir=.*#scriptdir=$path#g" configuration.sh


clustersNumber=$(($# / 3))

declare -a param=("$@")
next=0
i=0
reservation="";
for i in `seq 1 $clustersNumber`;
	do
		clusters[$i]=${param[$next]}
		nodes=$((${param[$next+1]}+${param[$next+2]}))
		reservation="$reservation ${param[$next]}:rdef=/nodes=$nodes,"

		next=$(($next+3))
		i=$(($i+1))
        done

reservation=${reservation#?}
reservation=${reservation%?}

echo "starting grid5kLaucher..."$nodes
echo ""
echo "reserving nodes..."
oargridsub -t allow_classic_ssh -w '0:20:00' $reservation > tmp
echo "done"


#retreving batch and grid reservation IDs
RES_ID=$(grep "Grid reservation id" tmp | cut -f2 -d=)
OAR_JOB_KEY_PATH=$(grep "SSH KEY" tmp | cut -b 25-)
#BATCH_ID=$(grep "batchId" tmp | cut -f2 -s -d=)
#BATCH_ID=${BATCH_ID//[[:space:]]/}

rm myfractal.xml

echo '<?xml version="1.0" encoding="ISO-8859-1" ?>' >> myfractal.xml
echo '<FRACTAL>'  >> myfractal.xml
echo '<BootstrapIdentity>' >> myfractal.xml
echo '<nodelist>' >> myfractal.xml

echo 'Grid reservation id: ' $RES_ID

nodes='' #'nodes=('
servers='' #'servers=('
clients='' #'clients=('


j=0
next=0
for i in `seq 1 $clustersNumber`;
do
        nodes=$((${param[$next+1]}+${param[$next+2]}))
        reservation="$reservation ${param[$next]}:rdef=/nodes=$nodes,"

	nodeName=${param[$next]}
	serverNumber=${param[$next+1]}
        clientNumber=${param[$next+2]}

	echo ""
	echo "**********************"
	echo "* deploy on "$nodeName" *"
	echo "**********************"
	echo "server: "$serverNumber
	echo "client: "$clientNumber
	echo ""

	oargridstat -w -l $RES_ID -c $nodeName | sed '/^$/d' | sort | uniq > ./machines

        next=$(($next+3))
	k=0
	while read line
	do
		host $line > tmp
		name=$(cut tmp -f1 -d ' ')
		ip=$(cut tmp -f4 -d ' ')
 
		nodes="$nodes \"$name\""
		if [ $k -lt $serverNumber ]
		then
			echo 'grid5kLaucher. server: '$name
   			echo '<node id="'$j'" ip="'$ip'"/>' >> myfractal.xml
			servers="$servers \"$name\""
		else
		    echo 'grid5kLaucher. client: '$name
		    clients="$clients \"$name\""
		fi
		j=$((j+1))
		k=$((k+1))
	done < machines
done
echo ""

nodes="nodes=("$nodes")"
servers="servers=("$servers")"
clients="clients=("$clients")"

echo '</nodelist>' >> myfractal.xml
echo '</BootstrapIdentity>' >> myfractal.xml
echo '</FRACTAL>' >> myfractal.xml
echo "fractal configuration file is done"

sed -i "s/nodes=.*/${nodes}/g" configuration.sh
sed -i "s/servers=.*/${servers}/g" configuration.sh
sed -i "s/clients=.*/${clients}/g" configuration.sh
echo "configuration.sh file is done"

#echo "sleeping 600 sec before remove machines and tmp"
#sleep 600
#echo "getUp"

rm machines tmp

export OAR_JOB_KEY_FILE=$OAR_JOB_KEY_PATH

echo 'grid5kLaucher: exported oarJobKeyFile ' $OAR_JOB_KEY_PATH


echo 'grid5kLaucher: synchronizing keys and data...'
next=0
for i in `seq 1 $clustersNumber`;
do
        nodeName=${param[$next]}
	echo "synchronizing "$nodeName

	rsync -a -f"+ */" -f"- *" ../../jessy/scripts $nodeName.grid5000.fr:~/jessy	

#	sync --delete -avz ~/.ssh --exclude known_hosts $nodeName.grid5000.fr:
#	rsync --delete -avz ../../jessy $nodeName.grid5000.fr:~/
	rsync --delete -az ./* $nodeName.grid5000.fr:~/jessy/scripts/

	next=$(($next+3))
done

#rsync --delete -avz ~/.ssh --exclude known_hosts lille.grid5000.fr:
#rsync --delete -avz ./* lille.grid5000.fr:./jessy/scripts/
echo 'grid5kLaucher: done'

#echo "sleeping 600 sec before run experience"
#sleep 600
#echo "getUp"

echo ""
echo "******************************************************************************"
echo "grid5kLaucher: myfractal and configuration.sh are done, lauching experience..."
echo "******************************************************************************"

./experience.sh
echo "******************************************************************************"
echo "grid5kLaucher: done, deleting jobs"
echo "******************************************************************************"

oargriddel $RES_ID