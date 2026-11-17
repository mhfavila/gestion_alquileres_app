package com.gestion.alquileres.finanzas;

import com.gestion.alquileres.seguridad.ContextoHolder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TransaccionService {

    private final TransaccionRepository repository;

    public TransaccionService(TransaccionRepository repository) {
        this.repository = repository;
    }

    public List<Transaccion> obtenerPorContexto() {
        // Recuperamos el contexto actual de la petición
        return repository.findByContextoOrderByFechaDesc(ContextoHolder.get());
    }

    public Transaccion guardar(Transaccion transaccion) {
        // FORZAMOS el contexto. Nunca confiamos en lo que mande el cliente.
        transaccion.setContexto(ContextoHolder.get());
        return repository.save(transaccion);
    }
}
