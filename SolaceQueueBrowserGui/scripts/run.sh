CFG=sample/sample-config.json
OUT=out/run.out
JAR=target/SolaceQueueBrowserGui-1.0.0-jar-with-dependencies.jar
# check if JAR and CFG exist
if [ ! -f $JAR ]; then
    echo "JAR file not found: $JAR"
    exit 1
fi
if [ ! -f $CFG ]; then
    echo "CFG file not found: $CFG "
    exit 1
fi
# create the output dir 
if [ ! -d $OUT ]; then
    mkdir -p $OUT
fi


echo "Running $JAR with $CFG"
java -jar $JAR -c $CFG > $OUT 2>&1  &
echo "Check $OUT"
