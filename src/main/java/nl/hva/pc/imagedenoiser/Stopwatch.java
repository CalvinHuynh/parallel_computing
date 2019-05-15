package nl.hva.pc.imagedenoiser;

// source at https://algs4.cs.princeton.edu/14analysis/Stopwatch.java.html
/******************************************************************************
 *  Compilation:  javac Stopwatch.java
 *  Execution:    java Stopwatch n
 *  Dependencies: none
 *
 *  A utility class to measure the running time (wall clock) of a program.
 *
 *  % java8 Stopwatch 100000000
 *  6.666667e+11  0.5820 seconds
 *  6.666667e+11  8.4530 seconds
 *
 ******************************************************************************/

/**
 *  The {@code Stopwatch} data type is for measuring
 *  the time that elapses between the start and end of a
 *  programming task (wall-clock time).
 *
 *  See {@link StopwatchCPU} for a version that measures CPU time.
 *  
 *  @author Robert Sedgewick
 *  @author Kevin Wayne
 */

public class Stopwatch { 

    private static long start;

    /**
     * Initializes a new stopwatch.
     */
    public Stopwatch() {
        start = System.nanoTime();
    } 
    
    /**
     * Resets the timer
     */
    public void reset(){
        start = 0;
    }
    
    /**
     * Starts the timer
     */
    public void start(){
        start = 0;
        start = System.nanoTime();
    }
    
    /**
     * Returns the elapsed CPU time (in seconds) since the stopwatch was created.
     *
     * @return elapsed CPU time (in seconds) since the stopwatch was created
     */
    public long elapsedTime() {
        long now = System.nanoTime();
        return (now - start);
    }
} 