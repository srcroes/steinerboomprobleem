package utils;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class Utils {
    /* timeout */
    public static long timeout = 300;
    public static TimeUnit timeoutUnit = TimeUnit.SECONDS;

    private Utils() {
    }

    /**
     * get random element from a set
     *
     * @param set set to get a random element from
     * @param <E> Class of set elements
     * @return the randomly chosen element
     */
    public static <E> E getRandomSetElement(Set<E> set) {
        return set.stream().skip(new Random().nextInt(set.size())).findFirst().orElse(null);
    }

    /**
     * computes the symmetric difference between 2 sets
     *
     * @param s1  set 1
     * @param s2  set 2
     * @param <T> type of set elements
     * @return the symmetric difference
     * (items contained in just one of the sets: union(set 1, set 2) \ intersection(set 1, set 2))
     */
    public static <T> Set<T> diff(final Set<? extends T> s1, final Set<? extends T> s2) {
        Set<T> symmetricDiff = new HashSet<>(s1);
        symmetricDiff.addAll(s2);
        Set<T> tmp = new HashSet<>(s1);
        tmp.retainAll(s2);
        symmetricDiff.removeAll(tmp);
        return symmetricDiff;
    }

    /**
     * checks whether the current thread has an interrupt flag and throws an error if so
     *
     * @return true if the interrupt flag is not set
     * @throws InterruptedException if the interrupt flag is set
     */
    public static boolean notInterrupted() throws InterruptedException {
        if (Thread.interrupted()) {
//            System.out.println("throwing interruptedexception");
            throw new InterruptedException();
        } else return true;
    }
}
