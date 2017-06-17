package com.lisuiheng;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@EnableAutoConfiguration
public class Application {
    private final Logger log = LoggerFactory.getLogger(Application.class);

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Component
    public class DBTransport implements CommandLineRunner {
        private final Config config;

        public DBTransport(Config config) {
            this.config = config;
        }

        @Override
        public void run(String... strings) throws Exception {
            String classPath = System.getProperty("java.class.path") ;
            int lastIndex = classPath.lastIndexOf(File.separator);
            String backupDir = classPath.substring(0, lastIndex);
                String backupPath = Paths.get(backupDir).resolve("alldb.sql").toString();
            List<BackUp> list = config.getList();
            for (BackUp backUp : list) {
                log.debug("alldb path is {}", backupPath);
                StringBuilder database = new StringBuilder();
                for (String db : backUp.getDbs()) {
                    database.append(db).append(" ");
                }


                String dropDatabase = "";
                if(backUp.dropDatabase) {
                    dropDatabase = "--add-drop-database";
                }
                String fetchDataCommand = String
                    .format("mysqldump --hex-blob -h%s -u%s -p%s --opt --databases %s %s --skip-lock-tables --result-file=%s",
                        backUp.getSourceHost(),
                        backUp.getSourceUsername(),
                        backUp.getSourcePassword(),
                        database.toString(),
                        dropDatabase,
                        backupPath
                );
                runCommand(fetchDataCommand);

                String mysqlCommand = String.format("mysql -h%s -u%s -p%s  < %s",
                        backUp.getTargetHost(),
                        backUp.getTargetUsername(),
                        backUp.getTargetPassword(),
                        backupPath
                );
                runCommand(mysqlCommand);
            }
        }

        private void runCommand(String command) throws IOException, InterruptedException {
            log.info("run  command {}",command);
            Process p = Runtime.getRuntime().exec(new String[] {"/bin/sh", "-backUp",command});



            BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String str = null;
            while ((str = in.readLine()) != null) {
                System.out.println(str);
            }
            p.waitFor();
        }
    }



    @Data
    @Component
    @ConfigurationProperties(prefix = "transport.config")
    private class Config {
        private final List<BackUp> list = new ArrayList<>();

    }


    @Data
    public static class BackUp {
        private boolean dropDatabase;
        private String sourceHost;
        private String sourceUsername;
        private String sourcePassword;
        private String targetHost;
        private String targetUsername;
        private String targetPassword;
        private String []dbs;
    }

}
