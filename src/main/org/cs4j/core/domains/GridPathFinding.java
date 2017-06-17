package org.cs4j.core.domains;

import org.cs4j.core.*;
import org.cs4j.core.collections.PackedElement;
import org.cs4j.core.collections.Pair;
import org.cs4j.core.collections.PairInt;
import org.junit.Assert;

import java.io.*;
import java.util.*;

/**
 * Represents some grid (not a full problem!!!, only the grid!!!
 * <p/>
 * Note that the internal grid is represented as an array of integers
 * <p/>
 * <p>
 * Note: The grid is 1-based
 * </p>
 */
public class GridPathFinding extends MultipleGoalsSearchDomain {
    private static final int NUM_MOVES = 4;

    public static final char OBSTACLE_MARKER = '@';
    public static final char START_MARKER = 'S';
    public static final char GOAL_MARKER = 'G';

    private static final Map<String, Class> GridPathFindingPossibleParameters;

    private static final int RANDOM_PIVOTS_INDEXES_COUNT = 5000000;

    // Declare the parameters that can be tunes before running the search
    static
    {
        GridPathFindingPossibleParameters = new HashMap<>();
        GridPathFinding.GridPathFindingPossibleParameters.put("heuristic", String.class);
        // If the type of the heuristic is RANDOM_PIVOTS (note the S!), the number of random pivots to take over total
        GridPathFinding.GridPathFindingPossibleParameters.put("random-pivots-count", String.class);
        GridPathFinding.GridPathFindingPossibleParameters.put("pivots-distances-db-file", String.class);
        GridPathFinding.GridPathFindingPossibleParameters.put("pivots-count", Integer.class);
    }

    // The start location of the agent
    private int startX = -1;
    private int startY = -1;
    private PairInt start = null;

    public enum COST_FUNCTION {
        HEAVY,
        UNIT
    }

    private boolean heavy = false;

    private long agentLocationBitMask;

    private GridMap map;
    private List<Integer> goals;
    private List<PairInt> goalsPairs;
    private List<PackedElement> goalsPacked;

    private enum HeuristicType {
        // Manhattan distance
        MD,
        // TDH with furthest k pivots
        DH_FURTHEST,
        // Take the average between DH and MD, also, take MD if DH is 0 (this is inconsistent heuristic!)
        DH_MD_AVERAGE_MD_IF_DH_IS_0,
        // Take random pivot among the available (this is inconsistent heuristic if pvitosCount > 0!)
        DH_RANDOM_PIVOT,
        // Take random number of pivots from the available
        DH_RANDOM_PIVOTS,
        // Take random value between DH and MD
        RANDOM_DH_MD
    }

    private HeuristicType heuristicType;
    private int randomPivotsCount;
    private int[] randomPivotsIndexes;


    // The number of pivots in case DH_FURTHEST is used
    private int pivotsCount;
    // Required for the TDH heuristic
    private int[] orderedPivots;

    private Map<Integer, Map<Integer, Double>> distancesFromPivots;

    private GridPathFindingOperator[] reverseOperators;

    // The cost of the optimal solution
    private double optimalSolutionCost;

    /**
     * A specific Move performed on the grid
     */
    private class Move {
        // An identification of the move
        private char id;
        // The x and y deltas between this move and the previous move
        private int dx;
        private int dy;
        // Delta in the GridMap internal data structure
        private int delta;

        /**
         * The constructor of the class
         *
         * @param map The grid on which the move is performed
         * @param id The identification of the move
         * @param dx The x delta of the move
         * @param dy The y delta of the move
         */
        private Move(GridMap map, char id, int dx, int dy) {
            this.id = id;
            this.dx = dx;
            this.dy = dy;
            this.delta = dx + map.mapWidth * dy;
        }
    }

    /**
     * This class represents a grid on which the agent is moving
     * The grid must be a rectangle (and can contain obstacles)
     */
    private class GridMap {
        private int mapWidth;
        private int mapHeight;
        // Size of the rectangle
        private int mapSize;

        // The internal data of the grid is represented as a character array
        private char map[];

        private Move possibleMoves[];
        private int possibleMovesCount;

        private int obstaclesCount;

        private int _countObstacles() {
            int toReturn = 0;
            for (int i = 0; i < this.mapSize; ++i) {
                if (GridPathFinding.OBSTACLE_MARKER == this.map[i]) {
                    ++toReturn;
                }
            }
            return toReturn;
        }

        /**
         * The constructor of the class - constructs a Map with a pre-defined width and height
         *
         * @param mapWidth The width of the map
         * @param mapHeight The height of the map
         */
        private GridMap(int mapWidth, int mapHeight, int obstaclesCount) {
            if (obstaclesCount == -1) {
                obstaclesCount = this._countObstacles();
            }
            // Obstacles count not given - so let's count it
            this.obstaclesCount = obstaclesCount;
            this.mapWidth = mapWidth;
            this.mapHeight = mapHeight;

            // The total size of the map
            this.mapSize = this.mapWidth * this.mapHeight;
            // The locations of the map : (mapWidth * mapHeight)
            this.map = new char[this.mapSize];
            // All the possible moves (currently, can't move diagonally)
            this.possibleMoves = new Move[4];
            this.possibleMovesCount = this.possibleMoves.length;
            // Initialize all the moves according to the real directions to perform
            this.possibleMoves[0] = new Move(this, 'S',  0,  1);
            this.possibleMoves[1] = new Move(this, 'N',  0, -1);
            this.possibleMoves[2] = new Move(this, 'W', -1,  0);
            this.possibleMoves[3] = new Move(this, 'E',  1,  0);
        }

        /**
         * A constructor of the class that also counts obstacles
         *
         * @param mapWidth The width of the map
         * @param mapHeight The height of the map
         */
        private GridMap(int mapWidth, int mapHeight) {
            this(mapWidth, mapHeight, -1);
        }

        /**
         * Make some location to tbe 'blocked': The agent can't be placed at this location
         *
         * @param location The location to block
         */
        private void setBlocked(int location) {
            this.map[location] = '#';
        }

        /**
         * Whether the queried location is blocked
         *
         * @param location The location to check
         *
         * @return True if the location is blocked and False otherwise
         */
        private boolean isBlocked(int location) {
            return this.map[location] == '#' ||
                    this.map[location] == 'T' ||
                    this.map[location] == GridPathFinding.OBSTACLE_MARKER;
        }

