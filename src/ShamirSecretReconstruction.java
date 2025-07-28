// import java.math.BigInteger;
// import java.nio.file.Files;
// import java.nio.file.Paths;
// import java.util.*;
// import org.json.*;

// public class ShamirSecretReconstruction {

//     public static BigInteger lagrangeInterpolation(BigInteger[] x, BigInteger[] y, BigInteger prime) {
//         BigInteger secret = BigInteger.ZERO;
//         int k = x.length;

//         for (int i = 0; i < k; i++) {
//             BigInteger numerator = BigInteger.ONE;
//             BigInteger denominator = BigInteger.ONE;

//             for (int j = 0; j < k; j++) {
//                 if (i != j) {
//                     numerator = numerator.multiply(x[j].negate()).mod(prime);
//                     denominator = denominator.multiply(x[i].subtract(x[j])).mod(prime);
//                 }
//             }

//             BigInteger term = y[i].multiply(numerator).mod(prime);
//             BigInteger invDenominator = denominator.modInverse(prime);
//             term = term.multiply(invDenominator).mod(prime);
//             secret = secret.add(term).mod(prime);
//         }

//         return secret;
//     }

//     public static List<List<BigInteger[]>> getCombinations(List<BigInteger[]> shares, int k) {
//         List<List<BigInteger[]>> result = new ArrayList<>();
//         combineHelper(shares, new ArrayList<>(), 0, k, result);
//         return result;
//     }

//     private static void combineHelper(List<BigInteger[]> shares, List<BigInteger[]> temp, int start, int k, List<List<BigInteger[]>> result) {
//         if (k == 0) {
//             result.add(new ArrayList<>(temp));
//             return;
//         }

//         for (int i = start; i <= shares.size() - k; i++) {
//             temp.add(shares.get(i));
//             combineHelper(shares, temp, i + 1, k - 1, result);
//             temp.remove(temp.size() - 1);
//         }
//     }

//     public static void main(String[] args) throws Exception {
//         String json = Files.readString(Paths.get("input.json"));
//         JSONObject obj = new JSONObject(json);

//         int n = obj.getInt("n");
//         int k = obj.getInt("k");
//         JSONArray arr = obj.getJSONArray("shares");

//         List<BigInteger[]> shares = new ArrayList<>();
//         for (int i = 0; i < arr.length(); i++) {
//             JSONArray pair = arr.getJSONArray(i);
//             BigInteger x = new BigInteger(pair.get(0).toString());
//             BigInteger y = new BigInteger(pair.get(1).toString());
//             shares.add(new BigInteger[]{x, y});
//         }

//         BigInteger prime = new BigInteger("208351617316091241234326746312124448251235562226470491514186331217050270460481");

//         Map<BigInteger, Integer> secretCounts = new HashMap<>();
//         List<List<BigInteger[]>> combinations = getCombinations(shares, k);

//         for (List<BigInteger[]> combo : combinations) {
//             BigInteger[] x = new BigInteger[k];
//             BigInteger[] y = new BigInteger[k];

//             for (int i = 0; i < k; i++) {
//                 x[i] = combo.get(i)[0];
//                 y[i] = combo.get(i)[1];
//             }

//             BigInteger secret = lagrangeInterpolation(x, y, prime);
//             secretCounts.put(secret, secretCounts.getOrDefault(secret, 0) + 1);
//         }

//         BigInteger correctSecret = null;
//         int maxCount = 0;
//         for (Map.Entry<BigInteger, Integer> entry : secretCounts.entrySet()) {
//             if (entry.getValue() > maxCount) {
//                 maxCount = entry.getValue();
//                 correctSecret = entry.getKey();
//             }
//         }

//         System.out.println("✅ Reconstructed Secret: " + correctSecret);
//     }
// }



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

    // Lagrange interpolation to reconstruct the secret
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

    // Helper to generate all combinations of size k from n shares
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
        // Step 1: Read JSON input
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

        // Step 2: Generate all combinations of k out of n
        List<List<Share>> combinations = new ArrayList<>();
        generateCombinations(allShares, k, 0, new ArrayList<>(), combinations);

        // Step 3: Try all combinations to reconstruct the secret
        Map<BigInteger, Integer> frequencyMap = new HashMap<>();
        Map<BigInteger, List<List<Share>>> secretToCombos = new HashMap<>();

        for (List<Share> combo : combinations) {
            BigInteger secret = lagrangeInterpolation(combo, primeMod);
            frequencyMap.put(secret, frequencyMap.getOrDefault(secret, 0) + 1);
            secretToCombos.computeIfAbsent(secret, s -> new ArrayList<>()).add(combo);
        }

        // Step 4: Find the most frequent secret
        BigInteger correctSecret = null;
        int maxFreq = 0;
        for (Map.Entry<BigInteger, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFreq) {
                maxFreq = entry.getValue();
                correctSecret = entry.getKey();
            }
        }

        System.out.println("✅ Reconstructed Secret: " + correctSecret);

        // Optional: Identify likely correct shares
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

