/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package labs.pm.data;

import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.text.MessageFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.Instant;
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

    private Path reportsFolder = Path.of(config.getString("reports.folder"));
    private Path dataFolder = Path.of(config.getString("data.folder"));
    private Path tempFolder = Path.of(config.getString("temp.folder"));

    public Review parseReview(String text) {
        try {
            Object[] values = reviewFormat.parse(text);

            return new Review(Reteable.convert(Integer.parseInt((String) values[0])),(String)values[1]);
        }
        catch (ParseException| NumberFormatException e) {
            logger.log(Level.WARNING, "Error parsing review : "+ text + " " + e.getMessage());

            return null;
        }
    }

    public Product parseProduct(String text) throws ProductManagerException {
        Product product = null;
        try {
            Object[] values = productFormat.parse(text);

            int id = Integer.parseInt((String) values[1]);
            String name = (String) values[2];
            BigDecimal price = BigDecimal.valueOf(Double.parseDouble((String) values[3]));
            Rating rating = Reteable.convert(Integer.parseInt((String) values[4]));

            switch ((String) values[0]) {
                case "F":
                    LocalDate bestBefore = LocalDate.parse((String) values[5]);
                    product = createProduct(id,name,price,rating,bestBefore);
                    break;
                case "D":
                    product = createProduct(id,name,price,rating);
                    break;

            }

        }
        catch (ParseException | NumberFormatException | DateTimeParseException e) {
            logger.log(Level.WARNING, "Error parsing review : "+ text + " " + e.getMessage());

            throw new ProductManagerException("Unable to parse review",e);
        }
        return product;
    }
    private Product loadProduct(Path file) {
        Product product = null;
        try {
            product = parseProduct(Files.lines(dataFolder.resolve(file),Charset.forName("UTF-8"))
                    .findFirst().orElseThrow());
        } catch (IOException | ProductManagerException e) {
            logger.log(Level.WARNING, "Error loading Product "+ e.getMessage());
        }
        return product;
    }
    private void loadAllProducts() {
        try {
        products = Files.list(dataFolder)
                .filter(file -> file.getFileName().toString().startsWith("product"))
                .map(this::loadProduct)
                .filter(product -> product != null)
                .collect(Collectors.toMap(product -> product, product -> loadReviews(product)));

        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Error loading Data "+ e.getMessage());
        }

    }
    private List<Review> loadReviews(Product product) {
        Path file = dataFolder.resolve(MessageFormat.format(config.getString("reviews.data.file"),product.getId()));

        List<Review> reviews = null;

        if (Files.notExists(file)) {
            reviews = new ArrayList<>();
        }
        else {
            try {

            reviews = Files.lines(file, Charset.forName("UTF-8"))
                    .map(text -> parseReview(text))
                    .filter(review -> review != null)
                    .collect(Collectors.toList());
            } catch (IOException e) {
                logger.log(Level.WARNING, "Error loading reviews " + e.getMessage());
            }


        }

        return reviews;




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
        loadAllProducts();

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


    public void printProductReport(Product product) throws IOException {

        String path = MessageFormat.format(config.getString("report.file"),product.getId());
        Path productFile = reportsFolder.resolve(path);


        try(PrintWriter out = new PrintWriter(new OutputStreamWriter(Files.newOutputStream(productFile, StandardOpenOption.CREATE),"UTF-8"))) {
            out.append(formatter.formatProduct(product) + System.lineSeparator());


            List<Review> reviews = products.get(product);

            sort(reviews);


            if (reviews.isEmpty()) {
                out.append(formatter.getText("no.reviews")+ System.lineSeparator());
            }
            else {
                out.append(
                        reviews.stream()
                                .map(r->formatter.formatReview(r) + System.lineSeparator())
                                .collect(Collectors.joining())
                );
            }



        }

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
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Error printing product report " + e.getMessage());
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

    public void dumpData() {
        try {
            if (Files.notExists(tempFolder)) {
                Files.createDirectories(tempFolder);
            }
            Path tempFile = tempFolder.resolve(MessageFormat.format(config.getString("temp.file"), Instant.now()));

            try (ObjectOutputStream out = new ObjectOutputStream(Files.newOutputStream(tempFile, StandardOpenOption.CREATE))) {
                out.writeObject(products);
                products = new HashMap<>();

            }

        }
        catch (IOException e) {
            logger.log(Level.SEVERE, "Error dumping data" + e.getMessage() ,e);
        }
    }

    @SuppressWarnings("unchecked")
    public void restoreData() {
        try {
            Path tempFile = Files.list(tempFolder)
                    .filter(path -> path.getFileName().toString().endsWith("temp"))
                    .findFirst()
                    .orElseThrow();

            try (ObjectInputStream in = new ObjectInputStream(Files.newInputStream(tempFile, StandardOpenOption.DELETE_ON_CLOSE))) {
                products = (HashMap) in.readObject();

            }
        }
        catch (Exception ex) {
            logger.log(Level.SEVERE, "Error restoring data "+ ex.getMessage(), ex);
        }
    }
}
