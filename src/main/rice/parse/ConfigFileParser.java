package main.rice.parse;

import main.rice.node.*;
import main.rice.obj.APyObj;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class ConfigFileParser {
    /**
     * Reads and returns the contents of the file located at the input filepath;
     * throws an IOException if the file does not exist or cannot be read.
     * @param filepath, a string representing the file path.
     * @return a string representing the content of the file read in.
     * @throws IOException when the file doesn't exist or cannot be read.
     */
    public static String readFile(String filepath) throws IOException {
        StringBuilder content = new StringBuilder();
        BufferedReader br = new BufferedReader(new FileReader(filepath));
        String line;
        while ((line = br.readLine()) != null) {
            content.append(line).append("\n");
        }
        return content.toString();
    }

    /**
     * Parses the input string, which should be the contents of a JSON file
     * formatted according to the config file specifications.
     * This should build an APyNode tree for each parameter,
     * where each node's type, exhaustive domain, and random domain
     * should be set up to reflect the contents of the config file.
     * @param contents, a string representing the content of the config file.
     * @return a ConfigFile object that contains the information given by the file.
     * @throws InvalidConfigException when there are invalid parameters inside the file.
     */
    public static ConfigFile parse(String contents) throws InvalidConfigException {
        // Call createMap helper function to create a map from JSON key to value.
        Map<String, String> contentMap = createMap(contents);
        // Check if there's missing key in the config file, if yes, throw an exception.
        missingKeyCheck(contentMap);

        // Get corresponding values from contentMap.
        String funcName = contentMap.get("fname");
        funcName = funcName.substring(1, funcName.length() - 1); // Get rid of quotation marks.
        int numRand = Integer.parseInt(contentMap.get("num random"));
        List<APyNode<?>> nodes = new ArrayList<>();
        String type = contentMap.get("types");
        String exDoms = contentMap.get("exhaustive domain");
        String ranDoms = contentMap.get("random domain");

        type = type.substring(2, type.length() - 2); // Get rid of [" in the front and "] in the end.
        exDoms = exDoms.substring(2, exDoms.length() - 2);
        ranDoms = ranDoms.substring(2, ranDoms.length() - 2);
        String[] types = type.split("\", \""); // Split by ", " as written in the config file.
        String[] exDomain = exDoms.split("\", \"");
        String[] ranDomain = ranDoms.split("\", \"");

        // Check the length of each array of values. Decrease later work load.
        int typeLen = types.length;
        int exLen = exDomain.length;
        int ranLen = ranDomain.length;
        if (typeLen != ranLen || exLen != ranLen) {
            throw new InvalidConfigException("Length for domain and type not matching.");
        }
        for (int i = 0; i < typeLen; i++) {
            // Call parseHelper here to compute each node; the most important part of parser.
            nodes.add(parseHelper(types[i], exDomain[i], ranDomain[i]));
        }
        return new ConfigFile(funcName, nodes, numRand);
    }

    /**
     * Helper function createMap,
     * help to convert the content of config file to a map from string to string.
     * @param contents, a string representing the contents of the config file.
     * @return contentMap, the map from a string to string,
     * where each key represents the title (such as types, exhaustive domains),
     * and each value represents the corresponding value.
     */
    public static Map<String, String> createMap(String contents) {
        String[] splitting = contents.split("\n");
        List<String> temp = new ArrayList<>();
        // Add contents to a list line by line.
        for (String line : splitting) {
            if (!line.isEmpty()) {
                temp.add(line.strip());
            }
        }
        Map<String, String> contentMap = new HashMap<>();
        for (String line : temp) { // Iterate over the list and create map.
            if (line.contains(":")) {
                int colonIndex = line.indexOf(":");
                String key = line.substring(0, colonIndex).replace("\"", "");
                String value = line.substring(colonIndex + 1);
                while (value.endsWith(",")) {
                    value = value.substring(0, value.length() - 1);
                }
                key = key.strip();
                value = value.strip();
                contentMap.put(key, value);
            }
        }
        return contentMap;
    }

    /**
     * Helper function to check if there's one or more key missing in the config file.
     * Also help to check if the value of each key is of correct format (only consider out-most layer)
     * This is a very fundamental check, may help to decrease later work load.
     * @param contentMap, a map from string to string that is the result of createMap.
     * @throws InvalidConfigException when there's missing function, type, or domain name in the config file.
     */
    public static void missingKeyCheck(Map<String, String> contentMap) throws InvalidConfigException {
        if (!contentMap.containsKey("fname") // Missing function name.
                || !contentMap.get("fname").matches("\"([a-zA-Z]+[0-9]*)+\"")) {
            throw new InvalidConfigException("Missing fname or invalid function name.");
        } else if (!contentMap.containsKey("types") // Missing types name.
                || !contentMap.get("types").startsWith("[")
                || !contentMap.get("types").endsWith("]")) {
            throw new InvalidConfigException("Missing types or invalid types.");
        } else if (!contentMap.containsKey("exhaustive domain") // Missing exhautive domain name.
                || !contentMap.get("exhaustive domain").startsWith("[")
                || !contentMap.get("exhaustive domain").endsWith("]")) {
            throw new InvalidConfigException("Missing exDomain or invalid exhaustive domain.");
        } else if (!contentMap.containsKey("random domain") // Missing random domain name.
                || !contentMap.get("random domain").startsWith("[")
                || !contentMap.get("random domain").endsWith("]")) {
            throw new InvalidConfigException("Missing ranDomain or invalid random domain");
        } else if (!contentMap.containsKey("num random") // Missing random number field.
                || !contentMap.get("num random").matches("[0-9]+")) {
            throw new InvalidConfigException("Missing random number or invalid random number");
        }
    }

    /**
     * The most important helper function.
     * Help to parse type, exDomain, and ranDomain in a recursive way.
     * Will classify types into many categories: primitive/reference, int-value/float-value/char domain.
     * Will return a APyNode object for recursion or final result.
     * @param type, a string representing the (composite) type(s) to be parsed.
     * @param exDomain, a string representing the (composite) exhaustive domain(s) to be parsed.
     * @param ranDomain, a string representing the (composite) random domain(s) to be parsed.
     * @return an APyNode object, representing the node in the current parsing stage.
     * @throws InvalidConfigException when spurious parenthesis, type signature incorrect, or many other cases.
     */
    public static APyNode<?> parseHelper(String type, String exDomain, String ranDomain) throws InvalidConfigException {
        if (type.matches("[a-zA-Z]+( )*")) { // Base case: primitive types.
            if (!type.matches("( )*float( )*")) { // Type will match int and bool.
                // Call tranIntDomain helper function to turn the domain of Number object to integer.
                List<Integer> correctEx = tranIntDomain(parseInterval(exDomain));
                List<Integer> correctRan = tranIntDomain(parseInterval(ranDomain));
                if (type.matches("( )*int( )*")) { // Type matches int.
                    PyIntNode intNode = new PyIntNode();
                    intNode.setExDomain(correctEx);
                    intNode.setRanDomain(correctRan);
                    return intNode;
                } else if (type.matches("( )*bool( )*")) { // Type matches bool.
                    PyBoolNode boolNode = new PyBoolNode();
                    boolNode.setExDomain(correctEx);
                    boolNode.setRanDomain(correctRan);
                    return boolNode;
                }
            }
            else { // Type is float.
                PyFloatNode floatNode = new PyFloatNode();
                List<Double> floatEx = new ArrayList<>();
                List<Double> floatRan = new ArrayList<>();
                // Turn every Number element in the domain to a double value.
                for (Number num : parseInterval(exDomain)) {
                    floatEx.add(num.doubleValue());
                }
                for (Number num : parseInterval(ranDomain)) {
                    floatRan.add(num.doubleValue());
                }
                floatNode.setExDomain(floatEx);
                floatNode.setRanDomain(floatRan);
                return floatNode;
            }
        }
        else if (type.contains("(") && type.indexOf("(") != 0) { // Recursive case: reference types.
            // Split each input based on the position of the first left parenthesis.
            String copyTypePrev = type.substring(0, type.indexOf("("));
            String copyTypeAfter = type.substring(type.indexOf("(") + 1);
            copyTypeAfter = copyTypeAfter.strip(); // Remember to strip out the space in the front of the string.
            if (!copyTypePrev.matches("( )*str( )*")) {
                // Type matches list, tuple, dict, or set.
                if (!exDomain.contains("(") || !ranDomain.contains("(")) {
                    // Small check for the position of left parenthesis.
                    throw new InvalidConfigException("#Parenthesis not matching for type and domain.");
                }
                // Get the substring before and after the first left parenthesis.
                // Remember to strip out the space.
                String copyExPrev = exDomain.substring(0, exDomain.indexOf("("));
                String copyRanPrev = ranDomain.substring(0, ranDomain.indexOf("("));
                String copyExAfter = exDomain.substring(exDomain.indexOf("(") + 1);
                String copyRanAfter = ranDomain.substring(ranDomain.indexOf("(") + 1);
                copyExAfter = copyExAfter.strip();
                copyRanAfter = copyRanAfter.strip();

                if (!copyTypePrev.matches("( )*dict( )*")) { // Type matches list, tuple, or set.
                    // Call tranIntDomain helper function to turn the domain of Number object to integer.
                    APyNode<?> inner = parseHelper(copyTypeAfter, copyExAfter, copyRanAfter);
                    List<Integer> correctEx = tranIntDomain(parseInterval(copyExPrev));
                    List<Integer> correctRan = tranIntDomain(parseInterval(copyRanPrev));

                    if (copyTypePrev.matches("( )*list( )*")) { // Type matches list.
                        PyListNode<?> listNode = new PyListNode<>(inner);
                        listNode.setExDomain(correctEx);
                        listNode.setRanDomain(correctRan);
                        return listNode;
                    } else if (copyTypePrev.matches("( )*tuple( )*")) { // Type matches tuple.
                        PyTupleNode<?> tupleNode = new PyTupleNode<>(inner);
                        tupleNode.setExDomain(correctEx);
                        tupleNode.setRanDomain(correctRan);
                        return tupleNode;
                    } else if (copyTypePrev.matches("( )*set( )*")) { // Type matches set.
                        PySetNode<?> setNode = new PySetNode<>(inner);
                        setNode.setExDomain(correctEx);
                        setNode.setRanDomain(correctRan);
                        return setNode;
                    }
                }
                else { // Type matches dict, which is the most complicated case.
                    // Look for the first colon appearance and parse key and value.
                    int indexType = copyTypeAfter.indexOf(":");
                    int indexEx = copyExAfter.indexOf(":");
                    int indexRan = copyRanAfter.indexOf(":");
                    String keyType = copyTypeAfter.substring(0, indexType);
                    String valueType = copyTypeAfter.substring(indexType + 1);
                    valueType = valueType.strip(); // Remember to strip the space.
                    String keyEx = copyExAfter.substring(0, indexEx);
                    String valueEx = copyExAfter.substring(indexEx + 1);
                    valueEx = valueEx.strip(); // Remember to strip the space.
                    String keyRan = copyRanAfter.substring(0, indexRan);
                    String valueRan = copyRanAfter.substring(indexRan + 1);
                    valueRan = valueRan.strip(); // Remember to strip the space.

                    // Call parseHelper to recursively compute the left and right child node.
                    APyNode<?> innerKey = parseHelper(keyType, keyEx, keyRan);
                    APyNode<?> innerValue = parseHelper(valueType, valueEx, valueRan);

                    PyDictNode<?, ?> dictNode = new PyDictNode<>(innerKey, innerValue);
                    dictNode.setExDomain(parseInterval(copyExPrev));
                    dictNode.setRanDomain(parseInterval(copyRanPrev));
                    return dictNode;
                }
            }
            else { // Type matches string.
                Set<Character> charDomain = new HashSet<>();
                for (int i = 0; i < copyTypeAfter.length(); i++) {
                    charDomain.add(copyTypeAfter.charAt(i));
                }
                PyStringNode stringNode = new PyStringNode(charDomain);
                stringNode.setExDomain(parseInterval(exDomain));
                stringNode.setRanDomain(parseInterval(ranDomain));
                return stringNode;
            }
        }
        // For any other case, regard them as invalid type signature.
        throw new InvalidConfigException("Invalid type signature.");
    }

    /**
     * One of the most important helper functions in the program.
     * Take in a string representing the interval to be parsed. Turn it into a list of Number objects.
     * There are two cases: one containing ~ (tilde) or the one with brackets.
     * The interval after parsed is not the final form, still need to be processed in the parHelper.
     * @param interval, a string representing the interval to be parsed.
     * @return result, a list of Number objects representing the interval after parsed.
     * @throws InvalidConfigException when the interval is invalid.
     */
    public static List<Number> parseInterval(String interval) throws InvalidConfigException{
        List<Number> result = new ArrayList<>();
        interval = interval.strip();
        // Set up regular expression to represent tilde interval form.
        if (interval.matches("(-)?[0-9]+(~)(-)?[0-9]+( )*")) {
            int tildeIndex = interval.indexOf("~");
            String left = interval.substring(0, tildeIndex);
            String right = interval.substring(tildeIndex + 1);
            while (right.endsWith(" ")) { // Strip out the space in the end of string.
                right = right.substring(0, right.length() - 1);
            }
            // Turn string to a integer.
            int leftNum = Integer.parseInt(left);
            int rightNum = Integer.parseInt(right);

            if (leftNum > rightNum) { // Check if the left number is greater than the right number.
                throw new InvalidConfigException("Invalid interval with left number greater than the right.");
            }
            for (int i = leftNum; i <= rightNum; i++) {
                result.add(i);
            }
        }
        // Set up regular expression to represent the bracket interval form.
        else if (interval.matches("\\[(-?[0-9]+(\\.[0-9]+)?, )*(-?[0-9]+(\\.[0-9]+)?)\\]")){
            String copyInterval = interval.substring(1, interval.length() - 1);
            String[] temp = copyInterval.split(", ");
            for (String num : temp) {
                // Considering the existence of float, we need to put in double value of every number.
                Double ans = Double.parseDouble(num);
                result.add(ans);
            }
        }
        else { // If the interval is not of the form above, then it's invalid.
            throw new InvalidConfigException("Invalid interval. Cannot be parsed.");
        }
        return result;
    }

    /**
     * Helper function to turn a Number list to an Integer list.
     * Will be used in types that have integers in their exhaustive or random domains,
     * such as int, bool, list, tuple, set, and dict.
     * @param numDomain, a list of Number objects that is returned by parseInterval method.
     * @return intDomain, a list of Integer objects that is transformed from Number objects.
     * @throws InvalidConfigException when the number is a decimal.
     */
    public static List<Integer> tranIntDomain(List<Number> numDomain) throws InvalidConfigException{
        List<Integer> intDomain = new ArrayList<>();
        for (Number num : numDomain) {
            double actual = num.doubleValue();
            if (Math.floor(actual) != actual) { // Check if the number is a decimal.
                throw new InvalidConfigException("Need integers instead of decimals");
            }
            intDomain.add(num.intValue());
        }
        return intDomain;
    }
}