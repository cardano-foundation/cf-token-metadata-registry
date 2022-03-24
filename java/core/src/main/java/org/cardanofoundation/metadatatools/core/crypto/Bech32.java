package org.cardanofoundation.metadatatools.core.crypto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

public class Bech32 {
    private static final int CHECKSUM_SIZE = 6;
    private static final char SEPARATOR = '1';
    private static final String CHARSET = "qpzry9x8gf2tvdw0s3jn54khce6mua7l";
    private static final byte[] CHARSET_REV = {
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
            15, -1, 10, 17, 21, 20, 26, 30,  7,  5, -1, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25,  9,  8, 23, -1, 18, 22, 31, 27, 19, -1,
            1,  0,  3, 16, 11, 28, 12, 14,  6,  4,  2, -1, -1, -1, -1, -1,
            -1, 29, -1, 24, 13, 25,  9,  8, 23, -1, 18, 22, 31, 27, 19, -1,
            1,  0,  3, 16, 11, 28, 12, 14,  6,  4,  2, -1, -1, -1, -1, -1
    };

    public enum Encoding {
        BECH32(1), BECH32M(0x2bc830a3);

        private final int constantValue;

        Encoding(final int constantValue) {
            this.constantValue = constantValue;
        }

        static Encoding fromConstantValue(final int constantValue) {
            if (constantValue == Encoding.BECH32.constantValue) {
                return Encoding.BECH32;
            } else if (constantValue == Encoding.BECH32M.constantValue) {
                return Encoding.BECH32M;
            } else {
                return null;
            }
        }

        final int getConstantValue() {
            return this.constantValue;
        }
    }

    @Getter
    @AllArgsConstructor(access = AccessLevel.PRIVATE)
    public static class Bech32Data {
        private final Encoding encoding;
        private final String hrp;
        private final byte[] data;
    }

    private static int polymod(final byte[] values) {
        int c = 1;
        for (final byte v : values) {
            int c0 = (c >>> 25) & 0xff;
            c = ((c & 0x1ffffff) << 5) ^ (v & 0xff);
            if ((c0 & 1) != 0) c ^= 0x3b6a57b2;
            if ((c0 & 2) != 0) c ^= 0x26508e6d;
            if ((c0 & 4) != 0) c ^= 0x1ea119fa;
            if ((c0 & 8) != 0) c ^= 0x3d4233dd;
            if ((c0 & 16) != 0) c ^= 0x2a1462b3;
        }
        return c;
    }

    private static byte[] convertBase(final byte[] input, final int fromBase, final int toBase, final boolean pad) {
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        int acc = 0;
        int bits = 0;
        final int maxValue = (1 << toBase) - 1;
        final int maxAccumulatorValue = (1 << (fromBase + toBase - 1)) - 1;
        for (final byte val : input) {
            acc = ((acc << fromBase) | (val & 0xff)) & maxAccumulatorValue;
            bits += fromBase;
            while (bits >= toBase) {
                bits -= toBase;
                out.write((acc >>> bits) & maxValue);
            }
        }
        if (pad) {
            if (bits > 0) {
                out.write((acc << (toBase - bits)) & maxValue);
            }
        } else if (bits >= fromBase || ((acc << (toBase - bits)) & maxValue) > 0) {
            throw new IllegalArgumentException("Invalid input to bech32 coding.");
        }
        return out.toByteArray();
    }

    private static byte[] expandHrp(final String hrp) {
        final byte[] out = new byte[hrp.length() * 2 + 1];
        IntStream.range(0, hrp.length()).parallel().forEach(
                index -> {
                    final int c = hrp.charAt(index);
                    out[index] = (byte) (c >>> 5);
                    out[index + hrp.length() + 1] = (byte) (c & 0x1f);
                }
        );
        return out;
    }

