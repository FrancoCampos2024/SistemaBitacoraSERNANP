package com.example.demo.Controlador;

import com.example.demo.Entidad.BITACORA;
import com.example.demo.Entidad.UNIDADES;
import com.example.demo.Repositorio.RepositorioUnidades;
import com.example.demo.Servicios.ServicioBitacora;
import com.example.demo.Servicios.ServicioTipocombustible;
import com.example.demo.Servicios.ServicioTipounidad;
import com.example.demo.Servicios.ServicioUnidades;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("Unidades")
public class UnidadControlador {
    @Autowired
    @Qualifier("ServicioUnidades")
    private ServicioUnidades servicioUnidades;

    @Autowired
    private ServicioTipocombustible servicioTipocombustible;

    @Autowired
    private ServicioTipounidad servicioTipounidad;
    @Autowired
    private ServicioBitacora servicioBitacora;


    @GetMapping("/ListaUnidades")
    public String listarUnidades(
            Model model,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(name = "identificador", required = false) String identificador,
            @RequestParam(name = "tipoUnidad", required = false) String tipoUnidad
    ) {
        Pageable pageable = PageRequest.of(page, 5, Sort.by("identificador").ascending());
        Page<UNIDADES> unidadesPage;

        if (identificador != null && !identificador.isBlank()) {
            unidadesPage = servicioUnidades.buscarPorIdentificador(identificador, pageable);
        } else if (tipoUnidad != null && !tipoUnidad.isBlank()) {
            unidadesPage = servicioUnidades.buscarPorTipoUnidad(tipoUnidad, pageable);
        } else {
            unidadesPage = servicioUnidades.listarTodoPaginado(pageable);
        }

        model.addAttribute("unidad", new UNIDADES());
        model.addAttribute("unidadcondato", new UNIDADES());
        model.addAttribute("unidades", unidadesPage);
        model.addAttribute("tipounidad", servicioTipounidad.getTipounidad());
        model.addAttribute("tipocombustible", servicioTipocombustible.listarTipocombustible());

        model.addAttribute("identificador", identificador); // para mantener el filtro
        model.addAttribute("tipoUnidad", tipoUnidad);       // para mantener el filtro

        return "Unidades/ListarUnidades";
    }


    @PostMapping("/Agregar")
    public String agregarUnidades(@ModelAttribute("unidad") UNIDADES unidad,
                                  Model model,
                                  @RequestParam(name="tipoUnidad.idtipou",required = false)Integer idtipoUnidad,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(name="tipoCombustible.idtipocombustible",required = false)Integer idtipocombustible,
                                  RedirectAttributes redirectAttributes,
                                  @RequestParam(name = "identificador", required = false) String identificador,
                                  @RequestParam("tipoIdentificador") String tipoIdentificador,
                                  @RequestParam(name = "tipoUnidad", required = false) String tipoUnidad) {

        // Validación manual
        String error = validarUnidad(unidad,idtipocombustible,idtipoUnidad,tipoIdentificador);

        if (error != null) {
            Pageable pageable = PageRequest.of(page, 5, Sort.by("identificador").ascending());
            Page<UNIDADES> unidadesPage;

            if (identificador != null && !identificador.isBlank()) {
                unidadesPage = servicioUnidades.buscarPorIdentificador(identificador, pageable);
            } else if (tipoUnidad != null && !tipoUnidad.isBlank()) {
                unidadesPage = servicioUnidades.buscarPorTipoUnidad(tipoUnidad, pageable);
            } else {
                unidadesPage = servicioUnidades.listarTodoPaginado(pageable);
            }

            model.addAttribute("unidad", unidad);
            model.addAttribute("unidadcondato",new UNIDADES());
            model.addAttribute("unidades", unidadesPage);
            model.addAttribute("tipocombustible", servicioTipocombustible.listarTipocombustible());
            model.addAttribute("tipounidad", servicioTipounidad.getTipounidad());
            model.addAttribute("error", error);
            model.addAttribute("mostrarModal", true);
            return "Unidades/ListarUnidades";
        }

        unidad.setEstado(true);
        servicioUnidades.agregarunidad(unidad);
        redirectAttributes.addFlashAttribute("guardadoExito", true);

        return "redirect:/Unidades/ListaUnidades";
    }


