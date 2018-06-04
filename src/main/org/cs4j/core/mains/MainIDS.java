package org.cs4j.core.mains;

import org.cs4j.core.SearchAlgorithm;
import org.cs4j.core.SearchResultImpl;
import org.cs4j.core.SearchState;
import org.cs4j.core.algorithms.kgoal.KxAstar;
import org.cs4j.core.algorithms.kgoal.LazyKWAstarMin;
import org.cs4j.core.domains.IDSDomain;


import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;

public class MainIDS {
    public static void main(String[] args) {
        int[] storiesRange = {4,8};
        int[] devicesRange = {20, 50};
        SearchAlgorithm kxAstar = new KxAstar();
        SearchAlgorithm lazyKWAstarMin = new LazyKWAstarMin();

        PrintWriter pw = null;
        try {
            pw = new PrintWriter(new File("output.csv"));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("id");
        sb.append(',');
        sb.append("KxAStar Expanded Nodes");
        sb.append(',');
        sb.append("KxAStar Generated Nodes");
        sb.append(',');
        sb.append("lazyKWAstarMin Expanded Nodes");
        sb.append(',');
        sb.append("lazyKWAstarMin Generated Nodes");
        sb.append(',');
        sb.append("Domain Total Nodes");
        sb.append('\n');

        int iterations = 10;


        try {

            for(int i = 0; i < iterations; i++){
                IDSDomain domain = new IDSDomain();
                domain.setupIDSDomain(5, storiesRange, devicesRange, 15);
                SearchResultImpl resKxA = kxAstar.concreteSearch(domain);
                System.out.println(resKxA);
                System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");

                domain.setAllGoalsValid();

                SearchResultImpl resLazy = lazyKWAstarMin.concreteSearch(domain);
                System.out.println(resLazy);
                System.out.println("-------------------------------------------");


                sb.append("" + i);
                sb.append(',');
                sb.append("" + resKxA.getExpanded());
                sb.append(',');
                sb.append("" + resKxA.getGenerated());
                sb.append(',');
                sb.append("" + resLazy.getExpanded());
                sb.append(',');
                sb.append("" + resLazy.getGenerated());
                sb.append(',');
                sb.append("" + domain.getNumOfNodes());
                sb.append('\n');


            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        pw.write(sb.toString());
        pw.close();
        System.out.println("done!");
    }
}
