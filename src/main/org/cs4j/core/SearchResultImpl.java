/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.cs4j.core;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.text.DecimalFormat;
import java.util.*;

/**
 * The search result class.
 *
 * @author Matthew Hatem
 */
public class SearchResultImpl /*implements SearchResultImpl*/ {

    // Number of expanded states
    public long expanded;
    // Number of generated states
    public long generated;
    // Number of duplicates: states that were reached more than a single time
    public long duplicates;
    // Number of states that were reached a second time, while they are still in OPEN list (updating their values)
    public long opupdated;
    // Number of states that were actually re-opened
    public long reopened;

    private boolean problemSolved;

    private long startWallTimeMillis;
    private long startCpuTimeMillis;
    private long stopWallTimeMillis;
    private long stopCpuTimeMillis;

    private TreeMap<String, Long> arrStartCpuTimeMillis = new TreeMap<>();;
    private TreeMap<String, Long> arrCpuTimeMillis = new TreeMap<>();
    private TreeMap<String, Long> arrCpuTimeMillisCalled = new TreeMap<>();
    private TreeMap extras = new TreeMap();

    public List<Iteration> iterations = new ArrayList<>();
    private List<Solution> solutions = new ArrayList<>();
    private List<SearchResultImpl> concreteResults = new ArrayList<>();
    private int minTimeOutInMs;

    public void setExtras(String key,Object val){
        extras.put(key,val);
    }

    public TreeMap<String,Object> getExtras(){
        return extras;
    }

    public SearchResultImpl() {}

    // TODO: Copy time
    public SearchResultImpl(SearchResultImpl result) {
        this.startWallTimeMillis = result.startWallTimeMillis;
        this.startCpuTimeMillis = result.startCpuTimeMillis;
        this.minTimeOutInMs = result.minTimeOutInMs;
    }

    public void startArrCpuTimeMillis(String name){
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long startTime = bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1L;
        arrStartCpuTimeMillis.put(name,startTime);
    }

    public void stopArrCpuTimeMillis(String name){
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        long arrStopCpuTimeMillis = bean.isCurrentThreadCpuTimeSupported() ? bean.getCurrentThreadCpuTime() : -1L;
        long startCpuTimeMillis = arrStartCpuTimeMillis.get(name);
        long passed = arrStopCpuTimeMillis - startCpuTimeMillis;
        long totalPassed = 0;
        if(arrCpuTimeMillis.containsKey(name)) {
            totalPassed = arrCpuTimeMillis.get(name);
        }
        totalPassed +=passed;
        arrCpuTimeMillis.put(name,totalPassed);

        long called = 0;
        if(arrCpuTimeMillisCalled.containsKey(name)) {
            called = arrCpuTimeMillisCalled.get(name);
        }
        called +=1;
        arrCpuTimeMillisCalled.put(name,called);
    }

    public void printArrCpuTimeMillis(){
        DecimalFormat formatter = new DecimalFormat("#,###");
        long val;
        long called = 0;
        for(Map.Entry<String, Long> entry : arrCpuTimeMillis.entrySet()) {
            if(arrCpuTimeMillisCalled.containsKey(entry.getKey())) {
                called = arrCpuTimeMillisCalled.get(entry.getKey());
            }
            val = (long) (entry.getValue() * 0.000001);
            System.out.println(entry.getKey() +"\t: "+formatter.format(val)+"\tcalled: "+formatter.format(called));
            entry.getKey();
            entry.getValue();
        }
    }

    //@Override
    public long getExpanded() {
        return this.expanded;
    }

    //@Override
    public long getFirstIterationExpanded() {
        if (this.iterations.size() > 0) {
            Iteration current = this.iterations.get(0);
            return current.getExpanded();
        }
        return this.expanded;
    }

    //@Override
    public long getGenerated () {
        return this.generated;
    }

    //@Override
    public double getAverageExpanded() {
        if (this.concreteResults.size() == 0) {
            return this.getExpanded();
        }
        double toReturn = 0;
        for (SearchResultImpl result: this.concreteResults) {
            toReturn += result.getExpanded();
            System.out.println(toReturn);
        }
        return toReturn / (this.concreteResults.size());
    }

    //@Override
    public long getReopened () {
        return this.reopened;
    }

    //@Override
    public long getDuplicates() {
        return this.duplicates;
    }

    //@Override
    public long getUpdatedInOpen() {
        return this.opupdated;
    }

