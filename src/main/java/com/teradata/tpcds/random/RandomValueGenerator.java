package com.teradata.tpcds.random;

import com.teradata.tpcds.distribution.AdjectivesDistribution;
import com.teradata.tpcds.distribution.AdverbsDistribution;
import com.teradata.tpcds.distribution.ArticlesDistribution;
import com.teradata.tpcds.distribution.AuxilariesDistribution;
import com.teradata.tpcds.distribution.CalendarDistribution;
import com.teradata.tpcds.distribution.NounsDistribution;
import com.teradata.tpcds.distribution.PrepositionsDistribution;
import com.teradata.tpcds.distribution.SentencesDistribution;
import com.teradata.tpcds.distribution.SyllablesDistribution;
import com.teradata.tpcds.distribution.TerminatorsDistribution;
import com.teradata.tpcds.distribution.TopDomainsDistribution;
import com.teradata.tpcds.distribution.VerbsDistribution;
import com.teradata.tpcds.type.Date;
import com.teradata.tpcds.type.Decimal;

import static com.teradata.tpcds.type.Date.fromJulianDays;
import static com.teradata.tpcds.type.Date.getDaysInYear;
import static com.teradata.tpcds.type.Date.toJulianDays;
import static java.util.Objects.requireNonNull;

public final class RandomValueGenerator
{
    private static final String ALPHA_NUMERIC = "abcdefghijklmnopqrstuvxyzABCDEFGHIJKLMNOPQRSTUVXYZ0123456789";

    private RandomValueGenerator() {}

    public static int generateUniformRandomInt(int min, int max, RandomNumberStream randomNumberStream)
    {
        int result = (int) randomNumberStream.nextRandom(); // truncating long to int copies behavior of c code.
        result %= max - min + 1;
        result += min;
        return result;
    }

    public static long generateUniformRandomKey(long min, long max, RandomNumberStream randomNumberStream)
    {
        int result = (int) randomNumberStream.nextRandom(); // truncating long to int copies behavior of c code
        result %= (int) (max - min + 1);
        result += (int) min;
        return result;
    }

    public static Decimal generateUniformRandomDecimal(Decimal min, Decimal max, RandomNumberStream randomNumberStream)
    {
        int precision = min.getPrecision() < max.getPrecision() ? min.getPrecision() : max.getPrecision();

        // compute number
        long number = randomNumberStream.nextRandom();
        number %= max.getNumber() - min.getNumber() + 1;
        number += min.getNumber();

        // compute scale
        int scale = 0;
        long n = number;
        while (n > 10) {
            n /= 10;
            scale += 1;
        }
        return new Decimal(precision, scale, number);
    }

    public static Date generateUniformRandomDate(Date min, Date max, RandomNumberStream randomNumberStream)
    {
        int range = toJulianDays(max) - toJulianDays(min);
        int julianDays = toJulianDays(min) + generateUniformRandomInt(0, range, randomNumberStream);
        return fromJulianDays(julianDays);
    }

    public static Date generateSalesReturnsRandomDate(Date min, Date max, int distIndex, RandomNumberStream randomNumberStream)
    {
        // get random date based on distribution.
        // Copying behavior of dsdgen, but unclear what's going on there
        // Date.day represents the day of the month, but for some reason
        // it is interpreted in genrand_date as the day of the year.
        // That means we always start somewhere in January, I guess.
        int dayCount = min.getDay();
        int year = min.getYear();
        int totalWeight = 0;
        int range = toJulianDays(max) - toJulianDays(min);
        //TODO: may want to pass in the distribution itself, not an index.
        // calculate the sum of all the weights in the range
        for (int i = 0; i < range; i++) {
            totalWeight += CalendarDistribution.getWeight(dayCount, distIndex);
            if (dayCount == getDaysInYear(year)) {
                year += 1;
                dayCount = 1;
            }
            else {
                dayCount += 1;
            }
        }

        // Choose a random int up to totalWeight.
        // Then work backwards to get the first date where the chosen number is
        // greater than or equal to the sum of all weights from min to date.
        int tempWeightSum = generateUniformRandomInt(1, totalWeight, randomNumberStream);
        dayCount = min.getDay();
        int julianDays = Date.toJulianDays(min);
        year = min.getYear();
        while (tempWeightSum > 0) {
            tempWeightSum -= CalendarDistribution.getWeight(dayCount, distIndex);
            dayCount += 1;
            julianDays += 1;
            if (dayCount > getDaysInYear(year)) {
                dayCount = 1;
                year += 1;
            }
        }

        return fromJulianDays(julianDays);
    }

    public static String generateRandomCharset(String set, int min, int max, RandomNumberStream randomNumberStream)
    {
        requireNonNull(set, "set is null");

        int length = generateUniformRandomInt(min, max, randomNumberStream);
        StringBuilder builder = new StringBuilder();

        // It seems like it would make more sense to make length the loop condition.
        // For some reason dsdgen doesn't do that, and we want the RNG seeds to be the same
        // so we copy the behavior.
        for (int i = 0; i < max; i++) {
            int index = generateUniformRandomInt(0, set.length() - 1, randomNumberStream);
            if (i < length) {
                builder.append(set.charAt(index));
            }
        }
        return builder.toString();
    }

