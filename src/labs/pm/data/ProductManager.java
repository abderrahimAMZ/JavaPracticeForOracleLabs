/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package labs.pm.data;

import java.math.BigDecimal;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.FormatStyle;
import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static java.util.Collections.sort;

/**
 * @author amz
 **/
public class ProductManager {


    private static final Logger logger = Logger.getLogger(ProductManager.class.getName());

    private ResourceBundle config = ResourceBundle.getBundle("config");

    private MessageFormat reviewFormat = new MessageFormat(config.getString("review.data.format"));
    private MessageFormat productFormat = new MessageFormat(config.getString("product.data.format"));

    public void parseReview(String text) throws ProductManagerException {
        try {
            Object[] values = reviewFormat.parse(text);

            reviewProduct(Integer.parseInt((String)values[0]), Reteable.convert(Integer.parseInt((String) values[1])),(String)values[2]);

        }
        catch (ParseException| NumberFormatException e) {
            logger.log(Level.WARNING, "Error parsing review : "+ text + " " + e.getMessage());

            throw new ProductManagerException("Unable to parse review",e);
        }
    }

    public void parseProduct(String text) throws ProductManagerException {
        try {
            Object[] values = productFormat.parse(text);

            int id = Integer.parseInt((String) values[1]);
            String name = (String) values[2];
            BigDecimal price = BigDecimal.valueOf(Double.parseDouble((String) values[3]));
            Rating rating = Reteable.convert(Integer.parseInt((String) values[4]));

            switch ((String) values[0]) {
                case "F":
                    LocalDate bestBefore = LocalDate.parse((String) values[5]);
                    createProduct(id,name,price,rating,bestBefore);
                    break;
                case "D":
                    createProduct(id,name,price,rating);
                    break;

            }

        }
        catch (ParseException | NumberFormatException | DateTimeParseException e) {
            logger.log(Level.WARNING, "Error parsing review : "+ text + " " + e.getMessage());

            throw new ProductManagerException("Unable to parse review",e);
        }
    }
    private Map<Product, List<Review>> products = new HashMap<>();

    private ResourceFormatter formatter;

    private static Map<String, ResourceFormatter> formatters =
            Map.of("en-GB", new ResourceFormatter(Locale.UK),
                    "en-US", new ResourceFormatter(Locale.US),
                    "es-US", new ResourceFormatter(new Locale("es","US")),
                    "fr-FR", new ResourceFormatter(Locale.FRANCE),
                    "zh-CN", new ResourceFormatter(Locale.CHINA)
                    );

    public ProductManager(Locale locale) {
        this(locale.toLanguageTag());

    }

    public ProductManager(String languageTag) {
        changeLocale(languageTag);
    }

    private static class ResourceFormatter {
        private Locale locale;
        private ResourceBundle resources;
        private DateTimeFormatter dateformat;
        private NumberFormat moneyformat;


        public ResourceFormatter(Locale locale) {
            this.locale = locale;
            resources = ResourceBundle.getBundle("labs.pm.data.resources",locale);
            dateformat = DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT);
            moneyformat = NumberFormat.getCurrencyInstance(locale);
    }


    public String formatProduct(Product product) {
        return MessageFormat.format(resources.getString("product"),
                product.getName(),
                moneyformat.format(product.getPrice()),
                product.getRating().getStars(),
                dateformat.format(product.getBestBefore())
        );
    }

    public String formatReview(Review review) {
        return MessageFormat.format(resources.getString("review"),
                review.getRating().getStars(),
                review.getComments()
        );
    }

    public String getText(String key) {
            return resources.getString(key);

    }



    }
    public void changeLocale(String languageTag) {
        formatter = formatters.getOrDefault(languageTag,formatters.get("en-GB"));
    }
    public static Set<String> getSupportedLocales() {
        return formatters.keySet();
    }
    public void printProductReport(Product product) {
        StringBuilder txt = new StringBuilder();

        txt.append(formatter.formatProduct(product));

        txt.append('\n');

        List<Review> reviews = products.get(product);

        sort(reviews);


        if (reviews.isEmpty()) {
            txt.append(formatter.getText("no.reviews"));
            txt.append("\n");
        }
        else {
            txt.append(
                    reviews.stream()
                            .map(r->formatter.formatReview(r) + '\n')
                            .collect(Collectors.joining())
                    );
        }
        System.out.println(txt);

    }


    public Product  createProduct(int id, String name, BigDecimal price, Rating rating, LocalDate bestBefore) {

        Product product =  new Food(id,name,price,rating,bestBefore);
        products.putIfAbsent(product,new ArrayList<>());
        return product;

    }
    public Product  createProduct(int id, String name, BigDecimal price, Rating rating) {

        Product product = new Drink(id,name,price,rating);
        products.putIfAbsent(product,new ArrayList<>());
        return product;

    }

    public Product reviewProduct(Product p1, Rating rating, String comments) {


        int sum = 0, i = 0;
        boolean reviewed = false;


        List<Review> reviews = products.get(p1);

        products.remove(p1,reviews);

        reviews.add(new Review(rating,comments));

        p1 = p1.applyRating(Reteable.convert(
                (int)Math.round(
                 reviews.stream()
                .mapToInt((review -> review.getRating().ordinal()))
                .average()
                .orElse(0)
                        )));

        products.put(p1,reviews);

        return p1;
    }

    public Product findProduct(int id) throws ProductManagerException {

        return products.keySet()
                .stream()
                .filter((product) -> product.getId() == id)
                .findFirst()
                .orElseThrow(() -> new ProductManagerException("no product found with the given id : " + id));

//        product result = null;
//        for (product product: products.keyset()) {
//            if (product.getid() == id) {
//                result = product;
//                break;
//            }
//        }
//        return result;
    }


    public Product reviewProduct(int id, Rating rating, String comments) {
        try {

            return reviewProduct(findProduct(id),rating,comments);
        }
        catch (ProductManagerException e) {
            logger.log(Level.INFO,e.getMessage());
            return null;
        }
    }

    public void printProductReport(int id) {
        try {
            printProductReport(findProduct(id));
        }
        catch (ProductManagerException e) {
            logger.log(Level.INFO,e.getMessage());
        }
    }


    public void printProducts(Predicate<Product> filter, Comparator<Product> sorter) {
        StringBuilder txt = new StringBuilder();

        txt.append(
                products.keySet().stream()
                        .sorted(sorter)
                        .filter(filter)
                        .map(p -> formatter.formatProduct(p) + '\n')
                        .collect(Collectors.joining())
                );
        System.out.println(txt);
    }

    public Map<String, String> getDiscounts() {
        return products.keySet()
                .stream()
                .collect(
                        Collectors.groupingBy(
                                product -> product.getRating().getStars(),
                                Collectors.collectingAndThen(
                                        Collectors.summingDouble(
                                                product -> product.getDiscount().doubleValue()),
                                                discount -> formatter.moneyformat.format(discount)
                                        )
                                )

                        );

    }


}
