package com.kanibl.demos.failover.test;


import java.util.Arrays;

import com.kanibl.demos.failover.test.config.JedisAccess;
import org.springframework.boot.Banner;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

@SpringBootApplication
public class Application implements CommandLineRunner {

    public static void main(String[]args) throws Exception {

        SpringApplication app = new SpringApplication(Application.class);
        app.setBannerMode(Banner.Mode.OFF);
        app.run(args);
    }

    //access command line arguments
    @Override
    public void run(String... args) throws Exception {
        System.out.println("Run");

        if (args != null && args.length != 0 && args[0].equalsIgnoreCase("cli")) {
            System.out.println("Call Redis");
            this.callRedis();
        }

    }


    private void callRedis() {
        JedisAccess jedisAccess = new JedisAccess();
        Jedis jedis = jedisAccess.getJedisConnection();
        jedis.del("cli.infinite.loop");

        long printDataEeverySeconds = 5;
        long lastPrintTime = System.currentTimeMillis();
        long nbOdOps = 0;


        boolean infinite = true;
        while (infinite) {

            long start  = System.currentTimeMillis();

            // retry the operation 3 times before failing
            int currentRetry = 0;

            while(currentRetry < 3) {
                currentRetry++;
                try {
                    jedis.incr("cli.infinite.loop");
                    currentRetry = 3; // stop
                } catch(JedisConnectionException jde) {
                    // reconnect
                    jedis = jedisAccess.getJedisConnection();
                    System.out.println("RECONNECT .....");

                } catch (Exception e) {
                }
            }


            nbOdOps++;
            long end  = System.currentTimeMillis();

            if ((end - printDataEeverySeconds*1000 ) >  lastPrintTime ) {
                lastPrintTime = end;
                System.out.println( "Ops per seconds : "+ (nbOdOps / printDataEeverySeconds));
                nbOdOps = 0;
            }

            try {
                Thread.sleep(20);
            } catch (InterruptedException ie){}

        }
    }
}
