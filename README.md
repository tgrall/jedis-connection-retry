# Simple Jedis Retry Connection Example

This sample application allow user to connect to Redis/Redis Enterprise Cluster 
and see the impact of a fail over on the client connections/operations.

This application can be deployed on Pivotal Cloud Foundry and executed as a simple CLI.


## Build & Run

The application is using environment variables to pass the Redis connection information:

```
export CONN_TYPE=FQDN
export HOST_LIST=localhost:26379,localhost:26380,localhost:26381,localhost:26382
export DB_NAME=db-001
export DB_HOST=localhost
export DB_PORT=6379
export DB_PASSWORD=foobared
```



### Run as a Web application

Once the environment variables are set you can run the Web application using the following command:

```
> mvn clean package

> java -jar ./target/jedis-connection-failover-demo-cloud.jar 

```

Then you can access the REST API using the following URI:

* http://localhost:8080/api/redis/config  : will show you the current configuration
* http://localhost:8080/api/redis/set_type/FQDN  or http://localhost:8080/api/redis/set_type/sentinel

Then you can run the application and run a loop that set/get values:

* http://localhost:8080/api/redis/400 where 400 is the number of iteration

Once done you will have a result with some basic metrics for example: (99999 operations)

```json
{
  conn_messages: {
  fqdn-retry-4: "success",
  fqdn-retry-3: "fail",
  fqdn-retry-2: "fail",
  fqdn-retry-1: "fail",
  conn_total_elapsed_time_ms: 6018
  },
  maxtime_command_ms: 6024,
  foo.incr.failed-retry: 2,
  foo.value.end: "22844",
  foo.value.begin: "530000",
  total_time_ms: 9924,
  foo.incr.reconnect: "Reconnecting"
}
```

Indicates that the Redis cluster/instances has been stopped during the ranL

* fqdn-retry-1 to 4 : the application retry to connect 4 times (during the shutdown of Redis)
* conn_total_elapsed_time_ms : the application tool 6 seconds to  reconnect (it was the time I manuall stop and restart Redis)
* then the metrics about the operation including the longest command

You can run the same command during rolling upgrades, failover, etc etc. You can see the impact on the application

### Run as a Command Line

For the command line it is the same application but just add `cli` to the command line as follow:

```
> java -jar ./target/jedis-connection-failover-demo-cloud.jar cli

```

You will see the number of operation per second (it is not a benchmark, the application stop for 20ms between each call),
but you will see when the application has to reconnect.