        /**
         * Calculate the index of the location in a one-dimensional array
         *
         * @param x The horizontal location
         * @param y The vertical location
         *
         * @return The calculated index
         */
        private int getLocationIndex(int x, int y) {
            return y * this.mapWidth + x;
        }

        /**
         * Calculate the index of the location in a one-dimensional array, given a pair of indexes
         *
         * @param location A pair whose first part represents the horizontal location and whose second part represents
         *                 the vertical location
         *
         * @return The calculated index
         */
        int getLocationIndex(PairInt location) {
            return this.getLocationIndex(location.first, location.second);
        }

        /**
         * Creates a Pair object with the dimensions of the given location
         *
         * @param location The required location
         * @return The calculated Pair
         */
        private PairInt getPosition(int location) {
            return new PairInt(location % this.mapWidth, location / this.mapWidth);
        }

        /**
         * @return The total number of obstacles on that Grid
         */
        public int getObstaclesCount() {
            return this.obstaclesCount;
        }

        /**
         * @return The probability of a single location to contain an obstacle
         */
        public double getObstaclesProbability() {
            return this.getObstaclesCount() / (double) (this.mapSize);
        }

        /**
         * @return The percentage of obstacles on the map
         */
        public int getObstaclesPercentage() {
            double prob = this.getObstaclesProbability();
            return (int) (prob * 100);
        }
    }

    /**
     * Returns the internal grid data for using in other contexts
     *
     * @return A character array which represents the grid
     */
    public char[] getGridMap() {
        return this.map.map;
    }

    /**
     * @return The width of the grid
     */
    public int getGridWidth() {
        return this.map.mapWidth;
    }

    /**
     * @return The height of the grid
     */
    public int getGridHeight() {
        return this.map.mapHeight;
    }

    /**
     * Initializes the reverse operators array: For each operator, set its reverse operator
     *
     * NOTE: This function is called only once (by the constructor)
     */
    private void _initializeReverseOperatorsArray() {
        // 8 is the maximum count of Vacuum Robot operators
        // (4 for regular moves and more 4 for diagonals)
        this.reverseOperators = new GridPathFindingOperator[8];
        int reversedMovesCount = 0;
        // Go over all the possible moves
        for (int i = 0; i < this.map.possibleMovesCount; i++) {
            // Go over all the possible moves
            for (int j = 0; j < this.map.possibleMovesCount; j++) {
                // In case the two operators are not reverse - ignore them
                if ((this.map.possibleMoves[i].dx != -this.map.possibleMoves[j].dx) ||
                        (this.map.possibleMoves[i].dy != -this.map.possibleMoves[j].dy)) {
                    continue;
                }
                // Define operator j to be reverse of operator i
                this.reverseOperators[i] = new GridPathFindingOperator(j);
                // Count the number of found 'reverse pairs'
                ++reversedMovesCount;
                break;
            }
        }
        // The number of reverse pairs must be equal to the total number of operators
        assert (reversedMovesCount == this.map.possibleMovesCount);
    }

    /**
     * Completes the initialization steps of the domain
     */
    private void _completeInit() {

        // MD is used by default
        this.heuristicType = HeuristicType.MD;
        // No need for this
        this.pivotsCount = -1;
        this.orderedPivots = null;
        this.distancesFromPivots = null;
        this.randomPivotsIndexes = null;

        // Compute bit masks for bit twiddling states in pack/unpack

        // The number of bits required in order to store all the locations of the grid map
        int locationBits = (int) Math.ceil(Utils.log2(this.map.mapSize));
        // The bit-mask required in order to access the locations bit-vector
        this.agentLocationBitMask = Utils.mask(locationBits);

        // Assure there is no overflow : at most 64 bits can be used in order to store the state
        if (locationBits > 64) {
            this.logger.fatal("Too many bits required: {}", locationBits);
            throw new IllegalArgumentException();
        }
        this.logger.info("Initializes reverse operators");
        // Initialize the array of reverse operators
        this._initializeReverseOperatorsArray();
        this.logger.info("Finished initializing reverse operators");

        // Create packed element for each goal
        this.goalsPacked = new ArrayList<>();
        for (int goal : this.goals) {
            long packed = goal & this.agentLocationBitMask;
            this.goalsPacked.add(new PackedElement(packed));
        }
        this._initializeMultipleGoalsEnvironment(this.totalGoalsCount());
    }

    /**
     * This constructor is used in order to generate a simple instance of the
     * domain - with a single agent and a single goal
     *
     * The constructor is used by some generators of instances, which want to
     * check that the generated instance is valid
     *
     * Note: Either start1Dim or start can be given, and also, either goal1Dim
     * or goal can be given
     *
     * @param width The width of the grid
     * @param height The height of the grid
     * @param map The grid itself (with obstacles filled)
     * @param start1Dim Start position (1-dimensional)
     * @param start The start position on the grid
     * @param goal1Dim Goal position (1-dimensional)
     * @param goal The SINGLE goal on the grid
     */
    private GridPathFinding(int width, int height, char[] map,
                            int start1Dim, PairInt start,
                            int goal1Dim, PairInt goal) {
        this.optimalSolutionCost = -1;
        // Either 1-dimensional or 2-dimensional input can be given for start and goal locations
        assert (((start1Dim == -1) ^ (start == null)) && ((goal1Dim == -1) ^ (start == null)));

        this.heavy = false;
        this.map = new GridMap(width, height);
        // Set the map explicitly
        this.map.map = map;
        if (start1Dim != -1) {
            start = this.map.getPosition(start1Dim);
        }

        this.start = start;
        this.startX = start.first;
        this.startY = start.second;

        this.goals = new ArrayList<>();
        this.goalsPairs = new ArrayList<>();

        if (goal1Dim != -1) {
            goal = this.map.getPosition(goal1Dim);
        } else {
            goal1Dim = this.map.getLocationIndex(goal);
        }
        this.goals.add(goal1Dim);
        this.goalsPairs.add(goal);

        // System.out.println("[INFO] Start: " + start.toString());
        // System.out.println("[INFO] Goal: " + goal.toString());
        // Now, complete the initialization by initializing other parameters
        this._completeInit();
    }

    /**
     * This constructor is used in order to generate a simple instance of the
     * domain - with a single agent and a single goal - start and goal
     * positions are given in 1-dimensional format
     *
     * The constructor is used by some generators of instances, which want to
     * check that the generated instance is valid
     *
     * @param width The width of the grid
     * @param height The height of the grid
     * @param map The grid itself (with obstacles filled)
     * @param start The start position on the grid (in a 1-dimensional format)
     * @param goal The SINGLE goal on the grid (in a 1-dimensional format)
     */
    public GridPathFinding(int width,
                           int height,
                           char[] map,
                           PairInt start,
                           PairInt goal) {
        this(width, height, map, -1, start, -1, goal);
    }

