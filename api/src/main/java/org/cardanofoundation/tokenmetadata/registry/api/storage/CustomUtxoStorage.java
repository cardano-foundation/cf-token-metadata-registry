package org.cardanofoundation.tokenmetadata.registry.api.storage;

import com.bloxbean.cardano.yaci.store.common.domain.AddressUtxo;
import com.bloxbean.cardano.yaci.store.common.domain.TxInput;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoCache;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.UtxoStorageImpl;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.model.UtxoId;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.TxInputRepository;
import com.bloxbean.cardano.yaci.store.utxo.storage.impl.repository.UtxoRepository;
import lombok.extern.slf4j.Slf4j;
import org.cardanofoundation.tokenmetadata.registry.api.service.Cip68FungibleTokenService;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;

@Repository
@Slf4j
public class CustomUtxoStorage extends UtxoStorageImpl {

    private final UtxoRepository utxoRepository;

    private final Cip68FungibleTokenService cip68FungibleTokenService;

    public CustomUtxoStorage(UtxoRepository utxoRepository,
                             TxInputRepository spentOutputRepository,
                             DSLContext dsl,
                             UtxoCache utxoCache,
                             PlatformTransactionManager platformTransactionManager,
                             Cip68FungibleTokenService cip68FungibleTokenService) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache, platformTransactionManager);
        this.utxoRepository = utxoRepository;
        this.cip68FungibleTokenService = cip68FungibleTokenService;
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        var automaticPaymentsUtxos = addressUtxoList
                .stream()
                .filter(cip68FungibleTokenService::containsReferenceNft)
                .toList();

        super.saveUnspent(automaticPaymentsUtxos);
    }

    @Override
    public void saveSpent(List<TxInput> txInputs) {
        var automaticPaymentInputs = txInputs
                .stream()
                .filter(txInput -> utxoRepository.findById(new UtxoId(txInput.getTxHash(), txInput.getOutputIndex())).isPresent())
                .toList();
        super.saveSpent(automaticPaymentInputs);
    }

}