    private static Encoding verifyChecksum(final String hrp, final byte[] values) {
        final byte[] hrpExpanded = expandHrp(hrp);
        final byte[] combined = new byte[hrpExpanded.length + values.length];
        System.arraycopy(hrpExpanded, 0, combined, 0, hrpExpanded.length);
        System.arraycopy(values, 0, combined, hrpExpanded.length, values.length);
        final int check = polymod(combined);
        return Encoding.fromConstantValue(check);
    }

    private static byte[] createChecksum(final Encoding encoding, final String hrp, final byte[] values) {
        final byte[] hrpExpanded = expandHrp(hrp);
        final byte[] enc = new byte[hrpExpanded.length + values.length + CHECKSUM_SIZE];
        System.arraycopy(hrpExpanded, 0, enc, 0, hrpExpanded.length);
        System.arraycopy(values, 0, enc, hrpExpanded.length, values.length);
        final int mod = polymod(enc) ^ encoding.getConstantValue();
        final byte[] out = new byte[CHECKSUM_SIZE];
        IntStream.range(0, CHECKSUM_SIZE).parallel().forEach(
                index -> out[index] = (byte) ((mod >>> (5 * (5 - index))) & 0x1f)
        );
        return out;
    }

    public static String encode(final String hrp, final byte[] values) {
        return encode(Encoding.BECH32, hrp, values);
    }

    public static String encode(final Encoding encoding, final String hrp, final byte[] values) {
        final String hrpSanitized = hrp.toLowerCase(Locale.ROOT).trim();
        final byte[] valuesBase5 = convertBase(values, 8, 5, true);
        final byte[] checksum = createChecksum(encoding, hrpSanitized, valuesBase5);
        final byte[] combined = new byte[valuesBase5.length + checksum.length];
        System.arraycopy(valuesBase5, 0, combined, 0, valuesBase5.length);
        System.arraycopy(checksum, 0, combined, valuesBase5.length, checksum.length);
        final StringBuilder stringBuilder = new StringBuilder(hrpSanitized.length() + 1 + combined.length);
        stringBuilder.append(hrpSanitized);
        stringBuilder.append(SEPARATOR);
        for (final byte b : combined) {
            stringBuilder.append(CHARSET.charAt(b));
        }
        return stringBuilder.toString();
    }

    public static Bech32Data decode(final String bech32String) throws IllegalArgumentException {
        final String bech32StringSanitized = bech32String.toLowerCase(Locale.ROOT).trim();
        bech32StringSanitized.chars().parallel().forEach(
                character -> {
                    if (character < 33 || character > 126) {
                        throw new IllegalArgumentException(String.format("Illegal character %s detected.", (char) character));
                    }
                }
        );

        final int pos = bech32StringSanitized.lastIndexOf(SEPARATOR);
        if (pos < 1) {
            throw new IllegalArgumentException("Missing human readable part");
        }
        final int dataPartLength = bech32StringSanitized.length() - 1 - pos;
        if (dataPartLength < CHECKSUM_SIZE) {
            throw new IllegalArgumentException(String.format("Data part too short. Expected minimum length %d but got %d.", CHECKSUM_SIZE, dataPartLength));
        }
        final byte[] values = new byte[dataPartLength];
        IntStream.range(pos + 1, dataPartLength + pos + 1).forEach(
                index -> {
                    final char c = bech32StringSanitized.charAt(index);
                    if (CHARSET_REV[c] == -1) {
                        throw new IllegalArgumentException("Invalid character in bech32 string.");
                    }
                    values[index - pos - 1] = CHARSET_REV[c];
                }
        );
        final String hrp = bech32StringSanitized.substring(0, pos);
        final Encoding encoding = verifyChecksum(hrp, values);
        if (encoding == null) {
            throw new IllegalArgumentException("Checksum invalid.");
        }
        return new Bech32Data(encoding, hrp, convertBase(Arrays.copyOfRange(values, 0, values.length - CHECKSUM_SIZE), 5, 8, false));
    }
}