    /**
     * This constructor is used in order to generate a simple instance of the
     * domain - with a single agent and a single goal - start and goal
     * positions are given in 1-dimensional format
     *
     * The constructor is used by some generators of instances, which want to
     * check that the generated instance is valid
     *
     * @param width The width of the grid
     * @param height The height of the grid
     * @param map The grid itself (with obstacles filled)
     * @param start The start position on the grid (in a 1-dimensional format)
     * @param goal The SINGLE goal on the grid (in a 1-dimensional format)
     */
    public GridPathFinding(int width,
                           int height,
                           char[] map,
                           int start,
                           int goal) {
        this(width, height, map, start, null, goal, null);
    }

    /**
     * Reads and initializes map from the given the (pre-)initialized buffered reader
     *
     * @param width The width of the map
     * @param height The height of the map
     * @param in The reader to read from
     *
     * @throws IOException In case the read operation failed
     */
    private void _readMap(int width, int height, BufferedReader in) throws IOException {
        // Create the map
        this.map = new GridMap(width, height);
        // Now, read all the locations
        for (int y = 0; y < height; ++y) {
            String line = in.readLine();
            char[] chars = line.toCharArray();
            int ci = 0;
            // Horizontal
            for (int x = 0; x < width; ++x) {
                char c = chars[ci++];
                switch (c) {
                    // An obstacle
                    case GridPathFinding.OBSTACLE_MARKER:
                    case '#':
                    case 'T': {
                        this.map.setBlocked(this.map.getLocationIndex(x, y));
                        break;
                        // The start location
                    } case GridPathFinding.START_MARKER:
                    case 's':
                    case 'V': {
                        this.startX = x;
                        this.startY = y;
                        this.start = new PairInt(x, y);
                        break;
                        // The end location
                    } case 'g':
                    case GridPathFinding.GOAL_MARKER: {
                        this.goals.add(this.map.getLocationIndex(x, y));
                        this.goalsPairs.add(new PairInt(x, y));
                        break;
                        // Empty location
                    } case '.':
                    case '_':
                    case ' ': {
                        break;
                        // End of line
                    } case '\n': {
                        assert x == chars.length;
                        break;
                        // Something strange
                    } default: {
                        this.logger.fatal("Unknown character: {}", c);
                    }
                }
            }
        }
    }

    /**
     * Reads a value of some field from the given reader
     *
     * @param in The reader to read from
     * @param fieldName The name of the field to check
     *
     * @return The read value
     *
     * @throws IOException If something wrong occurred
     */
    private int _readSingleIntValueFromLine(BufferedReader in, String fieldName) throws IOException {
        String[] sz = in.readLine().trim().split(" ");
        if (fieldName != null) {
            assert sz.length == 2 && sz[0].equals(fieldName);
        }
        return Integer.parseInt(sz[1]);
    }

    /**
     * Reads a map of the moving AI format
     *
     * @param mapReader The reader from which the map should be read
     *
     * @throws IOException If something wrong occurred
     */
    private void _readMovingAIMap(BufferedReader mapReader) throws IOException {
        // First, read the first line (should be ignored)
        String sz[] = mapReader.readLine().trim().split(" ");
        assert sz.length == 2 && sz[0].equals("type");
        // Now, read the height of the map
        int height = this._readSingleIntValueFromLine(mapReader, "height");
        // Now, read the height of the map
        int width = this._readSingleIntValueFromLine(mapReader, "width");
        sz = mapReader.readLine().trim().split(" ");
        assert sz.length == 1 && sz[0].equals("map");
        // Now, read the map itself by calling the relevant function
        this._readMap(width, height, mapReader);
    }

    /**
     * The function reads start locations and goal locations from an initialized BufferedReader
     *
     * @param problemReader The reader to read the data from
     *
     * @throws IOException If something wrong occurred
     */
    private void _readStartAndGoalsFromProblemFile(BufferedReader problemReader) throws IOException {
        // Read start location
        String[] sz = problemReader.readLine().trim().split(" ");
        assert sz.length == 2 && sz[0].equals("start:");
        String start[] = sz[1].split(",");
        assert start.length == 2;
        this.startX = Integer.parseInt(start[0]);
        this.startY = Integer.parseInt(start[1]);
        this.start = new PairInt(startX, startY);
        // Read goal locations
        sz = problemReader.readLine().trim().split(" ");
        assert sz.length >= 2 && sz[0].equals("goals:");
        for (int i = 1; i < sz.length; ++i) {
            String goal[] = sz[i].split(",");
            Assert.assertTrue(goal.length == 2);
            int goalX = Integer.parseInt(goal[0]);
            int goalY = Integer.parseInt(goal[1]);
            this.goals.add(this.map.getLocationIndex(goalX, goalY));
            this.goalsPairs.add(new PairInt(goalX, goalY));
        }
    }

    /**
     * Reads a map file of the following format:
     *
     * <link to map (.map file)
     * [start <start location>]
     * [goal <goal location>]
     *
     * @param mapFilePath A path to a .map file
     * @param in A buffered reader for reading the rest of the file
     */
    private void _initMapFormat2(String mapFilePath, BufferedReader in) throws IOException {
        try {
            BufferedReader mapReader =
                    new BufferedReader(
                            new InputStreamReader(
                                    new FileInputStream(mapFilePath)));
            // Read the map
            this._readMovingAIMap(mapReader);
            this.logger.info("Map read from " + mapFilePath);
            // Read start and goal locations
            this._readStartAndGoalsFromProblemFile(in);
        } catch (FileNotFoundException e) {
            this.logger.error("Can't find reference map file: " + mapFilePath);
            throw new IOException();
        }
    }

