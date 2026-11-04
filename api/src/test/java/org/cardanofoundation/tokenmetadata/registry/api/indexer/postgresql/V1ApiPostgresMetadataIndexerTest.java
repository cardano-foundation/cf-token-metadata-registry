package org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql;

import co.nstant.in.cbor.CborException;
import co.nstant.in.cbor.model.DataItem;
import co.nstant.in.cbor.model.UnicodeString;
import co.nstant.in.cbor.model.UnsignedInteger;
import com.bloxbean.cardano.client.common.cbor.CborSerializationUtil;
import com.bloxbean.cardano.client.crypto.Blake2bUtil;
import com.bloxbean.cardano.client.crypto.config.CryptoConfiguration;
import com.bloxbean.cardano.client.util.HexUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.cardanofoundation.tokenmetadata.registry.api.indexer.postgresql.data.MetadataQueryResult;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.AnnotatedSignature;
import org.cardanofoundation.tokenmetadata.registry.api.model.rest.TokenMetadata;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class V1ApiPostgresMetadataIndexerTest {

    /** Mainnet: a registry file named and written in upper case, stored under its lower-case subject. */
    private static final String STORED_SUBJECT = "0b9bedfbfb62b21a2a41f581c2811ee74ee062ce32b0eca43964cf796d6174726978";
    private static final String FILE_SUBJECT = "0b9bedfbfb62b21a2a41f581c2811ee74ee062ce32b0eca43964cf796D6174726978";
    /** Its real signature of "name", made over the subject as the file writes it. */
    private static final String NAME_SIGNATURE = "2553dbe2a6006f23aa45c6ea4e5e631587aff5439dd7beb1a3b68095435444eae7c49926f1c2"
            + "31844348d0639ffd60d6881d8fa82a17e128038de788162e970a";
    private static final String NAME_PUBLIC_KEY = "b64d5cf236b7aa273794e8b97a1f677777c24d71e56542913108a2a5f3b5f6af";

    private final NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
    private final V1ApiPostgresMetadataIndexer indexer = new V1ApiPostgresMetadataIndexer(new ObjectMapper(), jdbcTemplate);

    @SuppressWarnings("unchecked")
    private void storedEntry(String properties) {
        MetadataQueryResult row = new MetadataQueryResult();
        row.setSubject(STORED_SUBJECT);
        row.setProperties(properties);
        when(jdbcTemplate.query(anyString(), any(SqlParameterSource.class), any(RowMapper.class))).thenReturn(List.of(row));
    }

    private void storedEntry() {
        storedEntry("{\"subject\": \"" + FILE_SUBJECT + "\", \"name\": {\"value\": \"matrix\", \"sequenceNumber\": 0,"
                + " \"signatures\": [{\"signature\": \"" + NAME_SIGNATURE + "\", \"publicKey\": \"" + NAME_PUBLIC_KEY + "\"}]}}");
    }

    @Nested
    class ServedSubject {

        @Test
        void isTheOneWrittenInTheRegistryFileSoThatItsSignaturesVerify() {
            storedEntry();

            assertThat(indexer.findSubject(STORED_SUBJECT)).hasValueSatisfying(metadata -> {
                assertThat(metadata.getSubject()).isEqualTo(FILE_SUBJECT);
                assertThat(metadata.getName().getValue()).isEqualTo("matrix");
                assertThat(nameSignatureVerifies(metadata.getSubject(), metadata)).isTrue();
                assertThat(nameSignatureVerifies(STORED_SUBJECT, metadata)).isFalse();
            });
        }

        @Test
        void isTheOneWrittenInTheRegistryFileInABatchQueryTooWhichIsKeyedByTheStoredSubject() {
            storedEntry();

            Map<String, TokenMetadata> found = indexer.findSubjectsSelectProperties(List.of(STORED_SUBJECT), List.of());

            assertThat(found).containsOnlyKeys(STORED_SUBJECT);
            assertThat(found.get(STORED_SUBJECT).getSubject()).isEqualTo(FILE_SUBJECT);
        }

        @Test
        void isTheStoredSubjectWhenTheStoredFileHasNone() {
            storedEntry("{\"name\": {\"value\": \"matrix\", \"sequenceNumber\": 0}}");

            assertThat(indexer.findSubject(STORED_SUBJECT))
                    .hasValueSatisfying(metadata -> assertThat(metadata.getSubject()).isEqualTo(STORED_SUBJECT));
        }
    }

    /**
     * CIP-26: the signature is over blake2b-256 of the blake2b-256 hashes of the CBOR of the subject, the property name,
     * the value and the sequence number, concatenated.
     */
    private static boolean nameSignatureVerifies(String subject, TokenMetadata metadata) {
        ByteArrayOutputStream hashes = new ByteArrayOutputStream();
        hashes.writeBytes(cborHash(new UnicodeString(subject)));
        hashes.writeBytes(cborHash(new UnicodeString("name")));
        hashes.writeBytes(cborHash(new UnicodeString(metadata.getName().getValue())));
        hashes.writeBytes(cborHash(new UnsignedInteger(metadata.getName().getSequenceNumber().longValueExact())));
        byte[] message = Blake2bUtil.blake2bHash256(hashes.toByteArray());
        AnnotatedSignature signature = metadata.getName().getSignatures().getFirst();
        return CryptoConfiguration.INSTANCE.getSigningProvider().verify(HexUtil.decodeHexString(signature.getSignature()),
                message, HexUtil.decodeHexString(signature.getPublicKey()));
    }

    private static byte[] cborHash(DataItem item) {
        try {
            return Blake2bUtil.blake2bHash256(CborSerializationUtil.serialize(item));
        } catch (CborException e) {
            throw new IllegalStateException(e);
        }
    }
}
