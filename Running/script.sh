if [[ $# -ne 4 ]]
then
	echo "Error: Invalid number of arguments"
	exit -1
fi
function rmi(){
	ps cax |grep rmiregistry
	if [[ $? -eq 0 ]]
	then
		killall rmiregistry
		sleep 1
	fi
	cd
	rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false &
	cd - 1>/dev/null
	sleep 1
}
rmi
nproc=$2
echo Starting $nproc processes.
echo "0" > $4 
for (( i=1;i<=$nproc;i++ ))
do
	java -cp protobuf-java-3.0.2.jar: -Djava.security.policy=$PWD/grant.policy -Djava.rmi.server.codebase=file:$PWD/ DistributedLocking.LockProcess -i $i $1 $2 $3 $4 > $i.out &
	#java -cp protobuf-java-3.0.2.jar: -Djava.security.policy= -Djava.rmi.server.useCodebaseOnly=false  DistributedLocking.LockProcess -i $i $1 $2 $3 $4 > $i.out &
done

function ctrl_c() {
	pids=`ps aux|grep 'java -cp'|head -n -1|awk '{printf("%s\n",$2);}'`
	for pid in $pids
	do
		echo Killing -9 $pid
		kill -9 $pid
	done
	exit 0
}

trap ctrl_c SIGINT

while true
do
	sleep 5
done