    /**
     * The constructor of the general GridPathFinding domain
     *
     * @param stream The input stream for parsing the instance
     * @param costFunction The type of the cost function
     */
    public GridPathFinding(InputStream stream, COST_FUNCTION costFunction) {
        this.optimalSolutionCost = -1;
        // TODO:
        // this.heavy = (cost == COST.HEAVY);
        // Initialize the input-reader to allow parsing the state
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        try {
            this.goals = new ArrayList<>(1);
            this.goalsPairs = new ArrayList<>(1);
            // First, read the size of the grid
            String sz[] = in.readLine().trim().split(" ");
            assert sz.length == 2;
            if (sz[0].equals("map:")) {
                // Read the start and goal locations from the file
                this._initMapFormat2(sz[1], in);
            } else {
                int width = Integer.parseInt(sz[0]);
                int height = Integer.parseInt(sz[1]);
                // Read the map itself
                this._readMap(width, height, in);
            }
            // Assure there is a start location
            if (this.startX < 0 || this.startY < 0) {
                this.logger.fatal("No start location");
            }
        } catch(IOException e) {
            this.logger.fatal("Error reading input file");
            e.printStackTrace();
        }

        // Now, complete the initialization by initializing other parameters
        this._completeInit();
    }

    /**
     * A constructor of the class - start and end are given explicitly here
     * The map is assumed to be in the format of the Moving AI lab:
     *
     * type -type-
     * height -height-
     * width -width-
     * map
     * -map-data-
     *
     * @param stream The input stream for parsing the instance
     * @param start The start location
     * @param goal The goal location
     */
    public GridPathFinding(InputStream stream, int start, int goal) {
        this.optimalSolutionCost = -1;
        // TODO:
        // this.heavy = (cost == COST.HEAVY);
        // Initialize the input-reader to allow parsing the state
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        try {
            // Read the map (without start and goal locations)
            this._readMovingAIMap(in);
            // Read start
            this.start = this.map.getPosition(start);
            this.startX = this.start.first;
            this.startY = this.start.second;
            // Read goals
            this.goals = new ArrayList<>(1);
            this.goalsPairs = new ArrayList<>(1);
            PairInt goalPair = this.map.getPosition(goal);
            this.goals.add(goal);
            this.goalsPairs.add(goalPair);
            // Assure there is a start location
            if (this.startX < 0 || this.startY < 0) {
                Utils.fatal("No start location");
            }
        } catch(IOException e) {
            e.printStackTrace();
            this.logger.fatal("Error reading input file ");
        }

        // Now, complete the initialization by initializing other parameters
        this._completeInit();

    }

    /**
     * This constructor initializes a GridPathFinding problem by copying all
     * the parameters from other given problem and initializing start and goal
     * locations from the given input stream
     *
     * @param other The GridPathFinding problem to copy from
     * @param stream The stream to read start and goal locations from
     */
    public GridPathFinding(GridPathFinding other, InputStream stream) {
        this.optimalSolutionCost = -1;
        // TODO:
        // this.heavy = (cost == COST.HEAVY);
        // Initialize the input-reader to allow parsing the state
        BufferedReader in = new BufferedReader(new InputStreamReader(stream));
        // We need the map in order to read start and goals
        this.map = other.map;
        try {
            this.goals = new ArrayList<>(1);
            this.goalsPairs = new ArrayList<>(1);
            // First, read the size of the grid
            String sz[] = in.readLine().trim().split(" ");
            if (!sz[0].equals("map:")) {
                System.out.println("[ERROR] Copying GridPathFinding problem isn't allowed in this case");
                throw new IOException();
            }
            // Read start and goals locations
            this._readStartAndGoalsFromProblemFile(in);
            // Assure there is a start location
            if (this.startX < 0 || this.startY < 0) {
                Utils.fatal("No start location");
            }
        } catch(IOException e) {
            e.printStackTrace();
            Utils.fatal("Error reading input file");
        }

        this.heavy = other.heavy;
        this.agentLocationBitMask = other.agentLocationBitMask;
        this.reverseOperators = other.reverseOperators;

        this.heuristicType = other.heuristicType;
        this.pivotsCount = other.pivotsCount;
        this.orderedPivots = other.orderedPivots;
        this.distancesFromPivots = other.distancesFromPivots;
        this.randomPivotsCount = other.randomPivotsCount;
        this.randomPivotsIndexes = other.randomPivotsIndexes;
    }

    /**
     * The constructor of the general GridPathFinding domain (with the UNIT cost f
     * unction)
     *
     * @param stream The input stream for parsing the instance
     */
    public GridPathFinding(InputStream stream) {
        this(stream, COST_FUNCTION.UNIT);
    }

    /**
     * The function computes the differential heuristics value for a single pivot case
     *
     * @param startLocation The start location on the grid
     * @param pivotIndex The index of the pivot to take from the pivots map
     * @param goalLocation The end location on the grid
     * @param failIf0 Whether to return -1 if one of the pivots is unreachable from start or from goal or the distance
     *                from one of the pivots is 0
     *
     * @return The computed heuristic value or -1 in case one of the distances is unreachable or 0 and failIf0 is true
     */
    private double _computeDHForSinglePivot(int startLocation, int pivotIndex, int goalLocation, boolean failIf0) {
        int currentPivot = this.orderedPivots[pivotIndex];
        double distanceFromAgentToPivot = this.distancesFromPivots.get(currentPivot).get(startLocation);
        if ((failIf0 && distanceFromAgentToPivot == 0) || distanceFromAgentToPivot < 0) {
            return -1;
        }
        double distanceFromPivotToGoal = this.distancesFromPivots.get(currentPivot).get(goalLocation);
        if ((failIf0 && distanceFromPivotToGoal == 0) || distanceFromPivotToGoal < 0) {
            return -1;
        }
        return Math.abs(distanceFromAgentToPivot - distanceFromPivotToGoal);
    }

    /*
    private int getRandomPivotIndex(int index, GridPathFindingState state) {
        long longsSum = this.pack(state).getLongsSum();
        System.out.println(PRIMARIES_FOR_RANDOMS[index] + " " + longsSum);
        return (int)(Math.pow(PRIMARIES_FOR_RANDOMS[index], longsSum)) % this.pivotsCount;
    }
    */

    @Override
    protected int getNthValidGoalIndex(int validGoalIndex) {
        try {
            return super.getNthValidGoalIndex(validGoalIndex);
        } catch (NullPointerException e) {
            this.logger.warn("Valid goals not initialized - taking the first goal for heuristic");
            return 0;
        }
    }

    private PairInt getFirstValidGoalPair() {
        if (this.startStateIsGoal()) {
            return this.start;
        }
        return this.goalsPairs.get(this.getNthValidGoalIndex(0));
    }

