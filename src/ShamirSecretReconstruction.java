import org.json.JSONArray;
import org.json.JSONObject;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class ShamirSecretReconstruction {

    static class Share {
        int x;
        BigInteger y;
        Share(int x, BigInteger y) {
            this.x = x;
            this.y = y;
        }
    }
    public static BigInteger lagrangeInterpolation(List<Share> shares, BigInteger primeMod) {
        BigInteger secret = BigInteger.ZERO;
        int k = shares.size();

        for (int i = 0; i < k; i++) {
            BigInteger xi = BigInteger.valueOf(shares.get(i).x);
            BigInteger yi = shares.get(i).y;

            BigInteger numerator = BigInteger.ONE;
            BigInteger denominator = BigInteger.ONE;

            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = BigInteger.valueOf(shares.get(j).x);
                numerator = numerator.multiply(xj.negate()).mod(primeMod);
                denominator = denominator.multiply(xi.subtract(xj)).mod(primeMod);
            }

            BigInteger lagrange = numerator.multiply(denominator.modInverse(primeMod)).mod(primeMod);
            secret = secret.add(yi.multiply(lagrange)).mod(primeMod);
        }

        return secret;
    }

    public static void generateCombinations(List<Share> shares, int k, int start, List<Share> temp, List<List<Share>> result) {
        if (temp.size() == k) {
            result.add(new ArrayList<>(temp));
            return;
        }
        for (int i = start; i < shares.size(); i++) {
            temp.add(shares.get(i));
            generateCombinations(shares, k, i + 1, temp, result);
            temp.remove(temp.size() - 1);
        }
    }

    public static void main(String[] args) throws IOException {
        String jsonInput = Files.readString(Paths.get("input.json"));
        JSONObject jsonObject = new JSONObject(jsonInput);

        int n = jsonObject.getInt("n");
        int k = jsonObject.getInt("k");
        BigInteger primeMod = new BigInteger(jsonObject.getString("prime"));
        JSONArray sharesArray = jsonObject.getJSONArray("shares");

        List<Share> allShares = new ArrayList<>();
        for (int i = 0; i < sharesArray.length(); i++) {
            JSONObject share = sharesArray.getJSONObject(i);
            int x = share.getInt("x");
            BigInteger y = new BigInteger(share.getString("y"));
            allShares.add(new Share(x, y));
        }

        List<List<Share>> combinations = new ArrayList<>();
        generateCombinations(allShares, k, 0, new ArrayList<>(), combinations);

        Map<BigInteger, Integer> frequencyMap = new HashMap<>();
        Map<BigInteger, List<List<Share>>> secretToCombos = new HashMap<>();

        for (List<Share> combo : combinations) {
            BigInteger secret = lagrangeInterpolation(combo, primeMod);
            frequencyMap.put(secret, frequencyMap.getOrDefault(secret, 0) + 1);
            secretToCombos.computeIfAbsent(secret, s -> new ArrayList<>()).add(combo);
        }
        BigInteger correctSecret = null;
        int maxFreq = 0;
        for (Map.Entry<BigInteger, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                correctSecret = entry.getKey();
            }
        }

        System.out.println("✅ Reconstructed Secret: " + correctSecret);
        
        Set<String> validShareSet = new HashSet<>();
        for (Share s : secretToCombos.get(correctSecret).get(0)) {
            validShareSet.add(s.x + ":" + s.y);
        }

        System.out.println("\n✅ Valid Shares:");
        for (String valid : validShareSet) System.out.println(valid);

        System.out.println("\n❌ Possibly Corrupt Shares:");
        for (Share s : allShares) {
            String key = s.x + ":" + s.y;
            if (!validShareSet.contains(key)) {
                System.out.println(key);
            }
        }
    }
}

