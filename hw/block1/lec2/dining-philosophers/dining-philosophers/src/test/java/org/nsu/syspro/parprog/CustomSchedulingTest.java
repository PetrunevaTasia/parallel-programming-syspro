package org.nsu.syspro.parprog;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nsu.syspro.parprog.base.DefaultFork;
import org.nsu.syspro.parprog.base.DiningTable;
import org.nsu.syspro.parprog.examples.DefaultPhilosopher;
import org.nsu.syspro.parprog.helpers.TestLevels;
import org.nsu.syspro.parprog.interfaces.Fork;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomSchedulingTest extends TestLevels {

    static final class CustomizedPhilosopher extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            sleepMillis(this.id * 20);
            System.out.println(Thread.currentThread() + " " + this + ": onHungry");
            super.onHungry(left, right);
        }
    }
    static final class CustomizedPhilosopher2 extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            sleepMillis(this.id * 2);
            System.out.println(Thread.currentThread() + " " + this + ": onHungry");
            super.onHungry(left, right);
        }
    }

    static final class CustomizedFork extends DefaultFork {
        @Override
        public void acquire() {
            System.out.println(Thread.currentThread() + " trying to acquire " + this);
            super.acquire();
            System.out.println(Thread.currentThread() + " acquired " + this);
            sleepMillis(100);
        }
    }



    static final class CustomizedTable extends DiningTable<CustomizedPhilosopher, CustomizedFork> {
        public CustomizedTable(int N) {
            super(N);
        }

        @Override
        public CustomizedFork createFork() {
            return new CustomizedFork();
        }

        @Override
        public CustomizedPhilosopher createPhilosopher() {
            return new CustomizedPhilosopher();
        }
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testDeadlockFreedom(int N) {
        final CustomizedTable table = dine(new CustomizedTable(N), 1);
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(4)
    void testSingleSlow(int N) {
        BasicTest.BasicTable table = new BasicTest.BasicTable(N);
        table.setPhilosopherAt(1, new CustomizedPhilosopher());
        dine(table, 3);
        assertTrue(table.maxMeals() >= 1000);
    }

    @EnabledIf("mediumEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testWeakFairness(int N) {
        final BasicTest.BasicTable table = new BasicTest.BasicTable(N);
        for(int i = 1; i < N - 1; i += 2){
            table.setPhilosopherAt(i, new CustomizedPhilosopher());
        }
        for(int i = 0; i < N; i += 2){
            table.setPhilosopherAt(i, new CustomizedPhilosopher2());
        }
        dine(table, 1);
        assertTrue(table.minMeals() > 0); // every philosopher eat at least once
    }

    @EnabledIf("hardEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testStrongFairness(int N) {
        final BasicTest.BasicTable table = new BasicTest.BasicTable(N);
        table.setPhilosopherAt(0, new CustomizedPhilosopher());
        dine(table, 1);
        final long minMeals = table.minMeals();
        final long maxMeals = table.maxMeals();
        assertFalse(maxMeals < 1.5 * minMeals); // some king of gini index for philosophers
    }
}
