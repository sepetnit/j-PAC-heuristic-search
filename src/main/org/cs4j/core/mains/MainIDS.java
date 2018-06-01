package org.cs4j.core.mains;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.algorithms.kgoal.KxAstar;
import org.cs4j.core.domains.IDSDomain;

import java.io.IOException;

public class MainIDS {
    public static void main(String[] args) {
        IDSDomain domain = new IDSDomain();
        int[] storiesRange = {2,3};
        int[] devicesRange = {5, 10};
        SearchAlgorithm alg = new KxAstar();

        try {
            domain.setupIDSDomain(3, storiesRange, devicesRange, 10);
            domain.printDomain();
//            alg.concreteSearch(domain);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
