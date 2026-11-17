package com.gestion.alquileres.seguridad;

import com.gestion.alquileres.comun.Contexto;

public final class ContextoHolder {

    public static final String HEADER = "X-Context-Id";
    public static final String ATRIBUTO = "contexto";

    private static final ThreadLocal<Contexto> ACTUAL = new ThreadLocal<>();

    private ContextoHolder() {
    }

    public static void set(Contexto contexto) {
        ACTUAL.set(contexto);
    }

    public static Contexto get() {
        return ACTUAL.get();
    }

    public static void clear() {
        ACTUAL.remove();
    }
}