    /**
     * Compute the heuristic value of a given state
     *
     * @param s The state whose heuristic value should be computed
     * @return The computed value
     */
    private double[] computeHD(GridPathFindingState s) {
        assert this.validGoalsCount() == 1;
        // TODO: PROBLEMATICCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCCC
        int currentGoal = this.goals.get(this.getNthValidGoalIndex(0));
        // Compute also the Manhattan Distance
        int md = Utils.calcManhattanDistance(
                this.map.getPosition(s.agentLocation),
                // TODO: Deals with a single goal only!
                this.getFirstValidGoalPair());

        double maxDistance;

        switch (this.heuristicType) {
            // A simple Manhattan distance
            case MD: {
                return new double[]{md, md};
            }
            // A simple DH heuristic, but, choose max from DH and MD
            case DH_FURTHEST: {
                maxDistance = 0.0d;
                for (int i = 0; i < this.pivotsCount; ++i) {
                    // Compute the heuristic value for this pivot, don't return -1 if the distance from one of the
                    // pivots is 0
                    double diff = this._computeDHForSinglePivot(
                            s.agentLocation,
                            this.orderedPivots[i],
                            currentGoal,
                            false);
                    if (diff > maxDistance) {
                        maxDistance = diff;
                    }
                }
                // Take the maximum value (chose from MD and DH)
                double maxValue = Math.max(maxDistance, md);
                return new double[]{maxValue, maxValue};
            }
            // Take the average between DH (no max with MD) and MD; if DH == 0 => Take only MD
            case DH_MD_AVERAGE_MD_IF_DH_IS_0: {
                maxDistance = 0.0d;
                for (int i = 0; i < this.pivotsCount; ++i) {
                    // Compute the heuristic value for this pivot, return -1 if the distance from one of the
                    // pivots is 0
                    double diff = this._computeDHForSinglePivot(
                            s.agentLocation,
                            i,
                            currentGoal,
                            true);
                    if (diff > maxDistance) {
                        maxDistance = diff;
                    }
                }
                // If DH is greater than 0 => return the average, otherwise, return only MD
                if (maxDistance > 0) {
                    double val = (md + maxDistance) / 2;
                    return new double[]{val, val};
                } else {
                    return new double[]{md, md};
                }
            }
            case DH_RANDOM_PIVOT: {
                // The pivot index is calculated using the packed value (takes the first long only ...)
                double diff = this._computeDHForSinglePivot(
                        s.agentLocation,
                        (int)(this.pack(s).getLongsSum() % this.pivotsCount),
                        currentGoal,
                        false);
                // Take the maximum value (chose from MD and DH)
                double maxValue = Math.max(diff, md);
                return new double[]{maxValue, maxValue};
            }
            case DH_RANDOM_PIVOTS: {
                maxDistance = 0.0d;
                Set<Integer> usedIndexes = new HashSet<>();
                int currentIndex = (int)(this.pack(s).getLongsSum() % GridPathFinding.RANDOM_PIVOTS_INDEXES_COUNT);
                for (int i = 0; i < this.randomPivotsCount; ++i) {
                    int value = this.randomPivotsIndexes[currentIndex];
                    while (usedIndexes.contains(value)) {
                        currentIndex = (currentIndex + 1) % GridPathFinding.RANDOM_PIVOTS_INDEXES_COUNT;
                        value = this.randomPivotsIndexes[currentIndex];
                    }
                    usedIndexes.add(value);
                    // Compute the heuristic value for this pivot, don't return -1 if the distance from one of the
                    // pivots is 0
                    double diff = this._computeDHForSinglePivot(
                            s.agentLocation,
                            value,
                            currentGoal,
                            false);
                    if (diff > maxDistance) {
                        maxDistance = diff;
                    }
                }
                // Take the maximum value (chose from MD and DH)
                double maxValue = Math.max(maxDistance, md);
                return new double[]{maxValue, maxValue};
            }
            case RANDOM_DH_MD: {
                if (this.pack(s).getLongsSum() % 2 == 0) {
                    return new double[]{md, md};
                }
                // The pivot index is calculated using the packed value (takes the first long only ...)
                double diff = this._computeDHForSinglePivot(
                        s.agentLocation,
                        (int)(this.pack(s).getLongsSum() % this.pivotsCount),
                        currentGoal,
                        false);
                return new double[] {diff, diff};
            }
        }
        return new double[]{0, 0};
    }

    @Override
    public boolean isCurrentHeuristicConsistent() {
        return (this.heuristicType != HeuristicType.DH_MD_AVERAGE_MD_IF_DH_IS_0) &&
                // This heuristic is consistent only if the number of pivots is 1
                (this.heuristicType != HeuristicType.DH_RANDOM_PIVOT || this.pivotsCount == 1) &&
                (this.heuristicType != HeuristicType.RANDOM_DH_MD);
    }

    @Override
    public void setOptimalSolutionCost(double cost) {
        this.optimalSolutionCost = cost;
    }

    @Override
    public double getOptimalSolutionCost() {
        return this.optimalSolutionCost;
    }

    @Override
    public int maxGeneratedSize() {
    	return Integer.MAX_VALUE;
//        throw new UnsupportedOperationException();
    }

    /**
     * A GridPathFinding State
     */
    public final class GridPathFindingState extends SearchState {
        private double h;
        private double d;

        //private double hHat;
        //private double dHat;
        //private double sseD;
        //private double sseH;

        // The location of the agent
        public int agentLocation;
        // The depth of the search
        private int depth;

        // All the possible operators
        private GridPathFindingOperator[] ops;

        private GridPathFindingState parent;

        /**
         * A default constructor of the class
         */
        private GridPathFindingState() {
            this.h = this.d = -1;
            this.depth = -1;
            this.agentLocation = -1;
            this.ops = null;
            this.parent = null;
        }

        /**
         * A copy constructor
         *
         * @param state The state to copy
         */
        private GridPathFindingState(GridPathFindingState state) {
            this.h = state.h;
            this.d = state.d;
            this.depth = state.depth;
            // Copy the location of the robot
            this.agentLocation = state.agentLocation;
            // Copy the parent state
            this.parent = state.parent;
        }

        public void resetStateMetaData() {
            this.h = 0;
            this.d = 0;
            this.depth = 0;
            this.parent = null;
        }

        public SearchState copy() {
            return new GridPathFindingState(this);
        }

