/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package labs.pm.data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

/**
 * {@code Product} class represents  properties and behaviours of product objects in the product management Systems
 * <br>
 * each product has an id, name and price
 * </br>
 * each product can have a discount, it can be calculated based on
 * {@link Product.DISCOUNT_RATE DISCOUNT_RATE}
 * @version 4.0
 * @author amz
 **/
public sealed class Product implements Reteable<Product>, Serializable permits Food,Drink {
    private int id;
    private String name;
    /**
     * price of the product of type {@link BigDecimal}
     */
    private BigDecimal price;
    private Rating rating;

    /**
     * A constant that defines
     * {@link BigDecimal BigDecimal} discount value for the product
     * <br>
     * discount rate is 10%
     */
    public static final BigDecimal DISCOUNT_RATE = BigDecimal.valueOf(0.1);

    Product(int id, String name, BigDecimal price, Rating rating) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.rating = rating;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BigDecimal getPrice() {
        return price;
    }


    /**
     * calculates discount based on
     * {@link price price} and {@link DISCOUNT_RATE discount rate}
     *
     * @return BigDecimal
     */
    public BigDecimal getDiscount() {
        return this.price.multiply(DISCOUNT_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public Rating getRating() {
        return rating;
    }
    public Product applyRating(Rating newRating) {
        return new Product(getId(),getName(),getPrice(),newRating);
    }

    /**
     * assumes the bestbefore days is the current day.
     * @return the current day
     */
    public LocalDate getBestBefore() {
        return LocalDate.now();
    }

    @Override
    public String toString() {
        return id + ", " + name + ", " + price + ", " + rating.getStars() + ", " + getBestBefore();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Product product)) return false;
        return id == product.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
