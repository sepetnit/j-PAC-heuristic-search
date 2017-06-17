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
package org.cs4j.core.collections;

/**
 * The SearchQueueElement interface (see intrusive data structures)
 * 
 * @author Matthew Hatem
 */
public interface SearchQueueElement {

    /**
     * Sets the indexes of this object in the data structure.
     *
     * @param key The key in the map, whose place should be filled with the
     *            given value
     * @param index The value to set
     */
    void setIndex(int key, int index);

    /**
     * Returns the index of this object in the data structure.
     *
     * @return The found index
     */
    int getIndex(int key);

    /**
     *
     * @return Element's F value;
     */
    double getF();

    /**
     *
     * @return Element's G value;
     */
    double getG();

    /**
     *
     * @return Element's Depth value;
     */
    double getDepth();

    /**
     *
     * @return Element's H (heuristic) value;
     */
    double getH();

    /**
     *
     * @return Elements D value;
     */
    double getD();

    /**
     *
     * @return Elements Hhat value;
     */
    public double getHhat();

    /**
     *
     * @return Elements Dhat value;
     */
    public double getDhat();

    /**
     * @return Element's parent element;
     */
    SearchQueueElement getParent();
}
