package kr.hhplus.be.server.testcontainers;

import java.sql.Connection;
import java.sql.DriverManager;

import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MySQLContainer;

// @Testcontainers
public class TestContainersDebugTest {

    // @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @Test
    void testContainerStartup() throws Exception {
        System.out.println("=== TestContainers Debug ===");
        System.out.println("Container started: " + mysql.isRunning());
        System.out.println("Container ID: " + mysql.getContainerId());
        System.out.println("JDBC URL: " + mysql.getJdbcUrl());
        System.out.println("Container logs:");
        System.out.println(mysql.getLogs());

        // 실제 연결 테스트
        try (Connection conn = DriverManager.getConnection(
                mysql.getJdbcUrl(), mysql.getUsername(), mysql.getPassword())) {
            System.out.println("✅ Connection successful!");
        } catch (Exception e) {
            System.err.println("❌ Connection failed: " + e.getMessage());
            throw e;
        }
    }
}