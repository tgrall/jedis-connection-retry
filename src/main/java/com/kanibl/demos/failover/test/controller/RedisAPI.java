package com.kanibl.demos.failover.test.controller;

import com.kanibl.demos.failover.test.config.JedisAccess;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/redis")
public class RedisAPI {

    @Autowired
    private Environment env;

    JedisAccess jedisAccess = new JedisAccess();


    @GetMapping("/{nbCall}")
    public Map call(@PathVariable("nbCall") Integer nbCall) {
        Jedis jedis = jedisAccess.getJedisConnection();

        if (nbCall == null || nbCall < 1) {
            nbCall = 1;
        }

        long start  = System.currentTimeMillis();

        Map result = new HashMap();
        result.put("foo.value.begin",  jedis.get("foo") );


        for (Integer i = 0 ; i < nbCall ; i++) {

            long startCommand = System.currentTimeMillis();

            // retry the operation 3 times before failing
            int currentRetry = 0;


            while(currentRetry < 3) {
                currentRetry++;
                try {
                    jedis.incr("foo");
                    currentRetry = 3; // stop
                } catch(JedisConnectionException jde) {
                    // reconnect
                    result.put("foo.incr.failed-retry",  currentRetry  );
                    result.put("foo.incr.reconnect",  "Reconnecting"  );
                    jedis = jedisAccess.getJedisConnection();
                    System.out.println("RECONNECT .....");

                } catch (Exception e) {
                }
            }


            long elapsedTimeCommand = System.currentTimeMillis() - startCommand;


            if ( result.get("maxtime_command_ms") == null || (Long)result.get("maxtime_command_ms") <= elapsedTimeCommand ) {
                result.put("maxtime_command_ms", elapsedTimeCommand);
            }

        }
        result.put("foo.value.end",  jedis.get("foo") );

        result.put("total_time_ms", System.currentTimeMillis() - start);

        result.put("conn_messages", jedisAccess.getMessages());

        jedis.close();
        return result;
    }


    @GetMapping("/reset")
    public Map resetJedisPool() {

        jedisAccess.resetJedisPool();
        Map result = new HashMap();
        result.put("reset-pool", "ok");
        return result;
    }


    @GetMapping("/config")
    public Map getJedisAccessConfig() {
        return  jedisAccess.getConfig();
    }


    @GetMapping("/set_type/{type}")
    public Map call(@PathVariable("type") String type) {
        jedisAccess.setConnType(type);
        return  jedisAccess.getConfig();

    }

}