    public String validarUnidad(UNIDADES unidad ,Integer idtipocombustible,Integer idtipounidad,String tipoIdentificador) {
        String identificador = unidad.getIdentificador();
        if (unidad.getIdentificador() == null || unidad.getIdentificador().trim().isEmpty()) {
            return "Debe ingresar un identificador.";
        }
        identificador = identificador.trim().toUpperCase();

        // Validación según tipo
        if ("placa".equalsIgnoreCase(tipoIdentificador)) {
            String regexTodasPlacasPeru = "^(" +
                    "[A-HJ-NP-Z][A-HJ-NP-Z0-9]{2}-\\d{3}|" +   // Autos particulares (ej: A1B-234, sin I/O/Q)
                    "\\d{4}-[A-HJ-NP-Z]{2}|" +                // Motos tipo 1 (ej: 1234-KS)
                    "[A-HJ-NP-Z]{2}-\\d{4}|" +                // Motos tipo 2 (ej: KS-1234)
                    "[A-HJ-NP-Z]{3}-\\d{3}|" +                // Remolques
                    "CD-\\d{4}|" +
                    "CC-\\d{4}|" +
                    "OF-\\d{4}|" +
                    "(EP|FAP|MGP|PNP)-\\d{4,5}|" +
                    "PR-\\d{4}" +
                    ")$";

            if (!identificador.matches(regexTodasPlacasPeru)) {
                return "Formato de placa inválido. Debe ser un formato oficial peruano (Ej: A1B-234, 1234-AB, CD-1234). No se permiten las letras I, Ñ, O ni Q.";
            }
        }
        else if ("serie".equalsIgnoreCase(tipoIdentificador)) {
            identificador = identificador.trim().toUpperCase();

            if (identificador.contains("Ñ")) {
                return "El número de serie no puede contener la letra Ñ.";
            }

            String regexSerie = "^[A-HJ-NPR-Z0-9]{6,20}$";
            if (!identificador.matches(regexSerie)) {
                return "El número de serie debe tener entre 6 y 20 caracteres alfanuméricos válidos (sin I, O, Q ni Ñ).";
            }
        }
        else {
            return "Debe seleccionar si es Placa o N° de Serie.";
        }

        if (unidad.getNombre() == null || unidad.getNombre().trim().isEmpty()) {
            return "Debe ingresar un nombre para la unidad.";
        }
        if (unidad.getNombre().length() > 50) {
            return "El nombre no debe superar los 50 caracteres.";
        }

        if ( idtipocombustible == null || idtipocombustible == 0) {
            return "Debe seleccionar un tipo de combustible.";
        }

        if (idtipounidad == null || idtipounidad == 0) {
            return "Debe seleccionar un tipo de unidad.";
        }

        UNIDADES unidadexistente = servicioUnidades.findByIdentificador(unidad.getIdentificador());
        if (unidadexistente != null) {
            if (unidadexistente.getIdunidad()!=(unidad.getIdunidad())) {
                return "Identificador ya existente para otra unidad.";
            }
        }
        return null;
    }


    @GetMapping("/CambiarEstado/{id}")
    public String cambiarEstadoUnidad(@PathVariable int id) {
        UNIDADES unidad = servicioUnidades.buscarporid(id);
        if (unidad != null) {
            unidad.setEstado(!unidad.isEstado());
            servicioUnidades.agregarunidad(unidad);
        }
        return "redirect:/Unidades/ListaUnidades";
    }

    @GetMapping("/Editar/{id}")
    public String mostrarFormularioEditar(@PathVariable int id, Model model,
                                          @RequestParam(defaultValue = "0") int page,
                                          @RequestParam(name = "identificador", required = false) String identificador,
                                          @RequestParam(name = "tipoUnidad", required = false) String tipoUnidad) {
        Pageable pageable = PageRequest.of(page, 5, Sort.by("identificador").ascending());
        Page<UNIDADES> unidadesPage;

        if (identificador != null && !identificador.isBlank()) {
            unidadesPage = servicioUnidades.buscarPorIdentificador(identificador, pageable);
        } else if (tipoUnidad != null && !tipoUnidad.isBlank()) {
            unidadesPage = servicioUnidades.buscarPorTipoUnidad(tipoUnidad, pageable);
        } else {
            unidadesPage = servicioUnidades.listarTodoPaginado(pageable);
        }
        UNIDADES unidad = servicioUnidades.buscarporid(id);
        model.addAttribute("unidad", new UNIDADES());
        model.addAttribute("unidades", unidadesPage);
        model.addAttribute("unidadcondato", unidad);
        model.addAttribute("tipocombustible", servicioTipocombustible.listarTipocombustible());
        model.addAttribute("tipounidad", servicioTipounidad.getTipounidad());
        model.addAttribute("abrirModalEditar", true);
        return "Unidades/ListarUnidades";
    }

