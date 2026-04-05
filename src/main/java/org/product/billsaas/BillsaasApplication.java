package org.product.billsaas;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.sql.DataSource;

@SpringBootApplication
public class BillsaasApplication {

	public static void main(String[] args) {
		SpringApplication.run(BillsaasApplication.class, args);
	}
	@org.springframework.context.annotation.Bean
	CommandLineRunner test(DataSource dataSource) {
		return args -> {
			try {
				System.out.println("=== DB CONNECTION TEST ===");
				System.out.println(dataSource.getConnection().getMetaData().getURL());
				System.out.println("✅ DB CONNECTED SUCCESSFULLY");
			} catch (Exception e) {
				System.out.println("❌ DB CONNECTION FAILED");
				e.printStackTrace();
			}
		};
	}
}
