package utils.setutils;


import java.util.*;
import java.util.stream.Collectors;

/**
 * custom powerset implementation that iterates over subsets in order of size
 * also supports only iterating over subsets of a certain length (e.g. 3 for triples)
 *
 * @param <E> type of the elements
 */
public class PowerSet<E> implements Iterator<Set<E>>, Iterable<Set<E>> {
    private E[] arr = null;
    private final boolean fixedLen;
    private BitSet bset = null;
    private boolean last = false;
    private final Set<E> set;
    private final int startSize;

    public PowerSet(Set<E> set) {
        this(set, 0, false);
    }

    public PowerSet(Set<E> set, int startSize) {
        this(set, startSize, false);
    }

    @SuppressWarnings("unchecked")
    public PowerSet(Set<E> set, int startSize, boolean fixedLen) {
        arr = (E[]) set.toArray();
        this.fixedLen = fixedLen;
        bset = new BitSet(arr.length + 1);
        bset.set(0, startSize);

        this.set = set;
        this.startSize = startSize;
    }

    @Override
    public boolean hasNext() {
        return !(fixedLen && last) && !bset.get(arr.length);
    }

    @Override
    public Set<E> next() {
        Set<E> returnSet = bset.stream().mapToObj(v -> arr[v]).collect(Collectors.toSet());
        int start = bset.nextClearBit(Math.max(0, bset.nextSetBit(0)));
        if (start > 0 && start < arr.length) {
            // case: we can flip the rightmost 01 sequence
            bset.flip(start - 1, start + 1);
            int count = 0;
            // count 1 bits to the right of 01 sequence
            for (int i = start - 2; i >= 0; i--) {
                if (bset.get(i)) count += 1;
                else break;
            }
            // clear all bits to the right of 01 sequence
            bset.clear(0, start - 1);
            // set #count bits starting from the right
            bset.set(0, count);
        } else {
            if (fixedLen) {
                last = true;
            } else {
                // case: move on to next subset size
                int size = bset.cardinality() + 1;
                bset.clear();
                bset.set(0, size);
            }
        }
        return returnSet;
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Not Supported!");
    }

    @Override
    public Iterator<Set<E>> iterator() {
        return new PowerSet<>(this.set, this.startSize, this.fixedLen);
    }

    // TODO move these tests to separate test file

    public static void testPowerset() {
        Set<Integer> integers = Set.of(1, 2, 3, 4, 5, 6, 7, 8);
        PowerSet<Integer> powerSet = new PowerSet<>(integers);
        HashMap<Integer, List<Integer>> values = new HashMap<>();
        while (powerSet.hasNext()) {
            Set<Integer> set = powerSet.next();
            int sum = set.stream().mapToInt(v -> (int) Math.pow(2, v - 1.0)).sum();
            int size = set.size();
            values.computeIfAbsent(size, v -> new ArrayList<>());
            values.get(size).add(sum);
        }
        System.out.println(values);
        int count = 0;
        for (List<Integer> entry : values.values()) {
            List<Integer> sorted = entry.stream().sorted().toList();
            assert entry.equals(sorted);
            count += entry.size();
        }
        assert count == (int) Math.pow(2, integers.size());
        System.out.println("size-sorted powerset generator works");
    }

    public static void testFixedLenPowerSet() {
        PowerSet<Integer> powerSet = new PowerSet<>(Set.of(1, 2, 3, 4, 5, 6, 7, 8), 3, true);
        int count = 0;
        while (powerSet.hasNext()) {
            Set<Integer> set = powerSet.next();
            System.out.println(set);
            count++;
        }
        assert count == 56;
    }

}