package com.vladmihalcea.flexypool.adaptor;

import com.vladmihalcea.flexypool.model.Book;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * FlexyPoolConnectionProviderTest - FlexyPoolConnectionProvider Test
 *
 * @author Vlad Mihalcea
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:spring/applicationContext-tx.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class FlexyPoolConnectionProviderTest {

    @PersistenceContext(unitName = "persistenceUnit")
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private MockMetricsFactory metricsFactory;

    @Test
    @Transactional
    public void test() {
        final Book book = transactionTemplate.execute(new TransactionCallback<Book>() {
            @Override
            public Book doInTransaction(TransactionStatus status) {
                Book book = new Book();
                book.setId(1L);
                book.setName("High-Performance Java Persistence");
                entityManager.persist(book);
                return book;
            }
        });

        transactionTemplate.execute(new TransactionCallback<Void>() {
            @Override
            public Void doInTransaction(TransactionStatus status) {
                assertEquals(book.getName(), entityManager.find(Book.class, book.getId()).getName());
                return null;
            }
        });
        verify(metricsFactory.getConcurrentConnectionRequestCountHistogram(), times(2)).update(1);
        verify(metricsFactory.getConcurrentConnectionRequestCountHistogram(), times(2)).update(0);
    }
}