package randomgen.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;

import java.math.BigInteger;
import java.util.Random;

public class RandomJsonGenerator {
    private final RandomJsonGeneratorConfig config;
    private int arrayOrObjectNodes; // equal or larger than the max node depth

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
        this.arrayOrObjectNodes = 0;
    }

    public JsonNode createRandomJsonNode() {
        NodeType nodeType = getRandomNodeType();
        if (arrayOrObjectNodes >= config.getMaxNodeDepth()) {
            nodeType = NodeType.stringNode; // don't add depth, just value (String) nodes.
        }
        if ((arrayOrObjectNodes < config.getMaxNodeDepth() / 3) && (nodeType != NodeType.objectNode && nodeType != NodeType.arrayNode)) {
            nodeType = NodeType.objectNode;
        }
        return switch (nodeType) {
            case arrayNode -> createRandomArrayNode();
            case numberNode -> createRandomNumericNode();
            case objectNode -> createRandomObjectNode();
            case booleanNode -> createRandomBooleanNode();
            case stringNode -> createRandomTextNode();
            case nullNode -> JsonNodeFactory.instance.nullNode();
        };
    }

    public NodeType getRandomNodeType() {
        int rnd = new Random().nextInt(NodeType.values().length);
        return NodeType.values()[rnd];
    }

    private TextNode createRandomTextNode() {
        return JsonNodeFactory.instance.textNode(getRandomString());
    }

    private BooleanNode createRandomBooleanNode() {
        int rnd = new Random().nextInt(3); // 1 or 2
        return JsonNodeFactory.instance.booleanNode(rnd == 2);
    }

    private ObjectNode createRandomObjectNode() {
        ObjectNode objectNode = JsonNodeFactory.instance.objectNode();
        int nrOfObjectKeys = new Random().nextInt(config.getMaxObjectKeys());
        for (int i = 0; i < nrOfObjectKeys; i++) {
            if (keyIsTargetKey()) {
                objectNode.set(getRandomTargetKey(), createRandomJsonNode());
            } else {
                objectNode.set(getRandomString(), createRandomJsonNode());
            }
        }
        arrayOrObjectNodes++;
        return objectNode;
    }

    private boolean keyIsTargetKey() {
        int rnd = new Random().nextInt(1,101);
        return rnd <= config.getTargetKeyPercentage();
    }

    private String getRandomTargetKey() {
        int rnd = new Random().nextInt(config.getTargetKeys().size());
        return (String) config.getTargetKeys().toArray()[rnd];
    }

    private String getRandomString() {
        int stringLength = new Random().nextInt(config.getMaxStringLength());
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < stringLength; i++) {
            sb.append(getRandomCharacter());
        }
        return sb.toString();
    }

    private Character getRandomCharacter() {
        Character[] characters = config.getStringCharacters().toArray(new Character[0]);
        int randomIndex = new Random().nextInt(characters.length - 1);
        return characters[randomIndex];
    }

    private NumericNode createRandomNumericNode() {
        int rnd = new Random().nextInt(1, 5);
        Random random = new Random();
        return switch (rnd) {
            case 1 -> JsonNodeFactory.instance.numberNode(random.nextLong(config.getMaxLong()));
            case 2 -> JsonNodeFactory.instance.numberNode(random.nextFloat(config.getMaxFloat()));
            case 3 -> JsonNodeFactory.instance.numberNode(random.nextDouble(config.getMaxDouble()));
            case 4 -> BigIntegerNode.valueOf(new BigInteger(config.getMaxBigInt().bitLength(), new Random()));
            default -> throw new IllegalStateException("Unexpected value: " + rnd);
        };
    }

    private ArrayNode createRandomArrayNode() {
        ArrayNode arrayNode = JsonNodeFactory.instance.arrayNode();
        int nrOfArrayElements = new Random().nextInt(config.getMaxArraySize());
        for (int i = 0; i < nrOfArrayElements; i++) {
            arrayNode.add(createRandomJsonNode());
        }
        arrayOrObjectNodes++;
        return arrayNode;
    }


}
