package randomgen.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BigIntegerNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.NullNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ThreadLocalRandom;

public class RandomJsonGenerator {
    private final RandomJsonGeneratorConfig config;
    private final ThreadLocalRandom random = ThreadLocalRandom.current();

    private enum NodeType {
        arrayNode,
        objectNode,
        booleanNode,
        nullNode,
        stringNode,
        numberNode
    }

    public RandomJsonGenerator(RandomJsonGeneratorConfig config) {
        this.config = config;
    }

    public JsonNode createRandomJsonNode() {
        return createRandomJsonNode(new Context(), 0);
    }

    private JsonNode createRandomJsonNode(Context context, int depth) {
        boolean primitiveOnly = depth >= config.getMaxNodeDepth();
        NodeType nodeType = getRandomNodeType(primitiveOnly);
        if (reachedTargetSize(context.estimatedSizeBytes + 4)) {
            nodeType = NodeType.nullNode;
        } else if (config.hasTargetSize() && depth == 0) {
            // always start with an object root if generating json of certain size
            nodeType = NodeType.objectNode;
        } else if ((depth < config.getMaxNodeDepth() / 3) && (nodeType != NodeType.objectNode
                && nodeType != NodeType.arrayNode)) {
            // forcefully override chance to 50% to create an object if only <33% of max node depth is reached
            if (random.nextBoolean()) {
                nodeType = NodeType.objectNode;
            }
        }
        return switch (nodeType) {
            case arrayNode -> createRandomArrayNode(context, depth + 1);
            case objectNode -> createRandomObjectNode(context, depth + 1);
            case numberNode -> createRandomNumericNode(context);
            case booleanNode -> createRandomBooleanNode(context);
            case stringNode -> createRandomTextNode(context);
            case nullNode -> createNullNode(context);
        };
    }

    private JsonNode createNullNode(Context context) {
        NullNode node = JsonNodeFactory.instance.nullNode();
        context.estimatedSizeBytes += sizeOf(node);
        return node;
    }

    private NodeType getRandomNodeType(boolean primitiveOnly) {
        int offset = primitiveOnly ? NodeType.objectNode.ordinal() + 1 : 0;
        int rnd = random.nextInt(offset, NodeType.values().length);
        return NodeType.values()[rnd];
    }

    private TextNode createRandomTextNode(Context context) {
        TextNode node = JsonNodeFactory.instance.textNode(getRandomString());
        context.estimatedSizeBytes += sizeOf(node);
        return node;
    }

    private BooleanNode createRandomBooleanNode(Context context) {
        BooleanNode node = JsonNodeFactory.instance.booleanNode(random.nextBoolean());
        context.estimatedSizeBytes += sizeOf(node);
        return node;
    }

    private ObjectNode createRandomObjectNode(Context context, int depth) {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        context.estimatedSizeBytes += sizeOf(objectNode);
        int nrOfObjectKeys;
        if (depth == 1 && config.hasTargetSize()) {
            // the root object to be populated until we reach the target size
            nrOfObjectKeys = Integer.MAX_VALUE;
        } else {
            nrOfObjectKeys = random.nextInt(config.getMaxObjectKeys());
        }
        for (int i = 0; i < nrOfObjectKeys; i++) {
            String key = randomKey();
            while (objectNode.has(key)) {
                key = randomKey();
            }
            context.estimatedSizeBytes += sizeOf(JsonNodeFactory.instance.textNode(key));
            context.estimatedSizeBytes += 1; // for the semicolon
            if (i > 0) {
                context.estimatedSizeBytes += 1; // for the comma
            }
            JsonNode child = createRandomJsonNode(context, depth);
            objectNode.set(key, child);
            if (reachedTargetSize(context.estimatedSizeBytes)) {
                break;
            }
        }
        return objectNode;
    }

    private String randomKey() {
        double rnd = random.nextDouble(0, 1);
        return rnd <= config.getTargetKeyPercentage() ? getRandomTargetKey() : getRandomString();
    }

    private String getRandomTargetKey() {
        int rnd = random.nextInt(config.getTargetKeys().size());
        return randomizeCase((String) config.getTargetKeys().toArray()[rnd]);
    }

    private String randomizeCase(String input) {
        StringBuilder resultBuilder = new StringBuilder();

        for (char c : input.toCharArray()) {
            // Generate a random boolean to decide whether to convert to uppercase or lowercase
            boolean toUpperCase = random.nextBoolean();

            // Convert the character to uppercase or lowercase based on the random boolean
            char convertedChar = toUpperCase ? Character.toUpperCase(c) : Character.toLowerCase(c);

            // Append the converted character to the result string
            resultBuilder.append(convertedChar);
        }

        return resultBuilder.toString();
    }

    private String getRandomString() {
        int stringLength = random.nextInt(config.getMaxStringLength());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            sb.append(getRandomCharacter());
        }
        return sb.toString();
    }

    private Character getRandomCharacter() {
        Character[] characters = config.getAllowedCharacters().toArray(new Character[0]);
        int randomIndex = random.nextInt(characters.length - 1);
        return characters[randomIndex];
    }

    private NumericNode createRandomNumericNode(Context context) {
        int rnd = random.nextInt(1, 5);
        NumericNode node = switch (rnd) {
            case 1 -> JsonNodeFactory.instance.numberNode(random.nextLong(config.getMaxLong()));
            case 2 -> JsonNodeFactory.instance.numberNode(random.nextFloat(config.getMaxFloat()));
            case 3 -> JsonNodeFactory.instance.numberNode(random.nextDouble(config.getMaxDouble()));
            case 4 -> BigIntegerNode.valueOf(new BigInteger(config.getMaxBigInt().bitLength(), random));
            default -> throw new IllegalStateException("Unexpected value: " + rnd);
        };
        context.estimatedSizeBytes += sizeOf(node);
        return node;
    }

    private ArrayNode createRandomArrayNode(Context context, int depth) {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        context.estimatedSizeBytes += sizeOf(arrayNode);
        int nrOfArrayElements = random.nextInt(config.getMaxArraySize());
        for (int i = 0; i < nrOfArrayElements; i++) {
            if (i > 0) {
                context.estimatedSizeBytes += 1; // for the comma
            }
            arrayNode.add(createRandomJsonNode(context, depth));
            if (reachedTargetSize(context.estimatedSizeBytes)) {
                break;
            }
        }
        return arrayNode;
    }

    private boolean reachedTargetSize(int estimatedSizeBytes) {
        return config.hasTargetSize() && estimatedSizeBytes >= config.getTargetJsonSizeBytes();
    }

    private int sizeOf(JsonNode node) {
        return node.toString().getBytes(StandardCharsets.UTF_8).length;
    }

    private static class Context {
        int estimatedSizeBytes;
    }
}
