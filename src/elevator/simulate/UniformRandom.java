/*
 * Created by Zhen Guan
 * Student number: 202191382
 * Email: zguan@mun.ca
 */

package elevator.simulate;

import java.util.Date;
import java.util.Random;

public class UniformRandom {
    Random random = new Random(new Date().getTime());

    public int nextInt(int minInclusive, int maxInclusive) {
        return random.nextInt(maxInclusive - minInclusive + 1) + minInclusive;
    }

    public boolean nextBoolean() {
        return random.nextBoolean();
    }

    public Object nextRandomItemOf(Object[] values) {
        return values[random.nextInt(values.length)];
    }
}