        @Override
        public boolean equals(Object obj) {
            try {
                GridPathFindingState o = (GridPathFindingState)obj;
                // Assure the location of the agent is the same
                return this.agentLocation == o.agentLocation;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public SearchState getParent() {
            return this.parent;
        }

        /**
         * An auxiliary function for calculating the h and d values of the current state
         */
        private void computeHD() {
            double[] p = GridPathFinding.this.computeHD(this);
            this.h = p[0];
            this.d = p[1];
        }

        @Override
        public double getH() {
            if (this.h < 0) {
                this.computeHD();
            }
            return this.h;
        }

        @Override
        public double setH(double hValue) {
            //System.out.println("SETTTTTTTTTTTTTTTTTTTTTTTTTING");
            double toReturn = this.getH();
            this.h = hValue;
            return toReturn;
        }

        @Override
        public double getD() {
            if (this.d < 0) {
                this.computeHD();
            }
            return this.d;
        }

        @Override
        public String dumpState() {
            return GridPathFinding.this.dumpState(this);
        }

        @Override
        public String dumpStateShort() {
            return GridPathFinding.this.map.getPosition(this.agentLocation).toString();
        }

    }

    /**
     * The function performs a dump of the grid map used in the search and puts the positions of the agent on each
     * of the given state on the map
     *
     * @param states The states array (can be null)
     * @param obstaclesCountArray The obstacles counter (an OUTPUT parameter)
     *
     * @return A string representation of the map (with all agents located on it)
     */
    private String _dumpMap(SearchState states[], int[] obstaclesCountArray) {
        StringBuilder sb = new StringBuilder();
        int obstaclesCount = 0;
        // Now, dump the Map with the location of the agent and the goals
        for (int y = 0; y < this.map.mapHeight; ++y, sb.append('\n')) {
            // Horizontal
            for (int x = 0; x < this.map.mapWidth; ++x) {
                // Get the index of the current location
                int locationIndex = this.map.getLocationIndex(x, y);
                // Check if the location contains an obstacle
                if (this.map.isBlocked(locationIndex)) {
                    sb.append(GridPathFinding.OBSTACLE_MARKER);
                    ++obstaclesCount;
                } else {
                    boolean agentLocation = false;
                    if (states != null) {
                        // Check if the agent is at this location
                        for (int k = 0; k < states.length; ++k) {
                            if (((GridPathFindingState)states[k]).agentLocation == locationIndex) {
                                if (k == 0) {
                                    sb.append(GridPathFinding.START_MARKER);
                                } else if (k == states.length - 1) {
                                    sb.append(GridPathFinding.GOAL_MARKER);
                                } else {
                                    sb.append('X');
                                }
                                agentLocation = true;
                                break;
                            }
                        }
                    }
                    if (!agentLocation) {
                        sb.append('.');
                    }
                }
            }
        }
        // Set the output parameter
        if (obstaclesCountArray != null) {
            obstaclesCountArray[0] = obstaclesCount;
        }
        return sb.toString();
    }


    /*
    public String dumpStatesPath(SearchState[] states) {
        StringBuilder sb = new StringBuilder();

        GridPathFindingState lastState = (GridPathFindingState) states[states.length - 1];

        if (states != null) {
            lastState.

        }
    }*/


    @Override
    public String dumpStatesCollection(SearchState[] states) {
        StringBuilder sb = new StringBuilder();
        sb.append("********************************\n");
        sb.append('\n');

        GridPathFindingState lastState = null;

        if (states != null) {
            // All the data regarding a single state refers to the last state of the collection
            lastState = (GridPathFindingState) states[states.length - 1];
            // h
            sb.append("h: ");
            sb.append(lastState.getH());
            sb.append("\n");
            // d
            sb.append("d: ");
            sb.append(lastState.getD());
            sb.append("\n");
        }

        // Output parameter of the _dumpMap function
        int[] obstaclesCountArray = new int[1];
        sb.append(this._dumpMap(states, obstaclesCountArray));
        // Additional newline
        sb.append('\n');

        if (states != null) {
            PairInt agentLocation = this.map.getPosition(lastState.agentLocation);
            sb.append("Agent location: ");
            sb.append(agentLocation.toString());
            sb.append("\n");
            sb.append("Goals:");
            for (PairInt goal : this.goalsPairs) {
                sb.append(" ");
                sb.append(goal.toString());
            }
            sb.append("\n");
        }

        sb.append("obstacles count: ");
        sb.append(obstaclesCountArray[0]);

        sb.append("\n");
        sb.append("********************************\n\n");
        return sb.toString();
    }

    /**
     * The function reads the distances from all pivots of the TDH heuristic from the given pivots PDB file
     *
     * The file is assumed to be of the following formats:
     *
     *       <pivots-count>
     *       <pivot-1>
     *       <pivot-2>
     *       ...
     *       <pivot-n>
     *       <all-distances-from-pivot-1>
     *       <all-distances-from-pivot-2>
     *       ...
     *       <all-distances-from-pivot-n>
     *
     * @param pivotsPDBFile The input file which contains the pivots
     *
     * @return The created map of pivots : pivot => map : location => distance
     *
     * @throws IOException In something wrong occurred
     */
    private Pair<int[], Map<Integer, Map<Integer, Double>>> _readPivotsDB(
            String pivotsPDBFile) throws IOException {
        System.out.println("[INFO] Reading pivots DB from " + pivotsPDBFile);
        DataInputStream inputStream = new DataInputStream(new FileInputStream(pivotsPDBFile));
        // First, read count of pivots
        int pivotsCount = inputStream.readInt();
        // Next read the pivots
        int[] pivots = new int[pivotsCount];
        for (int i = 0; i < pivotsCount; ++i) {
            pivots[i] = inputStream.readInt();
        }
        Map<Integer, Map<Integer, Double>> distancesMap = new HashMap<>();
        // Finally, read the distances
        for (int pivot : pivots) {
            Map<Integer, Double> currentDistancesMap = new HashMap<>();
            for (int i = 0; i < this.map.mapSize; ++i) {
                currentDistancesMap.put(i, inputStream.readDouble());
            }
            distancesMap.put(pivot, currentDistancesMap);
        }
        System.out.println("[INFO] Finished reading pivots DB from " + pivotsPDBFile);
        return new Pair<>(pivots, distancesMap);
    }

    @Override
    public Map<String, Class> getPossibleParameters() {
        return GridPathFinding.GridPathFindingPossibleParameters;
    }

    @Override
    public void setAdditionalParameter(String parameterName, String value) {
        switch (parameterName) {
            case "heuristic": {
                switch (value) {
                    case "tdh-furthest": {
                        this.heuristicType = HeuristicType.DH_FURTHEST;
                        break;
                    }
                    case "dh-md-average-md-if-dh-is-0":
                        System.out.println("[WARNING] This heuristic is inconsistent");
                        this.heuristicType = HeuristicType.DH_MD_AVERAGE_MD_IF_DH_IS_0;
                        break;
                    case "dh-random-pivot":
                        System.out.println("[WARNING] This heuristic is inconsistent if pivots-count > 1");
                        this.heuristicType = HeuristicType.DH_RANDOM_PIVOT;
                        break;
                    case "dh-random-pivots":
                        System.out.println("[WARNING] This heuristic is inconsistent if pivots-count != random-pivots-count");
                        this.heuristicType = HeuristicType.DH_RANDOM_PIVOTS;
                        break;
                    case "random-dh-md": {
                        System.out.println("[WARNING] This heuristic is inconsistent");
                        this.heuristicType = HeuristicType.RANDOM_DH_MD;
                        break;
                    }
                    case "md": {
                        this.heuristicType = HeuristicType.MD;
                        break;
                    }
                    default: {
                        System.err.println("Illegal heuristic type for GridPathfinding domain: " + value);
                        throw new IllegalArgumentException();
                    }
                }
                break;
            }
            case "random-pivots-count": {
                if (this.heuristicType != HeuristicType.DH_RANDOM_PIVOTS) {
                    System.out.println("[ERROR] Heuristic type isn't DH_RANDOM_PIVOTS - can't set random pivots count");
                    throw new IllegalArgumentException();
                }
                this.randomPivotsCount = Integer.parseInt(value);
                // Fail if too high number of random pivots (if pivots-count was set)
                if (this.randomPivotsCount > this.pivotsCount && this.pivotsCount > -1) {
                    System.out.println("[ERROR] Too high number of random pivots (must be at most " +
                            this.pivotsCount + ")");
                    throw new IllegalArgumentException();
                }
                if (this.randomPivotsCount < 1) {
                    System.out.println("[ERROR] Illegal random pivots count (must be at least 1)");
                    throw new IllegalArgumentException();
                }
                if (this.pivotsCount > 0) {
                    this.randomPivotsIndexes = Utils.getRandomIntegerListArray(
                            GridPathFinding.RANDOM_PIVOTS_INDEXES_COUNT,
                            this.pivotsCount,
                            null);
                }
                break;
            }
            case "pivots-distances-db-file": {
                try {
                    Pair<int[], Map<Integer, Map<Integer, Double>>> readData = this._readPivotsDB(value);
                    this.orderedPivots = readData.getKey();
                    this.distancesFromPivots = readData.getValue();
                    // Debug:
                    //for (int p : this.orderedPivots) {
                    //    String formattedP = String.format("%7d", p);
                    //    System.out.println("Pivot: " + formattedP + " - " + this.map.getPosition(p));
                    //}
                } catch (IOException e) {
                    System.out.println("[ERROR] Reading pivots failed" +
                            (e.getMessage() != null ? " : " + e.getMessage() : ""));
                    throw new IllegalArgumentException();
                }
                break;
            } case "pivots-count": {
                if ((this.heuristicType != HeuristicType.DH_FURTHEST) &&
                        (this.heuristicType != HeuristicType.DH_MD_AVERAGE_MD_IF_DH_IS_0) &&
                        (this.heuristicType != HeuristicType.DH_RANDOM_PIVOT) &&
                        (this.heuristicType != HeuristicType.DH_RANDOM_PIVOTS) &&
                        (this.heuristicType != HeuristicType.RANDOM_DH_MD)) {
                    System.out.println("[ERROR] Heuristic type isn't DH - can't set pivots count");
                    throw new IllegalArgumentException();
                } else if (this.orderedPivots == null) {
                    System.out.println("[ERROR] Please specify pivots file");
                    throw new IllegalArgumentException();
                } else {
                    int pivotsCount = Integer.parseInt(value);
                    if (pivotsCount > this.orderedPivots.length) {
                        System.out.println("[ERROR] Insufficient pivots number (currently " +
                                this.orderedPivots.length + " but required " + pivotsCount + ")");
                        throw new IllegalArgumentException();
                    }
                    if (this.heuristicType == HeuristicType.DH_RANDOM_PIVOTS && pivotsCount < this.randomPivotsCount) {
                        System.out.println("[ERROR] Random pivots count for DH_RANDOM_PIVOTS heuristic was set for " +
                            this.randomPivotsCount + ", thus, at least " + this.randomPivotsCount + " must be set");
                        throw new IllegalArgumentException();
                    }
                    this.pivotsCount = pivotsCount;
                    if (this.heuristicType == HeuristicType.DH_RANDOM_PIVOTS && this.randomPivotsIndexes == null) {
                        this.randomPivotsIndexes = Utils.getRandomIntegerListArray(
                                GridPathFinding.RANDOM_PIVOTS_INDEXES_COUNT,
                                this.pivotsCount,
                                null);
                    }
                }
                break;
            } default: {
                System.out.println("No such parameter: " + parameterName + " (value: " + value + ")");
                throw new IllegalArgumentException();
            }
        }
    }

    /**
     * An auxiliary function for dumping a single state of the GridPathFinding domain instance
     *
     * @param state The state to dump
     *
     * @return A string representation of the state
     */
    private String dumpState(GridPathFindingState state) {
        return this.dumpStatesCollection(new GridPathFindingState[]{state});
    }

    /**
     * Checks whether the given move is valid for the given location
     *
     * @param location The location on the map, on which the move is applied
     * @param move The move to apply
     *
     * @return True if the move is valid and false otherwise
     */
    private boolean _isValidMove(int location, Move move) {
        // Add the delta of the move and get the next location
        int next = location + move.delta;

        // (next = y * this.mapWidth + x)

        // Assure the move doesn't cause the state to exceed the grid and also that the move
        // doesn't cause the state to reach a blocked location

        // Moving West/East && y changed => invalid!
        if (move.dx != 0 &&
                (next / this.map.mapWidth != location / this.map.mapWidth)) {
            return false;
            // Moving South/North && x changed => invalid
        } else if (move.dy != 0 &&
                (next % this.map.mapWidth != location % this.map.mapWidth)) {
            return false;
        }

        return (next > 0 && next < this.map.mapSize && !this.map.isBlocked(next));
    }

    /**
     * Checks settings of the domain and returns true if all is Ok and false otherwise
     *
     * @return Whether the domain settings are Ok
     */
    private boolean _checkSettings() {
        return this.heuristicType != HeuristicType.DH_FURTHEST || this.pivotsCount >= 1;
    }

    @Override
    protected SearchState createInitialState() {
        assert this.startX != -1 && this.startY != -1;
        // TODO: Redundant here!
        // Assert settings are ok
        assert this._checkSettings();
        GridPathFindingState state = new GridPathFindingState();
        state.agentLocation = this.map.getLocationIndex(this.startX, this.startY);
        // Compute the initial mapHeight and d values and fill the state with that values
        double hd[] = this.computeHD(state);
        state.h = hd[0];
        state.d = hd[1];
        state.depth = 0;
        state.parent = null;
        // System.out.println(this.dumpState(state));
        // Return the created state
        return state;
    }

    @Override
    public boolean stateIsOneOfValidGoals(SearchState s) {
        GridPathFindingState grs = (GridPathFindingState)s;
        int goalIndex = this.goals.indexOf(grs.agentLocation);
        return goalIndex > -1 && this.isValidGoal(goalIndex);
    }


    @Override
    public int totalGoalsCount() {
        return this.goals.size();
    }

    @Override
    public boolean goalsAreExplicit() {
        return true;
    }

    @Override
    public PackedElement getNthGoalFromAllGoals(int goalIndex) {
        return this.goalsPacked.get(goalIndex);
    }

    @Override
    public List<PackedElement> getAllGoalsInternal() {
        return this.goalsPacked;
    }

    /**
     * Init the possible operators for the given state
     *
     * @param state The state whose operators should be initialized
     */
    private void _initOps(GridPathFindingState state) {
        // An empty vector of operators
        Vector<GridPathFindingOperator> possibleOperators = new Vector<>();
        // Go over all the possible moves
        for (int i = 0; i < GridPathFinding.NUM_MOVES; ++i) {
            if (this._isValidMove(state.agentLocation, this.map.possibleMoves[i])) {
                possibleOperators.add(new GridPathFindingOperator(i));
            }
        }
        // Finally, create the possible operators array
        state.ops = possibleOperators.toArray(new GridPathFindingOperator[possibleOperators.size()]);
    }

    @Override
    public int getNumOperators(SearchState state) {
        GridPathFindingState grs = (GridPathFindingState) state;
        if (grs.ops == null) {
            this._initOps(grs);
        }
        return grs.ops.length;
    }

    @Override
    public Operator getOperator(SearchState state, int index) {
        GridPathFindingState grs = (GridPathFindingState)state;
        if (grs.ops == null) {
            this._initOps(grs);
        }
        return grs.ops[index];
    }

    @Override
    public SearchState copy(SearchState state) {
        return new GridPathFindingState((GridPathFindingState)state);
    }

    /**
     * Packs a state into a long number
     *
     * The packed state is a 64 bit (long) number which stores (currently) the
     * location of the agent
     */
    @Override
    public PackedElement pack(SearchState s) {
        GridPathFindingState state = (GridPathFindingState)s;
        long packed = 0L;
        // pack the location of the agent
        packed |= state.agentLocation & this.agentLocationBitMask;
        return new PackedElement(packed);
    }

    /**
     * An auxiliary function for unpacking Vacuum Robot state from a long
     * number. This function performs the actual unpacking
     *
     * @param packed The packed state
     * @param dst The destination state which is filled from the unpacked value
     */
    private void _unpackLite(long packed, GridPathFindingState dst) {
        dst.ops = null;
        // Finally, unpack the location of the robot
        dst.agentLocation = (int) (packed & this.agentLocationBitMask);
    }

    /**
     * An auxiliary function for unpacking Vacuum Robot state from a long number
     *
     * @param packed The packed state
     * @param dst The destination state which is filled from the unpacked value
     */
    private void unpack(long packed, GridPathFindingState dst) {
        this._unpackLite(packed, dst);
        // Compute the heuristic values
        double hd[] = this.computeHD(dst);
        dst.h = hd[0];
        dst.d = hd[1];
        dst.depth = 0;
    }
    /**
     * Unpacks a GridPathFinding state from a long number
     */
    @Override
    public GridPathFindingState unpack(PackedElement packed) {
        assert packed.getLongsCount() == 1;
        GridPathFindingState dst = new GridPathFindingState();
        this.unpack(packed.getFirst(), dst);
        return dst;
    }

    /**
     * Apply the given operator on the given state and generate a new state
     *
     * @param state The state to apply the operator on
     * @param op The operator to apply the state on
     *
     * @return The new generated state
     */
    @Override
    public SearchState applyOperator(SearchState state, Operator op) {
        GridPathFindingState s = (GridPathFindingState)state;
        GridPathFindingState grs = (GridPathFindingState)copy(s);
        GridPathFindingOperator o = (GridPathFindingOperator)op;

        grs.ops = null; // reset operators

        // Assure the type of the operator is actually a move
        if (o.type < 0 || o.type > 3) {
            System.err.println("Unknown operator type " + o.type);
            System.exit(1);
        }
        // Update the location of the robot
        grs.agentLocation += this.map.possibleMoves[o.type].delta;

        grs.depth++;

        double p[] = this.computeHD(grs);
        grs.h = p[0];
        grs.d = p[1];
        grs.parent = s;

        //dumpState(s);
        //dumpState(vrs);
        return grs;
    }

    private final class GridPathFindingOperator implements Operator {
        // UP = 0, DOWN = 1, LEFT = 2, RIGHT = 3
        public static final int NOP = -1;
        // Initially, the type of the operator is NOP
        private int type = GridPathFindingOperator.NOP;

        /**
         * The constructor of the class: initializes an operator with the given type
         *
         * @param type The type of the operator
         */
        private GridPathFindingOperator(int type) {
            this.type = type;
        }

        @Override
        public boolean equals(Object obj) {
            try {
                if (obj == null) {
                    return false;
                }
                GridPathFindingOperator o = (GridPathFindingOperator) obj;
                return type == o.type;
            } catch (ClassCastException e) {
                return false;
            }
        }

        @Override
        public double getCost(SearchState s, SearchState parent) {
            GridPathFindingState grs = (GridPathFindingState) s;
            double cost = 1.0d;
            // TODO: Heavy???
            return cost;
        }

        /**
         * Finds the reverse operator that applying it will reverse the state caused by this
         * operator
         */
        @Override
        public Operator reverse(SearchState state) {
            return GridPathFinding.this.reverseOperators[this.type];
        }
    }
}
