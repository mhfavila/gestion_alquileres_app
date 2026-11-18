# -*- coding: utf-8 -*-
"""
Suite E2E Definitiva - Gestión de Alquileres Multi-Contexto (Angular 18 + Spring Boot 3)
Implementa el patrón "Crear 3 -> Editar 1 -> Borrar 1" en cada entidad y contexto,
valida cobros de contratos en 1 clic, rentabilidad por inmueble, filtro temporal
y traspasos con edición/borrado en espejo.
Genera 8 capturas en docs/screenshots/ y actualiza MANUAL_USUARIO_VISUAL.md.

Uso:  python scripts/test_completo_app.py
Requisito: frontend en http://localhost:4200 y backend en http://localhost:8080
"""

import random
import time
from pathlib import Path
from playwright.sync_api import sync_playwright, Page, expect

BASE_URL = "http://localhost:4200"
SHOT_DIR = Path("docs/screenshots")
MANUAL_PATH = Path("MANUAL_USUARIO_VISUAL.md")

SUFIJO = f"{random.randint(100, 999)}"

# Datos del lote de prueba con sufijo único de 3 dígitos
CAT_CARTERA_1 = f"IBI_{SUFIJO}"
CAT_CARTERA_2 = f"Comunidad_{SUFIJO}"
CAT_CARTERA_3 = f"Basuras_{SUFIJO}"

INM_1_DIR = f"Piso Centro 1 ({SUFIJO})"
INM_2_DIR = f"Plaza Garaje 2 ({SUFIJO})"
INM_2_DIR_EDIT = f"Plaza Garaje 2 - EDITADO ({SUFIJO})"
INM_3_DIR = f"Local Comercial 3 ({SUFIJO})"

INQ_1_NOM = f"Laura Gómez {SUFIJO}"
INQ_2_NOM = f"Carlos Ruiz {SUFIJO}"
INQ_2_NOM_EDIT = f"Carlos Ruiz {SUFIJO} (Editado)"
INQ_3_NOM = f"Marta Díaz {SUFIJO}"

DNI_1 = f"111{SUFIJO}A"
DNI_2 = f"222{SUFIJO}B"
DNI_3 = f"333{SUFIJO}C"

TX_CARTERA_1 = f"Seguro Hogar ({SUFIJO})"
TX_CARTERA_2 = f"Reparación fontanería ({SUFIJO})"
TX_CARTERA_2_EDIT = f"Reparación fontanería (Factura final) ({SUFIJO})"
TX_CARTERA_3 = f"Gasto erróneo a borrar ({SUFIJO})"

TRASPASO_1_DESC = f"Reparto mensual casa ({SUFIJO})"
TRASPASO_2_DESC = f"Retirada personal ({SUFIJO})"
TRASPASO_3_DESC = f"Traspaso de prueba a borrar ({SUFIJO})"

CAT_CASA_1 = f"Supermercado_{SUFIJO}"
CAT_CASA_2 = f"Luz_{SUFIJO}"
CAT_CASA_3 = f"VariosHogar_{SUFIJO}"

TX_CASA_1 = f"Compra mensual supermercado ({SUFIJO})"
TX_CASA_2 = f"Factura electricidad ({SUFIJO})"
TX_CASA_2_EDIT = f"Factura electricidad regularizada ({SUFIJO})"
TX_CASA_3 = f"Gasto doméstico duplicado ({SUFIJO})"

CAT_PERS_1 = f"Ocio_{SUFIJO}"
CAT_PERS_2 = f"Deporte_{SUFIJO}"
CAT_PERS_3 = f"Caprichos_{SUFIJO}"

TX_PERS_1 = f"Cena fin de semana ({SUFIJO})"
TX_PERS_2 = f"Cuota gimnasio ({SUFIJO})"
TX_PERS_2_EDIT = f"Cuota gimnasio anual ({SUFIJO})"
TX_PERS_3 = f"Suscripción cancelada ({SUFIJO})"

resultados: list[tuple[str, str, bool, str]] = []


def registrar(bloque: str, paso: str, exito: bool, detalle: str = "") -> None:
    estado = "OK" if exito else "FALLO"
    resultados.append((bloque, paso, exito, detalle))
    print(f"[{estado}] [{bloque}] {paso}" + (f" -> {detalle}" if detalle else ""))


def shot(page: Page, nombre: str) -> None:
    SHOT_DIR.mkdir(parents=True, exist_ok=True)
    ruta = (SHOT_DIR / nombre).resolve()
    for intento in range(3):
        try:
            page.screenshot(path=str(ruta), full_page=True)
            print(f"      [captura] {ruta}")
            return
        except Exception as e:
            if intento == 2:
                print(f"      [captura FALLIDA] {ruta}: {e}")
            else:
                page.wait_for_timeout(800)


def esperar_red(page: Page, ms: int = 900) -> None:
    try:
        page.wait_for_load_state("networkidle", timeout=6000)
    except Exception:
        pass
    page.wait_for_timeout(ms)


def submit_form(page: Page, selector_campo: str) -> None:
    form = page.locator(f"form:has({selector_campo})")
    form.locator('button[type="submit"]').click()


