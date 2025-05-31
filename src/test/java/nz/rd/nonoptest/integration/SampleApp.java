// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonoptest.integration;

public class SampleApp extends SampleSuperClass implements SampleInterface {
    public SampleApp() {
        System.out.println("SampleApp constructor");
    }

    public void usedMethod1() {
        System.out.println("SampleApp.usedMethod1 called");
        super.usedSuperClassMethod1();
        usedMethod2();
    }

    private void usedMethod2() {
        System.out.println("SampleApp.usedMethod2 called");
        usedInterfaceMethod1();
    }

    public static void usedStaticMethod3() {
        System.out.println("SampleApp.usedStaticMethod3 called");
    }

    public void unusedMethod() {
        System.out.println("SampleApp.unusedMethod called - THIS SHOULD NOT APPEAR IF NOT CALLED");
    }

    public static void main(String[] args) {
        System.out.println("SampleApp main starting...");
        SampleApp app = new SampleApp();
        app.usedMethod1();
        SampleApp.usedStaticMethod3();
        // app.unusedMethod(); // Keep this commented out
        System.out.println("SampleApp main finished. Waiting a bit for agent report...");
        try {
            Thread.sleep(4000); // Give shutdown hook a moment
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}