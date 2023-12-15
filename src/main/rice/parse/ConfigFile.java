package main.rice.parse;

import main.rice.node.APyNode;

import java.util.List;

public class ConfigFile {
    /**
     * A string representing the name of the function under test.
     */
    private String funcName;

    /**
     * A List of PyNodes that will be used to generate TestCases for the function under test.
     */
    private List<APyNode<?>> nodes;

    /**
     * An integer representing the number of random test cases to be generated.
     */
    private int numRand;

    /**
     * The constructor for a ConfigFile object.
     * @param funcName, a string representing the name of the function under test.
     * @param nodes, a list of PyNodes to generate testcases.
     * @param numRand, an integer of random test cases to be generated.
     */
    public ConfigFile(String funcName, List<APyNode<?>> nodes, int numRand) {
        this.funcName = funcName;
        this.nodes = nodes;
        this.numRand = numRand;
    }

    /**
     * Getter method for funcName field.
     * @return a String representing the name of current function being tested.
     */
    public String getFuncName() {
        return this.funcName;
    }

    /**
     * Getter method for nodes field.
     * @return a list of PyNodes with which test cases are generated.
     */
    public List<APyNode<?>> getNodes() {
        return this.nodes;
    }

    /**
     * Getter method for numRand field.
     * @return an integer representing the number pf random test cases to be generated.
     */
    public int getNumRand() {
        return this.numRand;
    }
}