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
import org.cardanofoundation.tokenmetadata.registry.api.service.cip113.Cip113RegistryService;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Repository
@Slf4j
public class CustomUtxoStorage extends UtxoStorageImpl {

    private final UtxoRepository utxoRepository;
    private final Cip68FungibleTokenService cip68FungibleTokenService;
    private final Cip113RegistryService cip113RegistryService;

    public CustomUtxoStorage(UtxoRepository utxoRepository,
                             TxInputRepository spentOutputRepository,
                             DSLContext dsl,
                             UtxoCache utxoCache,
                             PlatformTransactionManager platformTransactionManager,
                             Cip68FungibleTokenService cip68FungibleTokenService,
                             Cip113RegistryService cip113RegistryService) {
        super(utxoRepository, spentOutputRepository, dsl, utxoCache, platformTransactionManager);
        this.utxoRepository = utxoRepository;
        this.cip68FungibleTokenService = cip68FungibleTokenService;
        this.cip113RegistryService = cip113RegistryService;
    }

    @Override
    public void saveUnspent(List<AddressUtxo> addressUtxoList) {
        List<AddressUtxo> automaticPaymentsUtxos = addressUtxoList
                .stream()
                .filter(utxo -> cip68FungibleTokenService.containsReferenceNft(utxo)
                        || cip113RegistryService.containsRegistryNode(utxo))
                .toList();

        super.saveUnspent(automaticPaymentsUtxos);
    }

    @Override
    public void saveSpent(List<TxInput> txInputs) {
        List<UtxoId> utxoIds = txInputs.stream()
                .map(txInput -> new UtxoId(txInput.getTxHash(), txInput.getOutputIndex()))
                .toList();
        Set<UtxoId> existingIds = utxoRepository.findAllById(utxoIds).stream()
                .map(entity -> new UtxoId(entity.getTxHash(), entity.getOutputIndex()))
                .collect(Collectors.toSet());

        List<TxInput> automaticPaymentInputs = txInputs.stream()
                .filter(txInput -> existingIds.contains(new UtxoId(txInput.getTxHash(), txInput.getOutputIndex())))
                .toList();
        super.saveSpent(automaticPaymentInputs);
    }

}
