// Copyright 2025 Rich Dougherty <rich@rd.nz>

package nz.rd.nonoptest.integration;

public interface SampleInterface {
    default void usedInterfaceMethod1() {
        System.out.println("SampleInterface.usedInterfaceMethod1 called (default method)");
    }
}