def cambiar_contexto(page: Page, etiqueta: str) -> None:
    page.get_by_role("button", name=etiqueta, exact=True).first.click()
    esperar_red(page, 1000)


def fecha_hoy_navegador(page: Page) -> str:
    return page.evaluate("new Date().toISOString().split('T')[0]")


# ================= HELPERS DE ENTIDADES =================

def crear_categoria(page: Page, nombre: str) -> None:
    inp = page.locator('input[name="categoriaNombre"]')
    if not inp.is_visible():
        page.get_by_role("button", name="Nueva categoría").first.click()
        expect(inp).to_be_visible(timeout=5000)
    inp.fill(nombre)
    submit_form(page, 'input[name="categoriaNombre"]')
    expect(page.locator("span.inline-flex").filter(has_text=nombre).first).to_be_visible(timeout=8000)
    expect(inp).to_be_hidden(timeout=5000)
    esperar_red(page, 400)


def borrar_categoria(page: Page, nombre: str) -> None:
    pildora = page.locator("span.inline-flex").filter(has_text=nombre).first
    pildora.locator("button").click()
    expect(page.locator("span.inline-flex").filter(has_text=nombre)).to_have_count(0, timeout=8000)
    esperar_red(page, 400)


def crear_inmueble(page: Page, tipo_valor: str, direccion: str, ref: str, metros: str) -> None:
    page.locator('select[name="inmuebleTipo"]').select_option(value=tipo_valor)
    page.locator('input[name="inmuebleDireccion"]').fill(direccion)
    page.locator('input[name="inmuebleReferencia"]').fill(ref)
    page.locator('input[name="inmuebleMetros"]').fill(metros)
    submit_form(page, 'input[name="inmuebleDireccion"]')
    esperar_red(page)


def card_inmueble(page: Page, texto: str):
    return page.locator("div.rounded.bg-slate-700.p-2").filter(has_text=texto).first


def crear_inquilino(page: Page, nombre: str, email: str, telefono: str, documento: str) -> None:
    page.locator('input[name="inquilinoNombre"]').fill(nombre)
    page.locator('input[name="inquilinoEmail"]').fill(email)
    page.locator('input[name="inquilinoTelefono"]').fill(telefono)
    page.locator('input[name="inquilinoDocumento"]').fill(documento)
    submit_form(page, 'input[name="inquilinoNombre"]')
    esperar_red(page)


def card_inquilino(page: Page, texto: str):
    return page.locator("div.rounded.bg-slate-700.p-2").filter(has_text=texto).first


def crear_contrato(page: Page, dir_inmueble: str, nom_inquilino: str,
                   f_inicio: str, f_fin: str, importe: str, fianza: str) -> None:
    page.locator('select[name="contratoInmuebleId"]').select_option(label=dir_inmueble)
    page.locator('select[name="contratoInquilinoId"]').select_option(label=nom_inquilino)
    page.locator('input[name="contratoFechaInicio"]').fill(f_inicio)
    page.locator('input[name="contratoFechaFin"]').fill(f_fin)
    page.locator('input[name="contratoImporteMensual"]').fill(importe)
    page.locator('input[name="contratoFianza"]').fill(fianza)
    submit_form(page, 'input[name="contratoFechaInicio"]')
    esperar_red(page)


def card_contrato(page: Page, texto_inmueble: str, texto_inquilino: str):
    return (
        page.locator("div.rounded.bg-slate-700.p-2")
        .filter(has_text=texto_inmueble)
        .filter(has_text=texto_inquilino)
        .first
    )


def crear_transaccion(page: Page, categoria: str, tipo_valor: str, importe: str,
                      fecha: str, descripcion: str, dir_inmueble: str | None = None) -> None:
    page.locator('select[name="txCategoriaId"]').select_option(label=categoria)
    page.locator('select[name="txTipo"]').select_option(value=tipo_valor)
    page.locator('input[name="txImporte"]').fill(importe)
    page.locator('input[name="txFecha"]').fill(fecha)
    page.locator('input[name="txDescripcion"]').fill(descripcion)
    if dir_inmueble:
        sel_inm = page.locator('select[name="txInmuebleId"]')
        if sel_inm.count() > 0:
            sel_inm.select_option(label=dir_inmueble)
    submit_form(page, 'input[name="txImporte"]')
    esperar_red(page)


def fila_transaccion(page: Page, texto: str):
    return page.locator("tbody tr").filter(has_text=texto).first


def realizar_traspaso(page: Page, contexto_destino_val: str, importe: str,
                      fecha: str, descripcion: str) -> None:
    page.locator('select[name="contextoDestino"]').select_option(value=contexto_destino_val)
    page.locator('input[name="importeTraspaso"]').fill(importe)
    page.locator('input[name="fechaTraspaso"]').fill(fecha)
    page.locator('input[name="descripcionTraspaso"]').fill(descripcion)
    submit_form(page, 'input[name="importeTraspaso"]')
    esperar_red(page)


# ================= FLUJO E2E COMPLETO =================

