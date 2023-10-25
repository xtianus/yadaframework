package net.yadaframework.tools;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Persistence;

/**
 * Generates a db schema using the JPA standard.
 * It depends on .../src/main/resources/META-INF/persistence.xml being properly configured.
 */
public class YadaSchemaGenerator {

	public static void main(String[] args) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("yadaPersistenceUnit");
        emf.createEntityManager().close();  // This line triggers schema generation to file
        emf.close();
    }

}
