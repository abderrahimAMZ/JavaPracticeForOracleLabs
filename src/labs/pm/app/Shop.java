/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package labs.pm.app;

import labs.pm.data.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * {@code Shop} class represents an application that manages Products
 * @version 4.0
 * @author amz
 */
public class Shop {
    public static void main(String[] args) {

        AtomicInteger clientCount = new AtomicInteger(0);

        ProductManager productManager = ProductManager.getInstance();

        Callable<String> client = () -> {
            String clientId = "Client " + clientCount.incrementAndGet();
            String threadName = Thread.currentThread().getName();
            int productId = ThreadLocalRandom.current().nextInt(63) + 101;
            String languageTag = ProductManager.getSupportedLocales()
                    .stream()
                    .skip(ThreadLocalRandom.current().nextInt(4))
                    .findFirst().get();

            StringBuilder log = new StringBuilder();

            log.append(clientId + " " + threadName + "\n-\t start of log\t-\n");

            // code will be placed here
            log.append(productManager.getDiscounts(languageTag)
                    .entrySet()
                    .stream()
                    .map(entry -> entry.getKey() + "\t" + entry.getValue())
                    .collect(Collectors.joining("\n"))
            );


            Product product = productManager.reviewProduct(productId, Rating.FOUR_STAR, "Yet another review");

            log.append((product != null)
                    ? "\nProduct " + productId + "reviewed\n"
                    : "\nProduct " + productId + "not reviewed\n"
            );

            productManager.printProductReport(productId, languageTag, clientId);

            log.append(clientId + "generated report for " + productId + " product");


            log.append("\n-\t tend of log\t-\n");

            return log.toString();
        };

        List<Callable<String>> clients = Stream.generate(() -> client)
                .limit(5)
                .collect(Collectors.toList());

        ExecutorService executorService = Executors.newFixedThreadPool(5);


        try {
            List<Future<String>> results = executorService.invokeAll(clients);

            executorService.shutdown();

            results.stream()
                    .forEach(result -> {
                        try {
                            System.out.println(result.get());
                        } catch (InterruptedException | ExecutionException e) {
                            Logger.getLogger(Shop.class.getName()).log(Level.SEVERE, "Error retrieving client log ", e);

                        }
                    });

        } catch (InterruptedException e) {
            Logger.getLogger(Shop.class.getName()).log(Level.SEVERE, "Error invoking client", e);
            throw new RuntimeException(e);
        }


        try {
            productManager.parseProduct("D, 101, Tea, 1.99, 3, ");
        } catch (ProductManagerException e) {
            Throwable cause = e.getCause();
        }

        productManager.printProductReport(101, "en-US","idk");

        productManager.parseReview("101, 4, nice hot cup of Tea");



        productManager.reviewProduct(101,Rating.NOT_RATED, "nice hot cup of Tea");
        productManager.reviewProduct(101,Rating.NOT_RATED, "lorem ipsom");
        productManager.reviewProduct(101,Rating.NOT_RATED, "nice hot cup of Tea");
        productManager.reviewProduct(101,Rating.NOT_RATED, "nice hot cup of Tea");
        productManager.reviewProduct(101,Rating.FOUR_STAR, "nice hot cup of Tea");
        productManager.printProductReport(101,"en-US","idk");

        productManager.dumpData();
        productManager.restoreData();

        Product p2 = productManager.createProduct(102,"Coffee",BigDecimal.valueOf(1.99), Rating.FOUR_STAR);

        p2 = productManager.reviewProduct(102,Rating.THREE_STAR, "Coffe was ok");
        p2 = productManager.reviewProduct(102,Rating.ONE_STAR, "Where is the milk?!");
        p2 = productManager.reviewProduct(102,Rating.FIVE_STAR, "It's eprfect with ten spoons of sugar!");

        productManager.printProductReport(102,"en-US","idk");


        Product p3 = productManager.createProduct(103,"Cake",BigDecimal.valueOf(3.99), Rating.FIVE_STAR, LocalDate.now().plusDays(2));

        p3 = productManager.reviewProduct(103,Rating.FIVE_STAR, "Very nice cake");
        p3 = productManager.reviewProduct(103,Rating.FOUR_STAR, "It good, but I've expected more chocolate");
        p3 = productManager.reviewProduct(103,Rating.FIVE_STAR, "This cake is perfect!");

        productManager.printProductReport(103,"en-US","idk");

        Product p4 = productManager.createProduct(104,"Cookie",BigDecimal.valueOf(3.99),Rating.TWO_STAR,LocalDate.now());

        p4 = productManager.reviewProduct(104,Rating.THREE_STAR, "Just another cookie");
        p4 = productManager.reviewProduct(104,Rating.THREE_STAR, "Ok");

        productManager.printProductReport(104,"en-US","idk");

        Product p5 = productManager.createProduct(105,"Hot Chocolate",BigDecimal.valueOf(2.50),Rating.NOT_RATED);


        p5 = productManager.reviewProduct(105,Rating.FOUR_STAR, "Tasty!");
        p5 = productManager.reviewProduct(105,Rating.FOUR_STAR, "No bad at all");

        productManager.printProductReport(105,"en-US", "idk");

        Product p6 = productManager.createProduct(106,"Chocolate",BigDecimal.valueOf(2.50),Rating.NOT_RATED, LocalDate.now().plusDays(3));


        p6 = productManager.reviewProduct(106,Rating.TWO_STAR, "Too sweet");
        p6 = productManager.reviewProduct(106,Rating.THREE_STAR, "Better then cookie");
        p6 = productManager.reviewProduct(106,Rating.TWO_STAR, "Too bitter");
        p6 = productManager.reviewProduct(106,Rating.ONE_STAR, "I don't get it!");

        productManager.printProductReport(106,"en-US","idk");



        System.out.println("comparing ratings");
        //productManager.printProducts((p,p20) -> p.getRating().ordinal() - p20.getRating().ordinal());
        System.out.println("comparing prices");
        //productManager.printProducts((p,p20) -> p.getPrice().compareTo(p20.getPrice()));


        Comparator<Product> ratingSorter = (pr1,pr2) -> pr2.getRating().ordinal() - pr1.getRating().ordinal();
        Comparator<Product> priceSorter = (pr1,pr2) -> pr1.getPrice().compareTo(pr2.getPrice());


        System.out.println("comparing by rating then price");
        productManager.printProducts(p-> p.getRating().ordinal() >2 ,ratingSorter.thenComparing(priceSorter),"en-US");



        productManager.getDiscounts("en-US")
                .forEach((rating, discount) -> System.out.println(rating + '\t' + discount));
    }

}