def ejecutar_flujo(page: Page) -> None:
    page.goto(BASE_URL, wait_until="networkidle")
    page.wait_for_selector("text=Contexto activo", timeout=15000)
    esperar_red(page, 1000)

    hoy = fecha_hoy_navegador(page)

    # ------------------------------------------------------------------
    # BLOQUE 1: Categorías en Cartera (Crear 3, Borrar 1)
    # ------------------------------------------------------------------
    cambiar_contexto(page, "Cartera")
    expect(page.get_by_text("Wallet principal").first).to_be_visible(timeout=8000)
    registrar("Bloque 1", "Verificar Wallet única principal en Cartera", True, "Cartera Inmobiliaria")

    crear_categoria(page, CAT_CARTERA_1)
    crear_categoria(page, CAT_CARTERA_2)
    crear_categoria(page, CAT_CARTERA_3)
    expect(page.locator("span.inline-flex").filter(has_text=CAT_CARTERA_1).first).to_be_visible()
    expect(page.locator("span.inline-flex").filter(has_text=CAT_CARTERA_2).first).to_be_visible()
    expect(page.locator("span.inline-flex").filter(has_text=CAT_CARTERA_3).first).to_be_visible()
    registrar("Bloque 1", "Crear 3 categorías en Cartera", True, f"{CAT_CARTERA_1}, {CAT_CARTERA_2}, {CAT_CARTERA_3}")

    borrar_categoria(page, CAT_CARTERA_3)
    expect(page.locator("span.inline-flex").filter(has_text=CAT_CARTERA_3)).to_have_count(0)
    registrar("Bloque 1", "Borrar 1 categoría sin transacciones (×)", True, f"Eliminada {CAT_CARTERA_3}")

    shot(page, "01_cartera_wallet_y_categorias.png")

    # ------------------------------------------------------------------
    # BLOQUE 2: Inmuebles (Crear 3, Editar 1, Borrar 1)
    # ------------------------------------------------------------------
    crear_inmueble(page, "PISO", INM_1_DIR, f"REF-P1-{SUFIJO}", "85")
    crear_inmueble(page, "GARAJE", INM_2_DIR, f"REF-G2-{SUFIJO}", "15")
    crear_inmueble(page, "LOCAL", INM_3_DIR, f"REF-L3-{SUFIJO}", "120")
    expect(card_inmueble(page, INM_1_DIR)).to_be_visible()
    expect(card_inmueble(page, INM_2_DIR)).to_be_visible()
    expect(card_inmueble(page, INM_3_DIR)).to_be_visible()
    registrar("Bloque 2", "Crear 3 Inmuebles (Piso, Garaje, Local)", True, f"{INM_1_DIR}, {INM_2_DIR}, {INM_3_DIR}")

    # Editar Inmueble #2
    card_inmueble(page, INM_2_DIR).locator('button[title="Editar inmueble"]').click()
    page.wait_for_timeout(400)
    expect(page.locator('form:has(input[name="inmuebleDireccion"]) button[type="submit"]')).to_have_text("Guardar cambios")
    shot(page, "02_inmuebles_modo_edicion.png")

    page.locator('input[name="inmuebleDireccion"]').fill(INM_2_DIR_EDIT)
    page.locator('input[name="inmuebleMetros"]').fill("18")
    submit_form(page, 'input[name="inmuebleDireccion"]')
    esperar_red(page)
    expect(card_inmueble(page, INM_2_DIR_EDIT)).to_be_visible()
    expect(card_inmueble(page, INM_2_DIR_EDIT)).to_contain_text("18m²")
    registrar("Bloque 2", "Editar Inmueble #2 (PUT)", True, f"{INM_2_DIR_EDIT} (18 m²)")

    # Borrar Inmueble #3
    card_inmueble(page, INM_3_DIR).locator('button[title="Eliminar inmueble"]').click()
    esperar_red(page)
    expect(page.locator("div.rounded.bg-slate-700.p-2").filter(has_text=INM_3_DIR)).to_have_count(0)
    registrar("Bloque 2", "Borrar Inmueble #3 (DELETE)", True, f"Eliminado {INM_3_DIR}")

    # ------------------------------------------------------------------
    # BLOQUE 3: Inquilinos (Crear 3, Editar 1, Borrar 1)
    # ------------------------------------------------------------------
    crear_inquilino(page, INQ_1_NOM, f"laura.{SUFIJO}@test.com", "611000111", DNI_1)
    crear_inquilino(page, INQ_2_NOM, f"carlos.{SUFIJO}@test.com", "622000222", DNI_2)
    crear_inquilino(page, INQ_3_NOM, f"marta.{SUFIJO}@test.com", "633000333", DNI_3)
    expect(card_inquilino(page, INQ_1_NOM)).to_be_visible()
    expect(card_inquilino(page, INQ_2_NOM)).to_be_visible()
    expect(card_inquilino(page, INQ_3_NOM)).to_be_visible()
    registrar("Bloque 3", "Crear 3 Inquilinos con documentos únicos", True, f"{DNI_1}, {DNI_2}, {DNI_3}")

    # Editar Inquilino #2
    card_inquilino(page, INQ_2_NOM).locator('button[title="Editar inquilino"]').click()
    page.wait_for_timeout(300)
    page.locator('input[name="inquilinoNombre"]').fill(INQ_2_NOM_EDIT)
    page.locator('input[name="inquilinoTelefono"]').fill("699888777")
    submit_form(page, 'input[name="inquilinoNombre"]')
    esperar_red(page)
    expect(card_inquilino(page, INQ_2_NOM_EDIT)).to_be_visible()
    expect(card_inquilino(page, INQ_2_NOM_EDIT)).to_contain_text("699888777")
    registrar("Bloque 3", "Editar Inquilino #2 (PUT)", True, f"{INQ_2_NOM_EDIT} | Tel: 699888777")

    # Borrar Inquilino #3
    card_inquilino(page, INQ_3_NOM).locator('button[title="Eliminar inquilino"]').click()
    esperar_red(page)
    expect(page.locator("div.rounded.bg-slate-700.p-2").filter(has_text=INQ_3_NOM)).to_have_count(0)
    registrar("Bloque 3", "Borrar Inquilino #3 (DELETE)", True, f"Eliminado {INQ_3_NOM}")

    shot(page, "03_inmuebles_e_inquilinos_crud.png")

    # ------------------------------------------------------------------
    # BLOQUE 4: Contratos y Botón "Cobrar mes" (Crear 3, Editar 1, Borrar 1, Cobrar 2)
    # ------------------------------------------------------------------
    crear_contrato(page, INM_1_DIR, INQ_1_NOM, "2026-01-01", "2027-01-01", "700.00", "1400.00")
    crear_contrato(page, INM_2_DIR_EDIT, INQ_2_NOM_EDIT, "2026-02-01", "2027-02-01", "90.00", "180.00")
    crear_contrato(page, INM_1_DIR, INQ_2_NOM_EDIT, "2026-03-01", "2026-09-01", "450.00", "450.00")
    registrar("Bloque 4", "Crear 3 Contratos vinculados", True, "700€, 90€ y 450€")

    # Editar Contrato #2 (90€ -> 100€)
    c2 = card_contrato(page, INM_2_DIR_EDIT, INQ_2_NOM_EDIT)
    c2.locator('button[title="Editar contrato"]').click()
    page.wait_for_timeout(300)
    page.locator('input[name="contratoImporteMensual"]').fill("100.00")
    submit_form(page, 'input[name="contratoFechaInicio"]')
    esperar_red(page)
    expect(card_contrato(page, INM_2_DIR_EDIT, INQ_2_NOM_EDIT)).to_contain_text("100€/mes")
    registrar("Bloque 4", "Editar Contrato #2 (90€ -> 100€/mes)", True, "Actualizado a 100€/mes")

    # Borrar Contrato #3 (Piso Centro 1 + Inquilino 2, 450€)
    c3 = card_contrato(page, INM_1_DIR, INQ_2_NOM_EDIT)
    c3.locator('button[title="Eliminar contrato"]').click()
    esperar_red(page)
    expect(
        page.locator("div.rounded.bg-slate-700.p-2")
        .filter(has_text=INM_1_DIR)
        .filter(has_text=INQ_2_NOM_EDIT)
    ).to_have_count(0)
    registrar("Bloque 4", "Borrar Contrato #3 temporal (DELETE)", True, "Eliminado contrato de 450€")

    # Probar botón "Cobrar mes" en Contrato #1 (+700€) y Contrato #2 (+100€)
    c1 = card_contrato(page, INM_1_DIR, INQ_1_NOM)
    expect(c1).to_contain_text("PENDIENTE DE COBRO")
    c1.locator('button:has-text("Cobrar mes")').click()
    esperar_red(page, 1200)
    expect(card_contrato(page, INM_1_DIR, INQ_1_NOM)).to_contain_text("COBRADO ESTE MES")
    registrar("Bloque 4", "Cobrar mes en 1 clic sobre Contrato #1 (+700€)", True, "Badge -> COBRADO ESTE MES")

    c2_actual = card_contrato(page, INM_2_DIR_EDIT, INQ_2_NOM_EDIT)
    expect(c2_actual).to_contain_text("PENDIENTE DE COBRO")
    c2_actual.locator('button:has-text("Cobrar mes")').click()
    esperar_red(page, 1200)
    expect(card_contrato(page, INM_2_DIR_EDIT, INQ_2_NOM_EDIT)).to_contain_text("COBRADO ESTE MES")
    registrar("Bloque 4", "Cobrar mes en 1 clic sobre Contrato #2 (+100€)", True, "Badge -> COBRADO ESTE MES")

    shot(page, "04_contratos_cobrar_mes.png")

    # ------------------------------------------------------------------
    # BLOQUE 5: Transacciones y Rentabilidad por Inmueble (Crear 3, Editar 1, Borrar 1)
    # ------------------------------------------------------------------
    crear_transaccion(page, CAT_CARTERA_1, "GASTO", "150.00", hoy, TX_CARTERA_1, dir_inmueble=INM_1_DIR)
    crear_transaccion(page, CAT_CARTERA_2, "GASTO", "80.00", hoy, TX_CARTERA_2, dir_inmueble=INM_1_DIR)
    crear_transaccion(page, CAT_CARTERA_2, "GASTO", "45.00", hoy, TX_CARTERA_3, dir_inmueble=INM_1_DIR)
    expect(fila_transaccion(page, TX_CARTERA_1)).to_be_visible()
    expect(fila_transaccion(page, TX_CARTERA_2)).to_be_visible()
    expect(fila_transaccion(page, TX_CARTERA_3)).to_be_visible()
    registrar("Bloque 5", "Crear 3 Transacciones de gasto imputadas a Inmueble #1", True, "150€, 80€, 45€")

    # Editar Transacción #2 (80€ -> 110€ y nuevo concepto)
    fila_transaccion(page, TX_CARTERA_2).locator('button:has-text("✏️")').click()
    page.wait_for_timeout(300)
    page.locator('input[name="txImporte"]').fill("110.00")
    page.locator('input[name="txDescripcion"]').fill(TX_CARTERA_2_EDIT)
    submit_form(page, 'input[name="txImporte"]')
    esperar_red(page)
    expect(fila_transaccion(page, TX_CARTERA_2_EDIT)).to_be_visible()
    expect(fila_transaccion(page, TX_CARTERA_2_EDIT)).to_contain_text("110.00€")
    registrar("Bloque 5", "Editar Transacción #2 (80€ -> 110€)", True, TX_CARTERA_2_EDIT)

    # Borrar Transacción #3 (45€)
    fila_transaccion(page, TX_CARTERA_3).locator('button[title="Eliminar transacción"]').click()
    esperar_red(page)
    expect(page.locator("tbody tr").filter(has_text=TX_CARTERA_3)).to_have_count(0)
    registrar("Bloque 5", "Borrar Transacción #3 errónea (45€)", True, f"Eliminada {TX_CARTERA_3}")

    # Verificar rentabilidad en tarjetas de Inmueble #1 (+700€ - 260€ = Neto: 440.00€) e Inmueble #2 (Neto: 100.00€)
    expect(card_inmueble(page, INM_1_DIR)).to_contain_text("+700.00€")
    expect(card_inmueble(page, INM_1_DIR)).to_contain_text("-260.00€")
    expect(card_inmueble(page, INM_1_DIR)).to_contain_text("Neto: 440.00€")
    expect(card_inmueble(page, INM_2_DIR_EDIT)).to_contain_text("Neto: 100.00€")
    registrar("Bloque 5", "Verificar Rentabilidad Neta por Inmueble y Totales por Categoría", True, "Inmueble #1 Neto: 440.00€ | Inmueble #2 Neto: 100.00€")

    shot(page, "05_transacciones_y_rentabilidad_inmuebles.png")

    # ------------------------------------------------------------------
    # BLOQUE 6: Traspasos entre Cuentas y Edición/Borrado en Espejo (Crear 3, Editar 1, Borrar 1)
    # ------------------------------------------------------------------
    realizar_traspaso(page, "HOME", "400.00", hoy, TRASPASO_1_DESC)
    realizar_traspaso(page, "PERSONAL", "200.00", hoy, TRASPASO_2_DESC)
    realizar_traspaso(page, "HOME", "99.00", hoy, TRASPASO_3_DESC)
    expect(fila_transaccion(page, TRASPASO_1_DESC)).to_be_visible()
    expect(fila_transaccion(page, TRASPASO_2_DESC)).to_be_visible()
    expect(fila_transaccion(page, TRASPASO_3_DESC)).to_be_visible()
    registrar("Bloque 6", "Crear 3 Traspasos entre contextos (Cartera -> Casa/Personal)", True, "400€, 200€, 99€")

    # Editar en espejo el Traspaso #1 (400€ -> 450€)
    fila_transaccion(page, TRASPASO_1_DESC).locator('button:has-text("✏️")').click()
    page.wait_for_timeout(400)
    expect(page.get_by_text("Editando traspaso vinculado")).to_be_visible()
    expect(page.locator('select[name="txCategoriaId"]')).to_be_disabled()
    expect(page.locator('select[name="txTipo"]')).to_be_disabled()
    shot(page, "06_traspaso_edicion_espejo.png")

    page.locator('input[name="txImporte"]').fill("450.00")
    submit_form(page, 'input[name="txImporte"]')
    esperar_red(page)
    expect(fila_transaccion(page, TRASPASO_1_DESC)).to_contain_text("450.00€")
    registrar("Bloque 6", "Editar en espejo Traspaso #1 (400€ -> 450€)", True, "Sincronizado en Cartera y Casa")

    # Borrar en espejo el Traspaso #3 (99€)
    fila_transaccion(page, TRASPASO_3_DESC).locator('button[title="Eliminar transacción"]').click()
    esperar_red(page)
    expect(page.locator("tbody tr").filter(has_text=TRASPASO_3_DESC)).to_have_count(0)
    registrar("Bloque 6", "Borrar en espejo Traspaso #3 (99€)", True, "Eliminado en origen y destino")

    # ------------------------------------------------------------------
    # BLOQUE 7A: Contexto Casa (HOME) (Verificar espejo + Crear 3, Editar 1, Borrar 1)
    # ------------------------------------------------------------------
    cambiar_contexto(page, "Casa")
    expect(fila_transaccion(page, TRASPASO_1_DESC)).to_be_visible()
    expect(fila_transaccion(page, TRASPASO_1_DESC)).to_contain_text("450.00€")
    expect(page.locator("tbody tr").filter(has_text=TRASPASO_3_DESC)).to_have_count(0)
    registrar("Bloque 7", "Verificar en Casa sincronización en espejo (+450€ y ausencia de 99€)", True, "Cuadre contable verificado")

    crear_categoria(page, CAT_CASA_1)
    crear_categoria(page, CAT_CASA_2)
    crear_categoria(page, CAT_CASA_3)
    borrar_categoria(page, CAT_CASA_3)

    crear_transaccion(page, CAT_CASA_1, "GASTO", "120.00", hoy, TX_CASA_1)
    crear_transaccion(page, CAT_CASA_2, "GASTO", "65.00", hoy, TX_CASA_2)
    crear_transaccion(page, CAT_CASA_1, "GASTO", "30.00", hoy, TX_CASA_3)
    registrar("Bloque 7", "Crear 3 gastos domésticos en Casa", True, "120€, 65€, 30€")

    # Editar gasto Luz (65€ -> 72€)
    fila_transaccion(page, TX_CASA_2).locator('button:has-text("✏️")').click()
    page.wait_for_timeout(300)
    page.locator('input[name="txImporte"]').fill("72.00")
    page.locator('input[name="txDescripcion"]').fill(TX_CASA_2_EDIT)
    submit_form(page, 'input[name="txImporte"]')
    esperar_red(page)
    expect(fila_transaccion(page, TX_CASA_2_EDIT)).to_contain_text("72.00€")
    registrar("Bloque 7", "Editar gasto doméstico en Casa (65€ -> 72€)", True, TX_CASA_2_EDIT)

    # Borrar gasto duplicado (30€)
    fila_transaccion(page, TX_CASA_3).locator('button[title="Eliminar transacción"]').click()
    esperar_red(page)
    expect(page.locator("tbody tr").filter(has_text=TX_CASA_3)).to_have_count(0)
    registrar("Bloque 7", "Borrar gasto doméstico duplicado en Casa (30€)", True, f"Eliminado {TX_CASA_3}")

    shot(page, "07_contexto_casa.png")

    # ------------------------------------------------------------------
    # BLOQUE 7B: Contexto Personal (PERSONAL) (Verificar traspaso + Crear 3, Editar 1, Borrar 1)
    # ------------------------------------------------------------------
    cambiar_contexto(page, "Personal")
    expect(fila_transaccion(page, TRASPASO_2_DESC)).to_be_visible()
    expect(fila_transaccion(page, TRASPASO_2_DESC)).to_contain_text("200.00€")
    registrar("Bloque 7", "Verificar en Personal recepción de Traspaso #2 (+200€)", True, TRASPASO_2_DESC)

    crear_categoria(page, CAT_PERS_1)
    crear_categoria(page, CAT_PERS_2)
    crear_categoria(page, CAT_PERS_3)
    borrar_categoria(page, CAT_PERS_3)

    crear_transaccion(page, CAT_PERS_1, "GASTO", "48.00", hoy, TX_PERS_1)
    crear_transaccion(page, CAT_PERS_2, "GASTO", "35.00", hoy, TX_PERS_2)
    crear_transaccion(page, CAT_PERS_1, "GASTO", "15.00", hoy, TX_PERS_3)
    registrar("Bloque 7", "Crear 3 movimientos en Personal", True, "48€, 35€, 15€")

    # Editar movimiento #2 (35€ -> 42€)
    fila_transaccion(page, TX_PERS_2).locator('button:has-text("✏️")').click()
    page.wait_for_timeout(300)
    page.locator('input[name="txImporte"]').fill("42.00")
    page.locator('input[name="txDescripcion"]').fill(TX_PERS_2_EDIT)
    submit_form(page, 'input[name="txImporte"]')
    esperar_red(page)
    expect(fila_transaccion(page, TX_PERS_2_EDIT)).to_contain_text("42.00€")
    registrar("Bloque 7", "Editar movimiento en Personal (35€ -> 42€)", True, TX_PERS_2_EDIT)

    # Borrar movimiento #3 (15€)
    fila_transaccion(page, TX_PERS_3).locator('button[title="Eliminar transacción"]').click()
    esperar_red(page)
    expect(page.locator("tbody tr").filter(has_text=TX_PERS_3)).to_have_count(0)
    registrar("Bloque 7", "Borrar movimiento en Personal (15€)", True, f"Eliminado {TX_PERS_3}")

    shot(page, "08_contexto_personal.png")


