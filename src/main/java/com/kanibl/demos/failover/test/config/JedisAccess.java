package com.kanibl.demos.failover.test.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisSentinelPool;
import redis.clients.jedis.exceptions.JedisConnectionException;

import javax.annotation.PostConstruct;
import java.util.*;

@Configuration
public class JedisAccess {

    @Autowired
    private Environment env;

    int retryCounter = 0;
    int maxRetries = 5;
    int waitTime  = 1000;


    private static String connType = "FQDN";
    private static String dbHost = null;
    private static String dbPort = null;
    private static String dbName = null;
    private static String dbPassword = null;
    private static Set<String> sentinels = new HashSet<String>();



    private static JedisPool jedisPool = null;
    private static JedisSentinelPool jedisSentinelPool = null;

    private static Map connectionStatusMessage = new HashMap<>();


    public JedisAccess() {
    }

    @PostConstruct
    private void initConfig() {

        System.out.println(env);

        String hostsList = env.getProperty("HOST_LIST");
        if (hostsList != null) {
            sentinels = new HashSet<String>(Arrays.asList(hostsList.split(",")));
        }
        dbName = env.getProperty("DB_NAME");
        dbHost = env.getProperty("DB_HOST");
        dbPort = env.getProperty("DB_PORT");
        dbPassword = env.getProperty("DB_PASSWORD");
        if (dbPassword.trim().equals("")) {
            dbPassword = null;
        }
        connType = env.getProperty("CONN_TYPE");

        System.out.println("Configuration : ");
        System.out.println("\t HOST_LIST : "+ hostsList);
        System.out.println("\t DB_HOST : "+ dbHost);
        System.out.println("\t DB_PORT : "+ dbPort);
        System.out.println("\t DB_PASSWORD : "+ dbPassword);
        System.out.println("\t CONN_TYPE : "+ connType);

    }


    private JedisSentinelPool getJedisSentinelPool() {
        if (jedisSentinelPool == null) {
            if (dbPassword != null) {
                jedisSentinelPool = new JedisSentinelPool(dbName, sentinels, dbPassword);
            } else {
                jedisSentinelPool = new JedisSentinelPool(dbName, sentinels);
            }
        }
        return jedisSentinelPool;

    }


    private JedisPool getJedisPool() {
        if (jedisPool == null) {
            JedisPoolConfig jedisPoolConfig = new JedisPoolConfig();
            jedisPoolConfig.setMaxTotal(3);
            jedisPoolConfig.setMaxWaitMillis(5000);
            if (dbPassword != null) {
                jedisPool = new JedisPool( jedisPoolConfig, dbHost, Integer.parseInt(dbPort), 5000, dbPassword );
            } else {
                jedisPool = new JedisPool( jedisPoolConfig, dbHost, Integer.parseInt(dbPort), 5000 );
            }

        }
        return jedisPool;
    }

    private Jedis getJedisFromSentinelPool() {
        connectionStatusMessage = new HashMap(); // reset messages
        long start = System.currentTimeMillis();
        Jedis jedis = null ;
        retryCounter = 0;
        while(retryCounter < maxRetries) {
            retryCounter++;
            try {
                jedis = this.getJedisSentinelPool().getResource();
                connectionStatusMessage.put("sentinel-retry-"+ retryCounter, "success");
                retryCounter = 99;
            } catch (Exception jce) {
                connectionStatusMessage.put("sentinel-retry-"+ retryCounter, "fail");
                if (retryCounter == maxRetries-1) {
                    throw jce;
                }
                try { Thread.sleep(1000 * retryCounter); } catch (Exception e) {}
            }
        }
        connectionStatusMessage.put("conn_total_elapsed_time_ms", System.currentTimeMillis() - start);
        return jedis;

    }

    private Jedis getJedisFromPool() {
        connectionStatusMessage = new HashMap(); // reset messages
        long start = System.currentTimeMillis();
        Jedis jedis = null ;
        retryCounter = 0;
        while(retryCounter < maxRetries) {
            retryCounter++;
            System.out.println(retryCounter);
            try {
                jedis = this.getJedisPool().getResource();
                connectionStatusMessage.put("fqdn-retry-"+ retryCounter, "success");
                retryCounter = 99;
            } catch (Exception jce) {
                connectionStatusMessage.put("fqdn-retry-"+ retryCounter, "fail");
                if (retryCounter == maxRetries-1) {
                    throw jce;
                }
                try { Thread.sleep(1000 * retryCounter); } catch (Exception e) {}
            }
        }
        connectionStatusMessage.put("conn_total_elapsed_time_ms", System.currentTimeMillis() - start);
        return jedis;

    }


    public Jedis getJedisConnection() {
        Jedis jedis = null;
        if ( connType.equalsIgnoreCase("FQDN")) {
            jedis = this.getJedisFromPool();
        } else {
            jedis = this.getJedisFromSentinelPool();
        }
        return jedis;
    }

    /**
     * Remove pool
     */
    public void resetJedisPool() {
        if (jedisPool != null) {
            jedisPool.close();
            jedisPool.destroy();
            jedisPool = null;
        }

        if (jedisSentinelPool != null) {
            jedisSentinelPool.close();
            jedisSentinelPool.destroy();
            jedisSentinelPool = null;
        }
    }

    public Map getConfig() {
        Map config = new HashMap();

        config.put("dbname", dbName);
        config.put("sentinels", sentinels);
        config.put("dbPort", dbPort );
        config.put("dbHost", dbHost );
        config.put("connType", connType );

        return config;
    }


    public String getConnType(){
        return connType;
    }

    public void setConnType(String type) {
        connType = type;
        this.resetJedisPool();
    }

    public Map getMessages() {
        return connectionStatusMessage;
    }
}
