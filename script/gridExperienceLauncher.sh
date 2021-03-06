#!/bin/bash

#sed -ie "s#^source.*#source $path/gridExperienceConfiguration.sh#g" gridExperienceConfiguration.sh
source gridExperienceConfiguration.sh


trap "stopExecution"  SIGINT SIGTERM

path=$(pwd)

function stopExecution(){
        
	read -p "the execution of Grid5kLauncher was interrupted. Interrupt the set of experiments? " -n 1 -r
if [[ $REPLY =~ ^[Yy]$ ]]
then
	echo ""
   exit 
fi
echo ""
}

function run {

		servers=`seq ${minServers} ${serverIncrement} ${maxServers}`
		#iterate over servers
		for s in ${servers}; 
		do
			#echo $s" servers"
			cLen=${#clusterInUse[@]}

			if [[ "$avoidUnbalancedRuns" == "true" && $s -lt $cLen ]]; then
				echo "    WARNING skipping run with "$s" servers on "$cLen " clusters"
				iterations=$(( $iterations+1 ))
			else
				serverPerCluster=$(( $s/$cLen ))
				clients=`seq ${minClientsForEachServer} ${clientIncrement} ${maxClientsForEachServer}`
				#iterate over clients
				for c in ${clients}; 
				do
					rest=$(( $serverPerCluster*$cLen ))
					rest=$(( $s-$rest ))

					if [[ "$avoidUnbalancedRuns" == "true" &&  $rest -gt 0 ]]; then
						echo "    WARNING skipping unbalanced run with "$s" servers on "$cLen " clusters"
						iterations=$(( $iterations+1 ))
					else
						for ciu in "${clusterInUse[@]}"
						do
							serverForThisLaunch=$serverPerCluster
							if [ $rest -gt 0 ]; then
								((serverForThisLaunch++))
								rest=$(( $rest - 1 ))
							fi
							clientForCluster=$(( $c*$serverForThisLaunch ))
							if [[ $clientForCluster == 0 && $serverForThisLaunch == 0 ]]; then
								clientForCluster=1;
								echo "    Running one extra client in "$ciu" to avoid run with 0 servers and 0 clients"
							fi
							launchCommand="$launchCommand $ciu $serverForThisLaunch $clientForCluster"
						done
					fi
					iterations=$(( $iterations+1 ))
					echo "    calling grid5kLauncher on " $launchCommand
					echo "**************************************************************************************************"
					echo ""
					echo ""

					./grid5kLauncher.sh $launchCommand
					echo ""
					echo ""
					if [ $iterations -gt $totalCombinations ]; then
						echo "***************************************** FINISH :) ***********************************************"
					else
						echo "******************************** Grid Experience: run " $iterations " of " $totalCombinations " *********************************"
						echo "    deployCluster, consistency: "$cons", threads:"$t
					fi
				launchCommand=""
				done
			fi
		done
}

function deployCluster {

if [ "$varyClusterDeployment" == "true" ]; then
#iterate over clusters
	i=-1
	for cluster in "${clusters[@]}"
	do
		i=$(( $i+1 ))
		#clusters used for this execution
		clusterInUse[$i]=$cluster
		run
	done
else
	clusterInUse=("${clusters[@]}")
	run
fi
}

iterations=1

consistencyCombinations=${#consistency[@]}

threadCombinations=$(( $maxClientsThread - $minClientsThread +1 ))

serverCombinations=$((  $maxServers - $minServers + 1 ))
serverCombinations=$(( $serverCombinations /  $serverIncrement ))

clientCombinations=$(( $maxClientsForEachServer - $minClientsForEachServer +1 ))
clientCombinations=$(( $clientCombinations /  $clientIncrement ))

if [ $varyClusterDeployment == "true" ]; then
	clusterCombination=${#clusters[@]}
else
	clusterCombination=1
fi

totalCombinations=$(( $consistencyCombinations * $threadCombinations * $clusterCombination * $serverCombinations * $clientCombinations ))

read -p "with this configuration there will be generated around $totalCombinations runs on the grid. Are you sure to continue? " -n 1 -r
if [[ $REPLY =~ ^[Nn]$ ]]
then
	echo ""
   exit 
fi
echo ""
echo "Ok. I will work, you can go for a beer"


#iterate over consistencies
for cons in "${consistency[@]}"
do
	consistencyVector="cons=(\""$cons"\")"
	sed -i "s/cons=.*/${consistencyVector}/g" configuration.sh

	threads=`seq ${minClientsThread} ${clientsThreadIncrement} ${maxClientsThread}`
	#iterate over threads
	for t in ${threads}; 
	do
		sed -i "s/client_thread_increment=.*/client_thread_increment=\"1\"/g" configuration.sh
		sed -i "s/client_thread_glb=.*/client_thread_glb=\"${t}\"/g" configuration.sh
		sed -i "s/client_thread_lub=.*/client_thread_lub=\"${t}\"/g" configuration.sh

		echo ""
		echo "******************************** Grid Experience: run " $iterations " of " $totalCombinations " ********************************"
		echo "    consistency: "$cons", threads:"$t
		#sleep 30
		deployCluster
	done
done

