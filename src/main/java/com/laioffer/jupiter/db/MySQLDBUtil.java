package com.laioffer.jupiter.db;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MySQLDBUtil {

    private static final String INSTANCE = "laiproject-instance.cw3n4aj7pupb.us-east-2.rds.amazonaws.com";
    private static final String PORT_NUM = "3306";
    private static final String DB_NAME = "jupiter"; // 在不同 DB_NAME 下的N坨数据之间不影响
    public static String getMySQLAddress() throws IOException {
        Properties prop = new Properties();
        String propFileName = "config.properties";


        InputStream inputStream = MySQLDBUtil.class.getClassLoader().getResourceAsStream(propFileName); // load file with relative path
        prop.load(inputStream); // read content


        String username = prop.getProperty("user"); // read the user you wrote in config.properties
        String password = prop.getProperty("password"); // read the password you wrote in config.properties
        return String.format("jdbc:mysql://%s:%s/%s?user=%s&password=%s&autoReconnect=true&serverTimezone=UTC&createDatabaseIfNotExist=true",
                INSTANCE, PORT_NUM, DB_NAME, username, password); // 5 个 %s 会被 variable 代替
    }

}
