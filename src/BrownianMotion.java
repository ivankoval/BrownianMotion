import java.util.Arrays;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import java.util.Scanner;

class RunnableMasterThread implements Runnable {
    private Thread t;
    private String threadName;
    Modeling modeling;

    RunnableMasterThread(String name, Modeling modeling) {
        threadName = name;
        this.modeling = modeling;
    }

    public void run() {
        int currentIteration = 0;
        while (currentIteration != modeling.iterationNum) {
            boolean completed = true;
            for(boolean value: modeling.isParticleIterated) {
                if(!value) {
                    completed = false;
                }
            }
            if (completed) {
                modeling.initIsParticleIterated();
                if (modeling.modelingMode.equals("1")) {

                    try {
                        t.sleep((long) modeling.delay * 1000);
                    } catch (InterruptedException e) {
                        System.out.println(e);
                    }
                    System.out.println("Distribution of particles in the crystal on " + (currentIteration + 1) + " iteration:" );
                    System.out.println(Arrays.toString(modeling.crystal.crystalState) + "\n");
                }
                modeling.wakeUpAll();
                currentIteration++;
            }
        }

        System.out.println("Final distribution of particles in the crystal:");
        System.out.println(Arrays.toString(modeling.crystal.crystalState));

    }

    public void start() {
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
}

class RunnableParticle implements Runnable {
    private Thread t;
    private String threadName;
    Modeling modeling;

    RunnableParticle(String name, Modeling modeling) {
        threadName = name;
        this.modeling = modeling;
    }

    public void run() {
        modeling.runParticleModeling(Integer.parseInt(threadName));
    }

    public void start() {
        if (t == null) {
            t = new Thread(this, threadName);
            t.start();
        }
    }
}

class Crystal {

    /** crystal parameters */
    public int N;     // length of the crystal
    public int K;    // number of particles
    public float p;    // probability of movement
    public int[] crystalState;     // current crystal state (number of particles in each cell)

    Crystal() {
        setCrystalParams();
    }

    public void setCrystalParams() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter length of the crystal");
        N = Integer.parseInt(scanner.nextLine());

        System.out.println("Enter number of particles");
        K = Integer.parseInt(scanner.nextLine());

        System.out.println("Enter probability of movement right hand side");
        p = Float.parseFloat(scanner.nextLine());

    }

    public void initCrystalState() {
        crystalState = new int[N];
        crystalState[0] = K;
        for (int i = 1; i < N; i++) {
            crystalState[i] = 0;
        }
    }

}

class Modeling {
    /** modeling parameters */
    public String modelingMode;
    public float delay;
    public int iterationNum;
    public boolean isParticleIterated[];

    public Crystal crystal;

    private final Lock lock = new ReentrantLock();

    public void initIsParticleIterated() {
        isParticleIterated = new boolean[crystal.K];
        for (int i = 0; i < crystal.K; i++) {
            isParticleIterated[i] = false;
        }
    }

    public synchronized void setParticleIterated(int particleNum) {
        if(!isParticleIterated[particleNum]) {
            try {
                isParticleIterated[particleNum] = true;
                wait();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public synchronized void wakeUpAll() {
        notifyAll();
    }

    public void runCrystalModeling() {
        System.out.println("Starting modeling...");

        crystal.initCrystalState();

        RunnableParticle[] runnableParticles = new RunnableParticle[crystal.K];
        for (int i = 0; i < crystal.K; i++) {
            runnableParticles[i] = new RunnableParticle(Integer.toString(i), this);
            runnableParticles[i].start();
        }

        RunnableMasterThread runnableMasterThread = new RunnableMasterThread("masterThread", this);
        runnableMasterThread.start();
    }

    public void runParticleModeling(int particleNum) {
        int currentParticlePosition = 0;

        for(int i = 0; i < iterationNum; i++) {

            int nextParticlePosition = setNewParticlePosition(currentParticlePosition);
            if(currentParticlePosition != nextParticlePosition) {

                lock.lock();

                try {
                    crystal.crystalState[currentParticlePosition]--;
                    crystal.crystalState[nextParticlePosition]++;
                } finally {
                    lock.unlock();
                }

                currentParticlePosition = nextParticlePosition;
            }

            setParticleIterated(particleNum);
        }
    }

    public int setNewParticlePosition(int currentPosition) {
        int direction = getBernoulliRandom(crystal.p);

        if ((currentPosition == (crystal.N-1)  && direction == 1) ||
                (currentPosition == 0 && direction == 0)) {
            return currentPosition;
        }

        return (direction == 1) ? currentPosition + 1 : currentPosition - 1;
    }

    public int getBernoulliRandom(float p) {
        int x = 0;
        if(Math.random() < p)
            x++;
        return x;
    }

    public Modeling(String modelingMode, float delay, int iterationNum) {
        this.modelingMode = modelingMode;
        this.delay = delay;
        this.iterationNum = iterationNum;

        crystal = new Crystal();
        initIsParticleIterated();
        runCrystalModeling();

    }

    public Modeling(String modelingMode, int iterationNum) {
        this.modelingMode = modelingMode;
        this.iterationNum = iterationNum;

        crystal = new Crystal();
        initIsParticleIterated();
        runCrystalModeling();
    }

}

public class BrownianMotion {

    public static String modelingMode;
    public static float executionTime;
    public static float delay;
    public static int iterationNum;

    public static void setModelingParams() {
        Scanner scanner = new Scanner(System.in);

        System.out.println("Please choose modeling mode:\n" +
                "1. 'restriction by time'\n" +
                "2. 'restriction by the number of iteration'\n" +
                "Type '1' or '2':");

        modelingMode = scanner.nextLine();

        while(!modelingMode.equals("1") && !modelingMode.equals("2")) {
            System.out.println("Sorry, you entered invalid symbol. Please choose between '1' or '2'");
            modelingMode = scanner.nextLine();
        }

        if (modelingMode.equals("1")) {
            System.out.println("You've picked restriction by time mode.\n" +
                    "Enter time of the program execution (in seconds):");
            executionTime = Float.parseFloat(scanner.nextLine());

            System.out.println("Enter delay between each iteration (in seconds):");
            delay = Float.parseFloat(scanner.nextLine());

            iterationNum = (int) (executionTime/delay);
            System.out.println("Calculated nymber of iterations: " + iterationNum);
        }

        if (modelingMode.equals("2")) {
            System.out.println("You've chosen restriction by iteration mode.\n" +
                    "Enter number of iterations:");
            iterationNum = Integer.parseInt(scanner.nextLine());
        }

    }

    public static void main(String args[]) {

        setModelingParams();

        if (modelingMode.equals("1")) {
            Modeling modeling = new Modeling(modelingMode, delay, iterationNum);
        }
        if (modelingMode.equals("2")) {
            Modeling modeling = new Modeling(modelingMode, iterationNum);
        }


    }
}