    @PostMapping("/Editado/{id}")
    public String guardadoedit(@ModelAttribute UNIDADES unidadcondato,
                               @PathVariable int id,
                               @RequestParam(name="tipoUnidad.idtipou", required=false) Integer idtipou,
                               @RequestParam(name="tipoCombustible.idtipocombustible", required=false) Integer idtipocombustible,
                               RedirectAttributes redirectAttributes,
                               Model model,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam("tipoIdentificador") String tipoIdentificador,
                               @RequestParam(name = "identificador", required = false) String identificador,
                               @RequestParam(name = "tipoUnidad", required = false) String tipoUnidad) {

        String error = validarUnidad(unidadcondato, idtipocombustible, idtipou,tipoIdentificador);
        if (error != null) {
            Pageable pageable = PageRequest.of(page, 5, Sort.by("identificador").ascending());
            Page<UNIDADES> unidadesPage;

            if (identificador != null && !identificador.isBlank()) {
                unidadesPage = servicioUnidades.buscarPorIdentificador(identificador, pageable);
            } else if (tipoUnidad != null && !tipoUnidad.isBlank()) {
                unidadesPage = servicioUnidades.buscarPorTipoUnidad(tipoUnidad, pageable);
            } else {
                unidadesPage = servicioUnidades.listarTodoPaginado(pageable);
            }

            model.addAttribute("errorE", error);
            model.addAttribute("unidad", new UNIDADES());
            model.addAttribute("unidadcondato", unidadcondato);
            model.addAttribute("unidades", unidadesPage);
            model.addAttribute("tipounidad", servicioTipounidad.getTipounidad());
            model.addAttribute("tipocombustible", servicioTipocombustible.listarTipocombustible());
            model.addAttribute("abrirModalEditar", true);
            return "Unidades/ListarUnidades";
        }

        // ✅ Actualización si todo está bien
        UNIDADES unidadbd = servicioUnidades.buscarporid(id);
        unidadbd.setIdentificador(unidadcondato.getIdentificador());
        unidadbd.setNombre(unidadcondato.getNombre());
        unidadbd.setTipoUnidad(servicioTipounidad.buscarTipounidadid(idtipou));
        unidadbd.setTipoCombustible(servicioTipocombustible.buscarTipocombustible(idtipocombustible));

        servicioUnidades.agregarunidad(unidadbd);
        redirectAttributes.addFlashAttribute("guardadoExito", true);

        return "redirect:/Unidades/ListaUnidades";
    }

    @GetMapping("/Filtrar")
    public String filtrarAjax(@RequestParam String nombreUnidad,
                              @RequestParam String tipoUnidad,
                              Model model) {
        List<UNIDADES> unidadesFiltradas = servicioUnidades.buscarPorIdentificadorOTipoUnidad(nombreUnidad, tipoUnidad);
        model.addAttribute("unidades", unidadesFiltradas);
        return "Unidades/ListarUnidades :: tablaUnidades";
    }

    @GetMapping("/ReporteMensual")
    public String verReporteMensual(@RequestParam(required = false) Integer mes,
                                    @RequestParam(required = false) Integer anio,
                                    @RequestParam(defaultValue = "0") int page,
                                    Model model) {
        LocalDate ahora = LocalDate.now();
        if (mes == null) mes = ahora.getMonthValue();
        if (anio == null) anio = ahora.getYear();

        Page<BITACORA> bitacoras = servicioBitacora.obtenerBitacorasConDetallePorMesYAnio(mes, anio, PageRequest.of(page, 5));

        List<Integer> aniosDisponibles = servicioBitacora.obtenerAniosDisponibles();

        model.addAttribute("mes", mes);
        model.addAttribute("anio", anio);
        model.addAttribute("aniosDisponibles", aniosDisponibles);
        model.addAttribute("bitacorasFiltradas", bitacoras);
        return "Bitacoras/ReporteMensual";
    }















}
