# Makefile for Spark WordCount project.

# Customize these paths for your environment.
# -----------------------------------------------------------
spark.root=/opt/spark
hadoop.root=/usr/local/hadoop
hbase.root=/usr/local/HBase

# Graph Structure
gs_app.name=Graph Structure
gs_jar.name=graph-structure-1.0.jar
gs_jar.path=graph-structure/target/${gs_jar.name}
gs_job.name=gs.GraphStructure

# Hbase Insert
hbase_insert_app.name=HBase Insert
hbase_insert_jar.name=hbase-insert-1.0.jar
hbase_insert_jar.path=hbase-insert/target/${hbase_insert_jar.name}
hbase_insert_job.name=hbi.HbaseInsertTable

# Hbase Read
hbase_read_app.name=HBase Read
hbase_read_jar.name=hbase-read-1.0.jar
hbase_read_jar.path=hbase-read/target/${hbase_read_jar.name}
hbase_read_job.name=hbr.HbaseReadJoin
hbase_read_output=hbr_output

local.master=local[*]
local.input=input
local.gs_output=gs_output

# Pseudo-Cluster Execution
hdfs.user.name=sidharth
hdfs.input=input
hdfs.fc_output=fc_output

# AWS EMR Execution
aws.emr.release=emr-5.26.0
aws.bucket.name=sidharth-cs6240
aws.log.dir=logs
aws.input=input
aws.fc_output=fc_output
aws.num.nodes=5
aws.instance.type=m4.large
# -----------------------------------------------------------

# Compiles code and builds jar (with dependencies).
jar:
	mvn clean package

# Removes local output directory.
clean-local-output:
	rm -rf ${local.fc_output}*
	rm -rf ${aws.log.dir}*

clean-local-output-gs:
	rm -rf ${local.gs_output}*

start-hbase:
	${hbase.root}/bin/start-hbase.sh

stop-hbase:
	${hbase.root}/bin/stop-hbase.sh

# Runs standalone
local-gs: jar clean-local-output-gs
	spark-submit --class ${gs_job.name} --master ${local.master} --name "${gs_app.name}" ${gs_jar.path} ${local.input} ${local.gs_output}

local-hbase-insert: jar
	spark-submit --class ${hbase_insert_job.name} --master ${local.master} --name "${hbase_insert_app.name}" ${hbase_insert_jar.path} ${local.input}

local-hbase-read: jar
	spark-submit --class ${hbase_read_job.name} --master ${local.master} --name "${hbase_read_app.name}" ${hbase_read_jar.path} ${local.input} ${hbase_read_output}

local-fc: jar clean-local-output-pr
	spark-submit --class ${pr_job.name} --driver-memory 5g  --executor-memory 6g --master ${local.master} --name "${pr_app.name}" ${pr_jar.path} ${local.input} -1 ${local.iterations} ${local.pr_output}



#local: jar clean-local-output
#	spark-submit --class ${job.name} --master ${local.master} --name "${app.name}" ${jar.name} ${local.input} ${local.input_nodes} ${local.sorted_output} ${local.no_followers_output}

# Start HDFS
start-hdfs:
	${hadoop.root}/sbin/start-dfs.sh

# Stop HDFS
stop-hdfs: 
	${hadoop.root}/sbin/stop-dfs.sh
	
# Start YARN
start-yarn: stop-yarn
	${hadoop.root}/sbin/start-yarn.sh

# Stop YARN
stop-yarn:
	${hadoop.root}/sbin/stop-yarn.sh

# Reformats & initializes HDFS.
format-hdfs: stop-hdfs
	rm -rf /tmp/hadoop*
	${hadoop.root}/bin/hdfs namenode -format

# Initializes user & input directories of HDFS.	
init-hdfs: start-hdfs
	${hadoop.root}/bin/hdfs dfs -rm -r -f /user
	${hadoop.root}/bin/hdfs dfs -mkdir /user
	${hadoop.root}/bin/hdfs dfs -mkdir /user/${hdfs.user.name}