    //@Override
    public boolean hasSolution() {
        return this.solutions != null && this.solutions.size() > 0;
    }

    //@Override
    public boolean problemSolved() {
        return this.problemSolved;
    }

    //@Override
    public void setProblemSolved() {
        this.problemSolved = true;
    }

    //@Override
    public List<Solution> getSolutions() {
        return this.solutions;
    }

    //@Override
    public List<SearchResultImpl> getConcreteResults() {
        return this.concreteResults;
    }

    //@Override
    public void addConcreteResult(SearchResultImpl result) {
        this.concreteResults.add(result);
    }

    //@Override
    public int solutionsCount() {
        return this.solutions.size();
    }

    /**
     * If multiple solutions were found, return the best one
     * (this is assumed to be the last one) #TODO: Is this assumption Ok?
     * @return the best solution found
     */
    public Solution getBestSolution() {return this.solutions.get(this.solutions.size()-1);}

    //@Override
    public long getWallTimeMillis() {
        return stopWallTimeMillis - startWallTimeMillis;
    }

    //@Override
    public long getCpuTimeMillis() {
        return (long)((stopCpuTimeMillis - startCpuTimeMillis) * 0.000001);
    }

    public void increaseCpuTimeMillis(long time) {
        this.stopCpuTimeMillis += time;
    }

    public void increaseWallTimeMillis(long time) {
        this.stopWallTimeMillis += time;
    }

    public void addSolution(Solution solution) {
        solutions.add(solution);
    }

    public void addIteration(int i, double bound, long expanded, long generated) {
        iterations.add(new Iteration(bound, expanded, generated));
    }

    public void addIterations(SearchResultImpl other) {
        other.iterations.addAll(this.iterations);
        this.iterations = other.iterations;
    }

    public void setExpanded(long expanded) {
        this.expanded = expanded;
    }

    public void setGenerated(long generated) {
        this.generated = generated;
    }

    public void increase(SearchResultImpl previous) {
        this.stopTimer(); // To record the stop wall and stop cpu times
        this.increaseStatesCounters(previous);
    }

    public void increaseStatesCounters(SearchResultImpl previous) {
        this.expanded += previous.getExpanded();
        this.generated += previous.getGenerated();
        this.reopened += previous.getReopened();
        this.opupdated += previous.getUpdatedInOpen();
    }

    public void startTimer() {
        this.startWallTimeMillis = System.currentTimeMillis();
        this.startCpuTimeMillis = getCpuTime();
        this.minTimeOutInMs = 1000*60*20;
    }

    public void stopTimer() {
        this.stopWallTimeMillis = System.currentTimeMillis();
        this.stopCpuTimeMillis = getCpuTime();
    }

    public boolean checkMinTimeOut(){
        long passed = getWallTimePassedInMS();
        return passed < this.minTimeOutInMs;
    }

    public long getWallTimePassedInMS(){
        return System.currentTimeMillis() - this.startWallTimeMillis;
    }

    public String printWallTime(){
        int duration = (int) getWallTimeMillis();
        int ms = duration%1000;
        String ret = "ms:"+ms;
        duration /=1000;

        int sec = duration%60;
        if(sec > 0) ret="sec:"+sec+","+ret;
        duration /=60;

        int min = duration;
        if(min > 0) ret="min:"+min+","+ret;
        return ret;
    }

    public long getCpuTimePassedInMs(){
        long ret = (long)((getCpuTime() - this.startCpuTimeMillis) * 0.000001);
        return ret;
    }

    private long getCpuTime() {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        return bean.isCurrentThreadCpuTimeSupported() ?
                bean.getCurrentThreadCpuTime() : -1L;
    }
  
  /*
   * Returns the machine Id
   */
  /*private String getMachineId() {
    String uname = "unknown";
    try {
      String switches[] = new String[] {"n", "s", "r", "m"};
      String tokens[] = new String[4];
      for (int i=0; i<switches.length; i++) {
        Process p = Runtime.getRuntime().exec("uname -"+switches[i]);
        p.waitFor();
        BufferedReader reader = new BufferedReader(
            new InputStreamReader(p.getInputStream()));
        tokens[i] = reader.readLine();
      }
      uname = tokens[0]+"-"+tokens[1]+"-"+tokens[2]+"-"+tokens[3];
    } catch (IOException e) {
      e.printStackTrace();
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
    
    return uname;
  }*/

    /*
     * The iteration class.
     */
    private static class Iteration {//implements SearchResultImpl.Iteration {
        private double b;
        private long e;
        private long g;

