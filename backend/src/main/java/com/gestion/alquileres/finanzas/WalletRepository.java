package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.comun.Contexto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {

    List<Wallet> findByContextoOrderByNombreAsc(Contexto contexto);

    /**
     * Balance del wallet dentro de su contexto: suma ingresos y resta gastos.
     * Vacío si el wallet no existe o pertenece a otro contexto.
     */
    default Optional<BigDecimal> calcularBalance(Long walletId, Contexto contexto) {
        return calcularBalanceEnContexto(walletId, contexto.name());
    }

    @Query(value = """
            SELECT COALESCE(SUM(
                       CASE WHEN t.tipo = 'INGRESO' THEN t.importe ELSE -t.importe END
                   ), 0)
            FROM wallets w
            LEFT JOIN transacciones t ON t.wallet_id = w.id
            WHERE w.id = :walletId
              AND w.contexto = :contexto
            GROUP BY w.id
            """, nativeQuery = true)
    Optional<BigDecimal> calcularBalanceEnContexto(@Param("walletId") Long walletId,
                                                    @Param("contexto") String contexto);
}
