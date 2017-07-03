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

/**
 * The search algorithm interface.
 *
 * @author Matthew Hatem
 */
public interface SearchAlgorithm extends SearchConfigurable {

    /**
     * @return The name of the algorithm
     */
    String getName();

    SearchResultImpl concreteSearch(MultipleGoalsSearchDomain domain);

    SearchResultImpl concreteSearch(SingleGoalSearchDomain domain);


    SearchResultImpl search(SearchDomain domain);

    boolean improveStateHValue(SearchState s, SearchDomain domain);

    boolean concreteImproveStateHValue(SearchState s, SingleGoalSearchDomain domain);

    boolean concreteImproveStateHValue(SearchState s, MultipleGoalsSearchDomain domain);

}
