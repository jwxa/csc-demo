ANNOUNCE_PORT=$(expr $1)

CONF_FILE="/tmp/redis.conf"

# generate redis.conf file
echo "port $ANNOUNCE_PORT
bind 0.0.0.0
cluster-enabled yes
cluster-config-file nodes.conf
cluster-node-timeout 5000
appendonly yes
loglevel debug
requirepass pass@123
masterauth  pass@123
protected-mode no
notify-keyspace-events Ex
" >> $CONF_FILE

# start server
redis-server $CONF_FILE