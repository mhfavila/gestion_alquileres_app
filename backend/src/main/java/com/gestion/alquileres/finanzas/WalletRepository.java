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
     * Obtiene en una única consulta SQL agregada (sin N+1) todas las wallets del contexto
     * junto con su saldo histórico real (SUM de ingresos menos gastos).
     */
    @Query("""
            SELECT new com.gestion.alquileres.finanzas.Wallet(
                w.id,
                w.nombre,
                w.contexto,
                COALESCE(SUM(CASE WHEN t.tipo = com.gestion.alquileres.comun.TipoTransaccion.INGRESO THEN t.importe ELSE -t.importe END), 0)
            )
            FROM Wallet w
            LEFT JOIN Transaccion t ON t.wallet.id = w.id AND t.contexto = w.contexto
            WHERE w.contexto = :contexto
            GROUP BY w.id, w.nombre, w.contexto
            ORDER BY w.nombre ASC
            """)
    List<Wallet> findByContextoConSaldoOrderByNombreAsc(@Param("contexto") Contexto contexto);

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