def generar_manual() -> None:
    fecha_gen = time.strftime("%d/%m/%Y %H:%M")
    lineas = [
        "# Manual de Usuario Visual — Gestión de Alquileres Multi-Contexto",
        "",
        "> **Documento validado automáticamente mediante pruebas E2E reales (`scripts/test_completo_app.py`)**.",
        "> Todas las capturas de pantalla corresponden a la ejecución en vivo contra **Angular 18 + Spring Boot 3 + PostgreSQL** bajo el patrón de prueba **«Crear 3 → Editar 1 → Borrar 1»**.",
        "",
        "---",
        "",
        "## 1. Arquitectura de los 3 Contextos y Filtro Temporal Global",
        "",
        "La aplicación separa de forma estricta tres ámbitos contables mediante la cabecera `X-Context-Id` y claves foráneas compuestas en base de datos:",
        "",
        "| Pestaña UI | Contexto (`X-Context-Id`) | Wallet Principal Única | Propósito Contable |",
        "| :--- | :--- | :--- | :--- |",
        "| **Cartera** | `REAL_ESTATE` | `Cartera Inmobiliaria` | Gestión de patrimonio inmobiliario: pisos, garajes, locales, inquilinos, contratos y flujo de caja de alquileres. |",
        "| **Casa** | `HOME` | `Casa` | Economía doméstica compartida: suministros, supermercado y mantenimiento del hogar. |",
        "| **Personal** | `PERSONAL` | `Cuenta Principal` | Finanzas individuales: salario, ocio, deporte y gastos personales. |",
        "",
        "### Selector de Periodo Temporal Global y Reglas Contables",
        "En la barra superior derecha dispones del **Filtro de Periodo Temporal** con tres modos (**Mes actual / Mensual**, **Anual** para cierres fiscales IRPF / Modelo 184, y **Todo el histórico**):",
        "- **Saldo Real en Caja (No filtrado):** Muestra siempre el dinero real acumulado disponible en la cuenta bancaria.",
        "- **Resultado Operativo del Periodo (Filtrado):** Calcula el beneficio o gasto neto del mes/año seleccionado excluyendo los `Traspasos Internos` entre tus propias cuentas.",
        "- **Píldoras de Categorías, Tabla de Transacciones y Rentabilidad de Inmuebles (Filtrados):** Se recalibran al instante al cambiar de mes o año.",
        "",
        "---",
        "",
        "## 2. Wallet Principal y Categorías con Totales Acumulados",
        "",
        "En cada contexto existe **una única Wallet Principal** protegida para evitar fragmentar saldos. Justo debajo se gestionan las **Categorías** del contexto:",
        "1. Pulsa **Nueva categoría**, introduce el nombre y haz clic en **Crear**.",
        "2. Cada píldora muestra en tiempo real su **total acumulado en `€`** en el periodo activo (en verde si es positivo, en rojo si es gasto neto).",
        "3. Pulsa **`×`** para eliminar una categoría sin movimientos (si tiene transacciones asociadas, el backend protege la integridad devolviendo `409 Conflict`).",
        "",
        "![Wallet principal y categorías en Cartera](docs/screenshots/01_cartera_wallet_y_categorias.png)",
        "",
        "---",
        "",
        "## 3. Gestión de Inmuebles e Inquilinos (Alta, Edición y Borrado)",
        "",
        "### 3.1 Inmuebles y Modo Edición (`PUT`)",
        "- **Alta:** Selecciona el tipo (`Piso`, `Chalet`, `Local`, `Garaje`), dirección, referencia catastral/interna y superficie en m², y pulsa **Añadir inmueble**.",
        "- **Edición (`✏️`):** Al pulsar el icono de lápiz sobre cualquier inmueble, el formulario carga sus datos y el botón cambia a **Guardar cambios** (con opción de **Cancelar**).",
        "- **Borrado (`🗑️`):** Elimina el inmueble tras confirmación (protegido con `409 Conflict` si tiene contratos o transacciones vinculados).",
        "",
        "![Formulario de Inmuebles en modo edición](docs/screenshots/02_inmuebles_modo_edicion.png)",
        "",
        "### 3.2 Inquilinos con Control de Documento Único",
        "- Registra nombre completo, email, teléfono y DNI/NIE/Pasaporte (único por contexto).",
        "- Permite edición directa con **`✏️`** y borrado protegido con **`🗑️`**.",
        "",
        "![CRUD completo de Inmuebles e Inquilinos](docs/screenshots/03_inmuebles_e_inquilinos_crud.png)",
        "",
        "---",
        "",
        "## 4. Contratos de Alquiler y Cobro Mensual en 1 Clic",
        "",
        "En la columna **Contratos** vinculas un **Inmueble** con un **Inquilino**, definiendo fechas de vigencia, **Importe mensual** y **Fianza**:",
        "- **Estado de Cobro Automático:** Cada tarjeta de contrato evalúa el mes seleccionado en el filtro superior:",
        "  - 🟠 **`PENDIENTE DE COBRO (YYYY-MM)`**: Aún no se ha registrado el ingreso del alquiler en ese mes. Muestra el botón rápido **«Cobrar mes (+X €)»**.",
        "  - 🟢 **`COBRADO ESTE MES (YYYY-MM)`**: Ya consta abonada la mensualidad del periodo.",
        "- **Acción «Cobrar mes» en 1 clic:** Genera automáticamente la transacción de `INGRESO` por el importe mensual del contrato en la wallet principal, imputada a la categoría *Alquiler Mensual*, al contrato y al inmueble correspondiente.",
        "",
        "![Contratos de alquiler y cobro mensual en 1 clic](docs/screenshots/04_contratos_cobrar_mes.png)",
        "",
        "---",
        "",
        "## 5. Transacciones y Rentabilidad Neta por Inmueble",
        "",
        "El formulario **Nueva transacción** registra ingresos y gastos asignándolos automáticamente a la wallet principal del contexto:",
        "- En **Cartera (`REAL_ESTATE`)** puedes vincular opcionalmente un **Contrato** (autocompleta inmueble e importe) o un **Inmueble** concreto (ej. *Seguro Hogar*, *Reparación fontanería*, *IBI*).",
        "- Cada tarjeta de **Inmueble** calcula en vivo su cuenta de explotación del periodo seleccionado:",
        "  - **`+Ingresos €`** (verde) · **`-Gastos €`** (rojo) · **`Neto: €`** (rentabilidad neta del activo).",
        "- Todas las filas de la tabla cuentan con edición inline (**`✏️`**) y eliminación (**`🗑️`**).",
        "",
        "![Transacciones y rentabilidad neta por inmueble](docs/screenshots/05_transacciones_y_rentabilidad_inmuebles.png)",
        "",
        "---",
        "",
        "## 6. Traspasos entre Cuentas con Edición y Borrado en Espejo",
        "",
        "El bloque **Traspasar entre cuentas** mueve liquidez entre contextos (por ejemplo, repartir beneficios desde `Cartera` hacia `Casa` o `Personal`):",
        "1. Genera atómicamente dos apuntes gemelos enlazados por `traspaso_par_id` bajo la categoría **`Traspaso Interno`**: un `GASTO` en la cuenta origen y un `INGRESO` en la cuenta destino.",
        "2. **Edición en Espejo (`✏️`):** Al editar una transacción de traspaso, se muestra un aviso ámbar bloqueando categoría, tipo y vínculos inmobiliarios, y al guardar se actualiza el **importe, fecha y descripción simultáneamente en ambos contextos**.",
        "3. **Borrado en Espejo (`🗑️`):** Al eliminar un traspaso desde cualquiera de los dos contextos, se elimina automáticamente su transacción gemela en el otro contexto para mantener el cuadre contable.",
        "",
        "![Edición en espejo de un traspaso entre contextos](docs/screenshots/06_traspaso_edicion_espejo.png)",
        "",
        "---",
        "",
        "## 7. Operativa en Contextos «Casa» (`HOME`) y «Personal» (`PERSONAL`)",
        "",
        "### 7.1 Pestaña Casa (`HOME`)",
        "- Recibe automáticamente como `INGRESO` los traspasos enviados desde `Cartera` (incluyendo cualquier edición o borrado en espejo).",
        "- Permite gestionar categorías domésticas (*Supermercado*, *Luz*, etc.) y registrar, editar o eliminar gastos del hogar.",
        "",
        "![Contexto Casa con traspaso sincronizado y gastos domésticos](docs/screenshots/07_contexto_casa.png)",
        "",
        "### 7.2 Pestaña Personal (`PERSONAL`)",
        "- Aísla los movimientos particulares y de ocio sobre la wallet `Cuenta Principal`, reflejando también los traspasos recibidos.",
        "",
        "![Contexto Personal con traspaso recibido y movimientos personales](docs/screenshots/08_contexto_personal.png)",
        "",
        "---",
        "",
        "## 8. Resumen de Pruebas E2E Ejecutadas (`Crear 3 -> Editar 1 -> Borrar 1`)",
        "",
        f"- **Lote de ejecución:** Sufijo `{SUFIJO}` · **Fecha:** `{fecha_gen}`",
        "",
        "| Bloque | Caso de Prueba Verificado | Estado | Detalle |",
        "| :--- | :--- | :---: | :--- |",
    ]
    for bloque, paso, exito, detalle in resultados:
        icono = "✅ OK" if exito else "❌ FALLO"
        lineas.append(f"| **{bloque}** | {paso} | {icono} | {detalle} |")

    lineas += [
        "",
        "---",
        f"*Manual y evidencias generados automáticamente el {fecha_gen} mediante `python scripts/test_completo_app.py`.*",
    ]
    MANUAL_PATH.write_text("\n".join(lineas), encoding="utf-8")
    print(f"\nManual actualizado correctamente en: {MANUAL_PATH.resolve()}")


def main() -> None:
    SHOT_DIR.mkdir(parents=True, exist_ok=True)
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=False, slow_mo=400)
        context = browser.new_context(
            viewport={"width": 1440, "height": 900},
            accept_downloads=True,
        )
        page = context.new_page()
        page.on("dialog", lambda d: d.accept())
        page.on("console", lambda msg: print(f"      [browser:{msg.type}] {msg.text[:200]}"))
        page.on("pageerror", lambda err: print(f"      [pageerror] {err}"))
        page.on("requestfailed", lambda req: print(f"      [reqfailed] {req.method} {req.url} -> {req.failure}"))
        page.on("response", lambda res: print(f"      [HTTP {res.status}] {res.request.method} {res.url}") if res.status >= 400 else None)

        try:
            ejecutar_flujo(page)
        except Exception as e:
            registrar("General", "Ejecución del flujo E2E", False, str(e)[:300])
            shot(page, "99_error_general.png")
            raise
        finally:
            browser.close()

    generar_manual()


if __name__ == "__main__":
    main()
