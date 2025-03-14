package org.nsu.syspro.parprog.base;

import org.nsu.syspro.parprog.interfaces.Fork;
import org.nsu.syspro.parprog.interfaces.Philosopher;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public abstract class DiningTable<P extends Philosopher, F extends Fork> {
    private final ArrayList<F> forks;
    private final ArrayList<P> phils;
    private final ArrayList<Long> numberOfMealsForEachPhilosopher;
    private final ExecutorService executorService;

    private boolean started;
    private volatile boolean shouldStop;

    public DiningTable(int N) {
        if (N < 2) {
            throw new IllegalStateException("Too small dining table");
        }

        started = false;
        forks = new ArrayList<>(N);
        phils = new ArrayList<>(N);
        numberOfMealsForEachPhilosopher = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            forks.add(createFork());
            phils.add(createPhilosopher());
            numberOfMealsForEachPhilosopher.add(0L);
        }
        executorService = Executors.newCachedThreadPool();
    }

    public synchronized void start() {
        if (started) {
            throw new IllegalStateException("Restart is not supported");
        }

        final int N = phils.size();
        for (int i = 0; i < N; i++) {
            final Philosopher p = phils.get(i);
            final Fork left = forks.get(i);
            final Fork right = forks.get((i + 1) % N);
            int finalI = i;
            executorService.submit(() -> {
                long count = 0;
                long startNum;
                long minValFromNeighbor;
                while (!executorService.isShutdown()) {

                    synchronized (numberOfMealsForEachPhilosopher){
                        startNum = numberOfMealsForEachPhilosopher.get(finalI);
                        minValFromNeighbor = getMinValOfMealsFromNeighbors();
                    }
                    if((startNum + count) >= 1.5 * minValFromNeighbor){
                        synchronized (numberOfMealsForEachPhilosopher) {
                            numberOfMealsForEachPhilosopher.set(finalI, startNum + count);
                        }
                        count = 0;
                        try {
                            Thread.sleep(2);
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    }
                    p.onHungry(left, right);
                    count++;
                }
            });
        }

        started = true;
    }

    public synchronized void stop() {
        if (shouldStop) {
            throw new IllegalStateException("Repeated stop is illegal");
        }

        if (!started) {
            throw new IllegalStateException("Start first");
        }

        shouldStop = true;
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(800, TimeUnit.DAYS)) {
                throw new IllegalStateException("Stop failed");
            }
        } catch (InterruptedException e) {
            throw new IllegalStateException("Stop interrupted.", e);
        }
    }

    public P philosopherAt(int index) {
        return phils.get(index);
    }

    public F forkAt(int index) {
        return forks.get(index);
    }

    public long maxMeals() {
        return phils.stream()
                .mapToLong(Philosopher::meals)
                .max()
                .getAsLong();
    }

    public long minMeals() {
        return phils.stream()
                .mapToLong(Philosopher::meals)
                .min()
                .getAsLong();
    }

    public long totalMeals() {
        return phils.stream()
                .mapToLong(Philosopher::meals)
                .sum();
    }

    public abstract F createFork();

    public abstract P createPhilosopher();


    public void setPhilosopherAt(int index, P philosopher){
        phils.set(index, philosopher);
    }

    private long getMinValOfMealsFromNeighbors(){
        return Collections.min(numberOfMealsForEachPhilosopher);
    }
}
