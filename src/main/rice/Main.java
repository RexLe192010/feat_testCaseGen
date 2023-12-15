package main.rice;

import main.rice.basegen.BaseSetGenerator;
import main.rice.concisegen.ConciseSetGenerator;
import main.rice.parse.ConfigFile;
import main.rice.parse.ConfigFileParser;
import main.rice.parse.InvalidConfigException;
import main.rice.test.TestCase;
import main.rice.test.Tester;

import java.io.IOException;
import java.util.Set;

public class Main {
    /**
     * main() delegates to generateTests() in order to compute the concise test set. It prints the result of calling
     * generateTests() to the console, along with an appropriate message explaining what's being printed.
     * @param args a String array containing three pieces of information: A String containing the path to the config
     *             file; A String containing the path to the reference solution; A String containing the path to the
     *             directory containing the buggy implementations.
     * @throws IOException if a file does not exist or could not be read
     * @throws InvalidConfigException if there is something run with the config file format
     * @throws InterruptedException if the function is interrupted unexpectedly
     */
    public static void main(String[] args) throws IOException, InvalidConfigException, InterruptedException {
        // Delegates to generateTests() in order to compute the concise test set
        Set<TestCase> testCases = generateTests(args);

        // Print out each test case in the concise test set
        System.out.println("The concise set of test cases for this function is: \n");
        int i = 0;
        for (TestCase testCase : testCases) {
            System.out.println("Test Case" + i + ":\n");
            System.out.println(testCase.toString());
            System.out.println("\n");
            i++;
        }
    }

    /**
     * This method is a helper for main() and takes the exact same array of arguments. It utilizes the components
     * that we built in homeworks 1-6 in order to perform end-to-end test case generation, returning the concise test
     * set.
     * @param args a String array containing three pieces of information: A String containing the path to the config
     *             file; A String containing the path to the reference solution; A String containing the path to the
     *             directory containing the buggy implementations.
     * @return the concise set of testcases for the function under test
     * @throws IOException if a file does not exist or could not be read
     * @throws InvalidConfigException if there is something run with the config file format
     * @throws InterruptedException if the function is interrupted unexpectedly
     */
    public static Set<TestCase> generateTests(String[] args)
            throws IOException, InvalidConfigException, InterruptedException{
        // Initialize the paths of the config file, the reference solution, and the directory of buggy implementations.
        String configPath = args[0];
        String solPath = args[1];
        String buggyPath = args[2];

        // Use the configFileParser to find the name of the function under test, a List of PyNodes that will
        // be used to generate TestCases for the function under test, and the number of random test cases to be
        // generated, encapsulated in a ConfigFile.
        ConfigFile configFile = ConfigFileParser.parse(ConfigFileParser.readFile(configPath));

        // Generate the baseSetGenerator using the info from the configFile.
        BaseSetGenerator baseGen = new BaseSetGenerator(configFile.getNodes(), configFile.getNumRand());

        // Generate the base test set and then the concise test set based upon above.
        Tester tester = new Tester(configFile.getFuncName(), solPath, buggyPath, baseGen.genBaseSet());
        tester.computeExpectedResults();
        return ConciseSetGenerator.setCover(tester.runTests());
    }
}