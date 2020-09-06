import de.vogella.jpa.eclipselink.model.Family;
import de.vogella.jpa.eclipselink.model.Person;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.persistence.Query;

import static org.junit.Assert.*;

public class JpaTest {
    private static final String PERSISTENCE_UNIT_NAME = "people";
    private EntityManagerFactory factory;

    @BeforeEach
    public void setUp() throws Exception {
        factory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT_NAME);
        EntityManager em = factory.createEntityManager();

        // Begin a new local transaction so that we can persist a new entity
        em.getTransaction().begin();

        // read the existing entries
        Query q = em.createQuery("select m from Person m");
        // Persons should be empty

        // do we have entries?
        boolean createNewEntries = (q.getResultList().size() == 0);

        // No, so lets create new entries
        if (createNewEntries) {
            assertEquals(0, q.getResultList().size());
            Family family = new Family();
            family.setDescription("Family for the Knopfs");
            em.persist(family);
            for (int i = 0; i < 40; i++) {
                Person person = new Person();
                person.setFirstName("Jim_" + i);
                person.setLastName("Knopf_" + i);
                person.setFamily(family);
                em.persist(person);
                // now persists the family person relationship
                family.getMembers().add(person);
                em.persist(person);
                em.persist(family);
            }
        }

        // Commit the transaction, which will cause the entity to
        // be stored in the database
        em.getTransaction().commit();

        // It is always good practice to close the EntityManager so that
        // resources are conserved.
        em.close();

    }

    @Test
    public void checkAvailablePeople() {
        // now lets check the database and see if the created entries are there
        // create a fresh, new EntityManager
        EntityManager em = factory.createEntityManager();

        // Perform a simple query for all the Message entities
        Query q = em.createQuery("select m from Person m");

        // We should have 40 Persons in the database
        assertEquals(40, q.getResultList().size());

        em.close();
    }

    @Test
    public void checkFamily() {
        EntityManager em = factory.createEntityManager();
        // Go through each of the entities and print out each of their
        // messages, as well as the date on which it was created
        Query q = em.createQuery("select f from Family f");

        Family family = (Family) q.getSingleResult();
        // We should have one family with 40 persons
        assertEquals(1, q.getResultList().size());
        assertEquals(40, family.getMembers().size());
        em.close();
    }

    @Test //(expected = javax.persistence.NoResultException.class)
    public void deletePerson() {
        EntityManager em = factory.createEntityManager();
        // Begin a new local transaction so that we can persist a new entity
        em.getTransaction().begin();
        Query q = em
                .createQuery("SELECT p FROM Person p WHERE p.firstName = :firstName AND p.lastName = :lastName");
        q.setParameter("firstName", "Jim_1");
        q.setParameter("lastName", "Knopf_1");
        //q.setParameter("lastName", "Knopf_!"); This test was broken from the start...
        Person user = (Person) q.getSingleResult();
        // Get the family for removing the family connection.
        Family family = user.getFamily();
        family.getMembers().remove(user);
        em.remove(user);
        em.getTransaction().commit();
        try {
            // Confirm that deleted entity is no longer in database.
            // Will throw exception if person is gone from persistence context.
            Person person = (Person) q.getSingleResult();
            // Begin a new local transaction so that we can persist a new entity
        } catch (javax.persistence.NoResultException e) {
            // Recreate the deleted person for the other tests.
            em.getTransaction().begin();
            Person person = new Person();
            person.setFirstName("Jim_1");
            person.setLastName("Knopf_1");
            person.setFamily(family);
            em.persist(person);
            // Restore the family connection.
            family.getMembers().add(person);
            em.persist(person);
            em.persist(family);
            em.getTransaction().commit();
            return;
        } finally {
            em.close();
        }
        fail("Should have thrown a javax.persistence.NoResultException");
    }
}