    public static String generateRandomEmail(String first, String last, RandomNumberStream randomNumberStream)
    {
        String domain = TopDomainsDistribution.pickRandomValue(1, 1, randomNumberStream);
        int companyLength = generateUniformRandomInt(10, 20, randomNumberStream);
        String company = generateRandomCharset(ALPHA_NUMERIC, 1, 20, randomNumberStream);
        company = company.substring(0, companyLength);

        return String.format("%s.%s@%s.%s", first, last, company, domain);
    }

    public static String generateRandomIpAddress(RandomNumberStream randomNumberStream)
    {
        int ipSegments[] = new int[4];

        for (int i = 0; i < 4; i++)
            ipSegments[i] = generateUniformRandomInt(1, 255, randomNumberStream);

        return String.format("%d.%d.%d.%d", ipSegments[0], ipSegments[1], ipSegments[2], ipSegments[3]);
    }

    public static String generateRandomUrl(RandomNumberStream randomNumberStream)
    {
        return "http://www.foo.com";  // This is what the c code does. No joke.
    }

    public static int generateExponentialRandomInt(int min, int max, RandomNumberStream randomNumberStream)
    {
        int range = max - min + 1;
        double doubleResult = 0;
        for (int i = 0; i < 12; i++) {
            doubleResult += randomNumberStream.nextRandomDouble() - 0.5;
        }
        return min + (int) (range * doubleResult);
    }

    public static long generateExponentialRandomKey(long min, long max, RandomNumberStream randomNumberStream)
    {
        double doubleResult = 0;
        for (int i = 0; i < 12; i++)
            doubleResult += (double) (randomNumberStream.nextRandom() / Integer.MAX_VALUE) - 0.5;
        return (int) min + (int) ((max - min + 1) * doubleResult); // truncating long to int copies behavior of c code
    }

    public static Decimal generateExponentialRandomDecimal(Decimal min, Decimal max, Decimal mean, RandomNumberStream randomNumberStream)
    {
        // compute precision
        int precision = min.getPrecision() < max.getPrecision() ? min.getPrecision() : max.getPrecision();

        // compute number
        double doubleResult = 0;
        for (int i = 0; i < 12; i++) {
            doubleResult /= 2.0;
            doubleResult += (double) randomNumberStream.nextRandom() / Integer.MAX_VALUE - 0.5;
        }

        long number = mean.getNumber() + (int) ((max.getNumber() - min.getNumber() + 1) * doubleResult);

        // compute scale
        int scale = 0;
        long n = number;
        while (n > 10) {
            n /= 10;
            scale += 1;
        }
        return new Decimal(precision, scale, number);
    }

    public static Date generateExponentialRandomDate(Date min, Date max, RandomNumberStream randomNumberStream)
    {
        int range = toJulianDays(max) - toJulianDays(min);
        int days = toJulianDays(min) + generateExponentialRandomInt(0, range, randomNumberStream);
        return fromJulianDays(days);
    }

    public static String generateRandomText(int minLength, int maxLength, RandomNumberStream stream)
    {
        boolean isSentenceBeginning = true;
        StringBuilder text = new StringBuilder();
        int targetLength = generateUniformRandomInt(minLength, maxLength, stream);

        while (targetLength > 0) {
            String generated = generateRandomSentence(stream);
            if (isSentenceBeginning) {
                generated = generated.substring(0, 1).toUpperCase() + generated.substring(1);
            }

            int generatedLength = generated.length();
            isSentenceBeginning = (generated.charAt(generatedLength - 1) == '.');

            // truncate so as not to exceed target length
            if (targetLength <= generatedLength) {
                generated = generated.substring(0, targetLength);
            }

            targetLength -= generatedLength;

            text.append(generated);
            if (targetLength > 0) {
                text.append(" ");
                targetLength -= 1;
            }
        }

        return text.toString();
    }

    private static String generateRandomSentence(RandomNumberStream stream)
    {
        StringBuilder verbiage = new StringBuilder();
        String syntax = SentencesDistribution.pickRandomValue(1, 1, stream);
        for (int i = 0; i < syntax.length(); i++) {
            switch (syntax.charAt(i)) {
                case 'N':
                    verbiage.append(NounsDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'V':
                    verbiage.append(VerbsDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'J':
                    verbiage.append(AdjectivesDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'D':
                    verbiage.append(AdverbsDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'X':
                    verbiage.append(AuxilariesDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'P':
                    verbiage.append(PrepositionsDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'A':
                    verbiage.append(ArticlesDistribution.pickRandomValue(1, 1, stream));
                    break;
                case 'T':
                    verbiage.append(TerminatorsDistribution.pickRandomValue(1, 1, stream));
                    break;
                default:
                    verbiage.append(syntax.charAt(i));  // this is for patterns that include punctuation.
                    break;
            }
        }
        return verbiage.toString();
    }

    // TODO: there are some callers of mk_word() that use "syllables" as the distribution name .  Others use "Syllables".
    // There is no distribution that has the upper case name. I wonder if distribution names are case insensitive.
    public static String generateWord(long seed, int maxChars)
    {
        long size = SyllablesDistribution.getSize();
        StringBuilder word = new StringBuilder();
        while (seed > 0) {
            String syllable = SyllablesDistribution.getMemberString((int) (seed % size) + 1, 1);
            seed /= size;
            if ((word.length() + syllable.length()) <= maxChars) {
                word.append(syllable);
            }
            else {
                break;
            }
        }
        return word.toString();
    }
}

