#!/bin/bash


#RES_ID=$(grep "Grid reservation id" tmpOar | cut -f2 -d=)
	
export clustersNumber=$(($# / 3))

#touch /root/.ssh/config
#rm /root/.ssh/known_hosts
#echo "Host *" >> /root/.ssh/config
#echo "    StrictHostKeyChecking no" >> /root/.ssh/config
#service ssh restart
rm -f myfractal.xml

echo '<?xml version="1.0" encoding="ISO-8859-1" ?>' >> myfractal.xml
echo '<FRACTAL>'  >> myfractal.xml
echo '<BootstrapIdentity>' >> myfractal.xml
echo '<nodelist>' >> myfractal.xml

nodeStr='' #'nodes=('
servers='' #'servers=('
clients='' #'clients=('
nodes=''

distem --copy-from vnode=r_uw,src=/etc/hosts,dest=/root/distemjessy/scripts   #One of the nodes has to be UW. myfractal.xml generated using the /etc/hosts file in some virtual node

FILE='/root/distemjessy/scripts/hosts'
j=0
while read line; do
        a=($line)
        if  [[ ${a[1]} == *_S* ]]
        then
                echo '<node id="'$j'" ip="'${a[0]}'"/>' >> myfractal.xml
                j=$((j+1))
                nodes="$nodes \"${a[1]}\""
                servers="$servers \"${a[1]}\""
        fi
        if  [[ ${a[1]} == *_C* ]]
        then
                nodes="$nodes \"${a[1]}\""
                clients="$clients \"${a[1]}\""
        fi
done < $FILE
export nodes
nodeStr="nodes=("$nodes")"
servers="servers=("$servers")"
clients="clients=("$clients")"

echo '</nodelist>' >> myfractal.xml
echo '</BootstrapIdentity>' >> myfractal.xml
echo '</FRACTAL>' >> myfractal.xml

./distemshufflemyfractal.sh

echo "fractal configuration file is done"

sed -i "s/nodes=.*/${nodeStr}/g" configuration.sh
sed -i "s/servers=.*/${servers}/g" configuration.sh
sed -i "s/clients=.*/${clients}/g" configuration.sh
echo "configuration.sh file is done"

while read line; do
	echo "synchronising all sites"
        a=($line)
        if  [[ ${a[1]} == *_C* ]] || [[ ${a[1]} == *_S* ]]
        then
                #echo "synchronising  "${a[1]}  "...."
                distem --copy-to vnode=${a[1]},src=/root/distemjessy,dest=/root &
		pids="$pids $!"

                #echo "*******done*******"
                echo ""
       fi
done < $FILE

for p in "${pids[@]}"
        do
                wait ${p};
        done
echo ""
echo "**************************************************************************************"
echo "*** grid5kLaucher: myfractal and configuration.sh are done, launching experience... ***"
echo "**************************************************************************************"

./distemexperience.sh ${param[*]}

echo "******************************************************************************"
echo "grid5kLaucher: done, deleting jobs"
echo "******************************************************************************"
echo "******************************************************************************"
echo "grid5kLaucher: done, deleting jobs"
echo "******************************************************************************"
