package com.proyecto.netsit_demo;

import org.junit.jupiter.api.Test;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.asteriskjava.manager.ManagerConnection;

@SpringBootTest
@ActiveProfiles("test")
class NetsitDemoApplicationTests {

    @MockitoBean
    ManagerConnection managerConnection;

    @Test
    void contextLoads() {
    }
}