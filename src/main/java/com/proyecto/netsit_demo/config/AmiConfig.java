package com.proyecto.netsit_demo.config;

import org.asteriskjava.manager.ManagerConnection;
import org.asteriskjava.manager.ManagerConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AmiConfig {

    @Bean(destroyMethod = "logoff")
    public ManagerConnection managerConnection(AsteriskAmiProperties props) throws Exception {
        ManagerConnectionFactory factory = new ManagerConnectionFactory(
            props.getHost(), props.getPort(), props.getUsername(), props.getPassword());
        ManagerConnection connection = factory.createManagerConnection();
        connection.login();
        return connection;
    }
}