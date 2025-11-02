ANNOUNCE_PORT=$(expr $1)
BUS_PORT=$(expr $1 + 1000)

CONF_FILE="/tmp/redis-test.conf"

cat <<EOF > $CONF_FILE
port $ANNOUNCE_PORT
bind 0.0.0.0
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
loglevel debug
requirepass pass@123
masterauth pass@123
protected-mode no
notify-keyspace-events Ex
cluster-announce-ip host.docker.internal
cluster-announce-port $ANNOUNCE_PORT
cluster-announce-bus-port $BUS_PORT
EOF

redis-server $CONF_FILE