# Load data to HDFS
upload-input-hdfs: start-hdfs
	${hadoop.root}/bin/hdfs dfs -put ${local.input}/* /user/${hdfs.user.name}/${hdfs.input}

# Removes hdfs output directory.
clean-hdfs-output:
	${hadoop.root}/bin/hdfs dfs -rm -r -f ${hdfs.fc_output}*

# Download output from HDFS to local.
download-output-hdfs:
	mkdir ${local.output}
	${hadoop.root}/bin/hdfs dfs -get ${hdfs.output}/* ${local.output}

# Runs pseudo-clustered (ALL). ONLY RUN THIS ONCE, THEN USE: make pseudoq
# Make sure Hadoop  is set up (in /etc/hadoop files) for pseudo-clustered operation (not standalone).
# https://hadoop.apache.org/docs/current/hadoop-project-dist/hadoop-common/SingleCluster.html#Pseudo-Distributed_Operation
pseudo: jar stop-yarn format-hdfs init-hdfs upload-input-hdfs start-yarn clean-local-output 
	spark-submit --class ${job.name} --master yarn --deploy-mode cluster ${jar.name} ${local.input} ${local.input_nodes} ${local.sorted_output} ${local.no_followers_output}

pseudo-hbase-insert: jar stop-yarn format-hdfs init-hdfs upload-input-hdfs start-yarn clean-local-output start-hbase
	spark-submit --class ${hbase_insert_job.name} --master yarn --deploy-mode cluster "${hbase_insert_app.name}" ${hbase_insert_jar.path} ${local.input}

# Runs pseudo-clustered (quickie).
pseudoq: jar clean-local-output clean-hdfs-output 
	spark-submit --class ${job.name} --master yarn --deploy-mode cluster ${jar.name} ${local.input} ${local.input_nodes} ${local.sorted_output} ${local.no_followers_output}
	make download-output-hdfs

# Create S3 bucket.
make-bucket:
	aws s3 mb s3://${aws.bucket.name}

# Upload data to S3 input dir.
upload-input-aws: make-bucket
	aws s3 sync ${local.input} s3://${aws.bucket.name}/${aws.input}
	aws s3 sync ${local.input_nodes} s3://${aws.bucket.name}/${aws.input_nodes}

# Delete S3 output dir.
delete-output-aws:

# Upload application to S3 bucket.
upload-app-pr:
	aws s3 cp ${pr_jar.path} s3://${aws.bucket.name}

upload-app-aws-map-join:
	aws s3 cp ${mj_jar.path} s3://${aws.bucket.name}

upload-app-aws:
	aws s3 cp ${jar.name} s3://${aws.bucket.name}

# Main EMR launch.
aws-pr: jar upload-app-aws-pr
	aws emr create-cluster \
		--name "Page Rank Spark Small" \
		--release-label ${aws.emr.release} \
		--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
	    --applications Name=Hadoop Name=Spark \
		--steps Type=CUSTOM_JAR,Name="${pr_app.name}",Jar="command-runner.jar",ActionOnFailure=TERMINATE_CLUSTER,Args=["spark-submit","--deploy-mode","cluster","--class","${pr_job.name}","s3://${aws.bucket.name}/${pr_jar.name}","s3://${aws.bucket.name}/${aws.input}","${local.k}","${local.iterations}","${local.pr_output}"] \
		--log-uri s3://${aws.bucket.name}/${aws.log.dir} \
		--use-default-roles \
		--enable-debugging \
		--auto-terminate

aws: jar upload-app-aws delete-output-aws
	aws emr create-cluster \
		--name "Followers Count Spark Cluster" \
		--release-label ${aws.emr.release} \
		--instance-groups '[{"InstanceCount":${aws.num.nodes},"InstanceGroupType":"CORE","InstanceType":"${aws.instance.type}"},{"InstanceCount":1,"InstanceGroupType":"MASTER","InstanceType":"${aws.instance.type}"}]' \
	    --applications Name=Hadoop Name=Spark \
		--steps Type=CUSTOM_JAR,Name="${app.name}",Jar="command-runner.jar",ActionOnFailure=TERMINATE_CLUSTER,Args=["spark-submit","--deploy-mode","cluster","--class","${job.name}","s3://${aws.bucket.name}/${jar.name}","s3://${aws.bucket.name}/${aws.input}","s3://${aws.bucket.name}/${aws.input_nodes}","s3://${aws.bucket.name}/${aws.sorted_output}","s3://${aws.bucket.name}/${aws.no_followers_output}"] \
		--log-uri s3://${aws.bucket.name}/${aws.log.dir} \
		--use-default-roles \
		--enable-debugging \
		--auto-terminate
		
# Download output from S3.
download-output-aws:
	aws s3 sync s3://${aws.bucket.name}/${aws.log.dir} ${aws.log.dir}

# Change to standalone mode.
switch-standalone:
	cp config/standalone/*.xml ${hadoop.root}/etc/hadoop

# Change to pseudo-cluster mode.
switch-pseudo:
	cp config/pseudo/*.xml ${hadoop.root}/etc/hadoop

# Package for release.
distro:
	rm -f Spark-Demo.tar.gz
	rm -f Spark-Demo.zip
	rm -rf build
	mkdir -p build/deliv/Spark-Demo
	cp -r src build/deliv/Spark-Demo
	cp -r config build/deliv/Spark-Demo
	cp -r input build/deliv/Spark-Demo
	cp pom.xml build/deliv/Spark-Demo
	cp Makefile build/deliv/Spark-Demo
	cp README.txt build/deliv/Spark-Demo
	tar -czf Spark-Demo.tar.gz -C build/deliv Spark-Demo
	cd build/deliv && zip -rq ../../Spark-Demo.zip Spark-Demo

# https://computingforgeeks.com/how-to-install-apache-hadoop-hbase-on-ubuntu/
# https://www.programcreek.com/2018/07/install-hadoop-and-hbase-on-ubuntu/