        public Iteration(double b, long e, long g) {
            this.b = b;
            this.e = e;
            this.g = g;
        }

        //@Override
        public double getBound() {
            return this.b;
        }
        //@Override
        public long getExpanded() {
            return this.e;
        }
        //@Override
        public long getGenerated() {
            return this.g;
        }
    }

    @Override
    public String toString() {
        StringBuffer sb = new StringBuffer();
        sb.append("Nodes Generated: ");sb.append(generated);sb.append("\n");
        sb.append("Nodes Expanded: ");sb.append(expanded);sb.append("\n");
        sb.append("Total Wall Time: "+this.getWallTimeMillis());sb.append("\n");
        sb.append("Total CPU Time: "+this.getCpuTimeMillis());sb.append("\n");
        // TODO: Currently only first solution is taken
        if (solutions.size() > 0) {
            sb.append(solutions.get(0));
        }
        return sb.toString();
    }

    public static class SolutionImpl implements Solution {
        private SearchDomain domain;
        private double cost;
        private List<Operator> operators = new ArrayList<>();
        private List<SearchState> states = new ArrayList<>();

        /**
         * A default constructor of the class
         */
        public SolutionImpl() {
            this.domain = null;
        }

        /**
         * A constructor of the class
         *
         * @param domain The domain on which the search was ran
         */
        public SolutionImpl(SearchDomain domain) {
            this.domain = domain;
        }


        @Override
        public List<Operator> getOperators() {
            return this.operators;
        }

        @Override
        public List<SearchState> getStates() {
            return this.states;
        }

        @Override
        public double getCost() {
            return this.cost;
        }

        @Override
        public int getLength() {
            return this.operators.size();
        }

        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            sb.append("Cost: ");
            sb.append(getCost());
            sb.append("\n");
            sb.append("Length: ");
            sb.append(getLength());
            sb.append("\n");
            return sb.toString();
        }

        public String dumpSolution() {
            if (this.domain != null) {
                // First, let's try dump the states of the solution, using a specific function that may be provided
                // by the domain
                String domainStatesCollectionDump = this.domain.dumpStatesCollection(
                        this.states.toArray(new SearchState[this.states.size()]));
                if (domainStatesCollectionDump != null) {
                    return domainStatesCollectionDump;
                }
            }
            // Otherwise, let's dump state by state
            StringBuffer sb = new StringBuffer();
            for (SearchState state: this.states) {
                sb.append(state.dumpState());
//                sb.append(state.dumpStateShort());
//                sb.append("\n");
            }
            return sb.toString();
        }

        public void addOperator(Operator operator) {
            this.operators.add(operator);
        }

        public void addOperators(List<Operator> operators) {
            for (Operator o : operators) {
                this.operators.add(o);
            }
        }

        public Operator removeLastOperator() {
            Operator toReturn = null;
            if (this.operators.size() > 0) {
                toReturn = this.operators.get(this.operators.size() - 1);
                this.operators.remove(this.operators.size() - 1);
            }
            return toReturn;
        }

        public void addState(SearchState state) {
            this.states.add(state);
        }

        public SearchState removeLastState() {
            SearchState toReturn = null;
            if (this.states.size() > 0) {
                toReturn = this.states.get(this.states.size() - 1);
                this.states.remove(this.states.size() - 1);
            }
            return toReturn;
        }

        public void addStates(List<SearchState> states) {
            for (SearchState o : states) {
                this.states.add(o);
            }
        }

        public void reverseAll() {
            Collections.reverse(this.states);
            Collections.reverse(this.operators);
        }

        public SearchState getGoal() {
            return this.states.get(this.states.size() - 1);
        }

        public void setCost(double cost) {
            this.cost = cost;
        }

    }

    /**
     * The Solution interface.
     */
    public interface Solution {

        /**
         * Returns a list of operators used to construct this solution.
         *
         * @return list of operators
         */
        List<Operator> getOperators();

        /**
         * Returns a list of states used to construct this solution
         *
         * @return list of states
         */
        List<SearchState> getStates();

        /**
         * Returns a string representation of the solution
         *
         * @return A string that represents the solution
         */
        String dumpSolution();

        /**
         * Returns the cost of the solution.
         *
         * @return the cost of the solution
         */
        double getCost();

        /**
         * Returns the length of the solution.
         */
        int getLength();

        /**
         *
         * @return The goal of the solution
         */
        SearchState getGoal();

    }


}
