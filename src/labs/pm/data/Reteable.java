/*
 * Copyright (c) 2024. Lorem ipsum dolor sit amet, consectetur adipiscing elit.
 * Morbi non lorem porttitor neque feugiat blandit. Ut vitae ipsum eget quam lacinia accumsan.
 * Etiam sed turpis ac ipsum condimentum fringilla. Maecenas magna.
 * Proin dapibus sapien vel ante. Aliquam erat volutpat. Pellentesque sagittis ligula eget metus.
 * Vestibulum commodo. Ut rhoncus gravida arcu.
 */

package labs.pm.data;

/**
 * @author amz
 **/

@FunctionalInterface
public interface Reteable<T> {

    public static final Rating DEFAULT_RATING = Rating.NOT_RATED;


    public abstract T applyRating(Rating rating);

    public default Rating getRating() {
        return DEFAULT_RATING;
    }

    public static Rating convert(int stars) {
        return (stars >= 0 && stars <= 5) ? Rating.values()[stars] : Rating.NOT_RATED;
    }

    public default T applyRating(int stars) {
        return applyRating(convert(stars));
    }
}
