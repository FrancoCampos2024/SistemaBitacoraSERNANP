package com.example.demo.Controlador;


import com.example.demo.Entidad.*;
import com.example.demo.Servicios.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.propertyeditors.CustomDateEditor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;

@Controller
@RequestMapping("Combustible")
public class ValeControlador {
    @Autowired
    @Qualifier("Serviciovalecombustible")
    private ServicioValecombustible servicioValecombustible;
    @Autowired
    @Qualifier("ServicioTipocombustible")
    private ServicioTipocombustible servicioTipocombustible;
    @Autowired
    @Qualifier("Serviciogrifo")
    private ServicioGrifo Serviciogrifo;
    @Autowired
    private ServicioGrifo servicioGrifo;
    @Autowired
    private ServicioResponsable servicioResponsable;
    @Autowired
    private ServicioDetallebkilometro servicioDetallebkilometro;
    @Autowired
    private ServicioDetallebhoras servicioDetallebhoras;
    @Autowired
    private ServicioDestino servicioDestino;
    @Autowired
    private ServicioDestinovale servicioDestinovale;


    @GetMapping("/Listavales")
    public String ListaVales(Model model,
                             @RequestParam(defaultValue = "0") int pagina,
                             @RequestParam(name = "numeroVale", required = false) String numeroVale,
                             @RequestParam(name = "desde", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date desde,
                             @RequestParam(name = "hasta", required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date hasta,
                             @ModelAttribute("vale") VALECOMBUSTIBLE vale,
                             @ModelAttribute("valecondato") VALECOMBUSTIBLE valecondato) {

        Pageable pageable = PageRequest.of(pagina, 10);
        Page<VALECOMBUSTIBLE> responsablePage;

        if (numeroVale != null && !numeroVale.isBlank()) {
            responsablePage = servicioValecombustible.buscarPorCoincidenciaNvale(numeroVale, pageable);

        } else if (desde != null && hasta != null) {
            responsablePage = servicioValecombustible.buscarPorRangoFechas(
                    new java.sql.Date(desde.getTime()),
                    new java.sql.Date(hasta.getTime()),
                    pageable
            );
        } else {
            responsablePage = servicioValecombustible.listarValecombustiblePaginado(pageable);
        }

        model.addAttribute("valescombustible", responsablePage);
        model.addAttribute("tiposcombustible", servicioTipocombustible.listarTipocombustible());
        model.addAttribute("grifos", servicioGrifo.ListarGrifo());
        model.addAttribute("numeroVale", numeroVale);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        if (!model.containsAttribute("vale")) {
            VALECOMBUSTIBLE nuevoVale = new VALECOMBUSTIBLE();
            nuevoVale.setFecha(new java.sql.Date(System.currentTimeMillis()));
            model.addAttribute("vale", nuevoVale);
        }

        if (!model.containsAttribute("valecondato")) {
            model.addAttribute("valecondato", new VALECOMBUSTIBLE());
        }

        return "Combustible/ListarValescombustible";
    }



    @PostMapping("/Agregar")
    public String agregarVale(@ModelAttribute("vale") VALECOMBUSTIBLE vale,
                              Model model,
                              @RequestParam(defaultValue = "0") int pagina, RedirectAttributes redirectAttributes) {

        String error = validarVale(vale);
        if (error != null) {
            Pageable pageable = PageRequest.of(pagina, 10);
            Page<VALECOMBUSTIBLE> responsablePage = servicioValecombustible.listarValecombustiblePaginado(pageable);

            model.addAttribute("valescombustible", responsablePage);
            model.addAttribute("tiposcombustible", servicioTipocombustible.listarTipocombustible());
            model.addAttribute("grifos", servicioGrifo.ListarGrifo());
            model.addAttribute("valecondato", new VALECOMBUSTIBLE());
            model.addAttribute("Error", error);
            model.addAttribute("vale", vale);
            model.addAttribute("abrirModalAgregar", true);

            return "Combustible/ListarValescombustible";
        }

        vale.setSaldorestante(vale.getCantidad());
        servicioValecombustible.AgregarValecombustible(vale);
        redirectAttributes.addFlashAttribute("guardadoExito", true);

        return "redirect:/Combustible/Listavales";
    }


    @InitBinder
    public void initBinder(WebDataBinder binder) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        dateFormat.setLenient(false);
        binder.registerCustomEditor(java.sql.Date.class, new CustomDateEditor(dateFormat, true));
    }


    private String validarVale(VALECOMBUSTIBLE vale) {
        if (vale == null) return "Error inesperado: los datos del vale están vacíos.";

        if (vale.getNvale() <= 0) {
            return "Debe ingresar un número de vale correcto.";
        }

        VALECOMBUSTIBLE existente = servicioValecombustible.buscarPorNroVale(vale.getNvale());
        if (existente != null && existente.getIdvcombustible() != vale.getIdvcombustible()) {
            return "El número de vale ingresado ya existe.";
        }


        if (vale.getFecha() == null) {
            return "Debe seleccionar una fecha.";
        }


        if (vale.getGrifo() == null || vale.getGrifo().getIdgrifo() == 0) {
            return "Debe seleccionar un grifo.";
        }

        if (vale.getTipoCombustible() == null || vale.getTipoCombustible().getIdtipocombustible() == 0) {
            return "Debe seleccionar un tipo de combustible.";
        }

        if (vale.getCantidad() <= 0) {
            return "La cantidad debe ser mayor a cero.";
        }

        return null;
    }





    @GetMapping("/Editar/{id}")
    public String editarVale(@PathVariable int id, Model model,@RequestParam(defaultValue = "0") int pagina) {

        Pageable pageable = PageRequest.of(pagina, 10);
        Page<VALECOMBUSTIBLE> responsablePage = servicioValecombustible.listarValecombustiblePaginado(pageable);

        model.addAttribute("valescombustible",responsablePage);

        model.addAttribute("vale",new VALECOMBUSTIBLE());
        model.addAttribute("valecondato", servicioValecombustible.obtenerPorId(id));

        model.addAttribute("grifos", servicioGrifo.ListarGrifo());
        model.addAttribute("tiposcombustible", servicioTipocombustible.listarTipocombustible());
        model.addAttribute("abrirModalEditar", true);


        return "Combustible/ListarValescombustible";
    }

    @PostMapping("/Editado/{id}")
    public String editarVale(@PathVariable int id,
                             @ModelAttribute("valecondato") VALECOMBUSTIBLE valecondato,
                             Model model,
                             @RequestParam(defaultValue = "0") int pagina, RedirectAttributes redirectAttributes) {

        String error = validarVale(valecondato);
        if (error != null) {
            Pageable pageable = PageRequest.of(pagina, 10);
            Page<VALECOMBUSTIBLE> responsablePage = servicioValecombustible.listarValecombustiblePaginado(pageable);

            model.addAttribute("valescombustible", responsablePage);
            model.addAttribute("tiposcombustible", servicioTipocombustible.listarTipocombustible());
            model.addAttribute("grifos", servicioGrifo.ListarGrifo());
            model.addAttribute("vale",new VALECOMBUSTIBLE());
            model.addAttribute("Error", error);
            model.addAttribute("valecondato", valecondato);
            model.addAttribute("abrirModalEditar", true);

            return "Combustible/ListarValescombustible";
        }


        VALECOMBUSTIBLE valeBD = servicioValecombustible.obtenerPorId(id);
        valeBD.setNvale(valecondato.getNvale());
        valeBD.setFecha(valecondato.getFecha());
        valeBD.setCantidad(valecondato.getCantidad());
        valeBD.setSaldorestante(valecondato.getCantidad());
        valeBD.setGrifo(valecondato.getGrifo());
        valeBD.setTipoCombustible(valecondato.getTipoCombustible());
        redirectAttributes.addFlashAttribute("guardadoExito", true);
        servicioValecombustible.editarValecombustible(valeBD);

        return "redirect:/Combustible/Listavales";
    }




    @GetMapping("/Detalle/{id}")
    public String detalleVale(@PathVariable Integer id, Model model) {

        VALECOMBUSTIBLE vale = servicioValecombustible.obtenerPorId(id);
        List<DESTINOVALE> detalle = vale.getDestinos();

        Map<Integer, Boolean> puedeEliminarMap = new HashMap<>();
        for (DESTINOVALE d : detalle) {
            boolean usadoEnKm = servicioDetallebkilometro.fueUsado(d.getIddestinovale());
            boolean usadoEnHoras = servicioDetallebhoras.fueUsado(d.getIddestinovale());
            puedeEliminarMap.put(d.getIddestinovale(), !(usadoEnKm || usadoEnHoras));
        }
        model.addAttribute("puedeEliminarMap", puedeEliminarMap);

        List<Map<String, Object>> resumenMensual = calcularResumenPorValeHastaSaldoCero(vale);
        model.addAttribute("destinovale",new DESTINOVALE());
        model.addAttribute("destinos",servicioDestino.listardestinos());
        model.addAttribute("responsables",servicioResponsable.listarResponsables());
        model.addAttribute("resumenMensual", resumenMensual);
        model.addAttribute("vale", vale);
        model.addAttribute("detalleDestinos", detalle);
        return "Combustible/DetalleVale";
    }

    public List<Map<String, Object>> calcularResumenPorValeHastaSaldoCero(VALECOMBUSTIBLE vale) {
        List<Map<String, Object>> resumen = new ArrayList<>();

        int mes = vale.getFecha().toLocalDate().getMonthValue();
        int anio = vale.getFecha().toLocalDate().getYear();
        double saldo = vale.getCantidad();

        LocalDate ahora = LocalDate.now();
        int mesActual = ahora.getMonthValue();
        int anioActual = ahora.getYear();

        int mesesTranscurridos = (anioActual - anio) * 12 + (mesActual - mes);

        System.out.println("mes: "+mes);
        System.out.println("anio: "+anio);
        System.out.println("mesActual: "+mesActual);
        System.out.println("anioActual: "+anioActual);

        for (int i = 0; i <= mesesTranscurridos; i++) {
            double saldousadoendkm = 0;
            double saldousadoendhoras = 0;
            double usototalmes = 0;

            List<DETALLEBKILOMETRO> dkm = servicioDetallebkilometro
                    .listardetalleparaconsumoporvale(vale.getIdvcombustible(), mes, anio);
            saldousadoendkm = dkm.stream()
                    .mapToDouble(DETALLEBKILOMETRO::getCombustiblegls)
                    .sum();

            List<DETALLEBHORAS> dh = servicioDetallebhoras
                    .listardetalleparaconsumoporvale(vale.getIdvcombustible(), mes, anio);
            saldousadoendhoras = dh.stream()
                    .mapToDouble(DETALLEBHORAS::getCombustible)
                    .sum();
            System.out.println("Saldo Usado en horas:"+saldousadoendhoras);

            usototalmes = saldousadoendkm + saldousadoendhoras;

            double saldoSiguiente = saldo - usototalmes;

            Map<String, Object> fila = new LinkedHashMap<>();
            fila.put("mes", Month.of(mes).getDisplayName(TextStyle.FULL, new Locale("es", "ES")) + " " + anio);
            fila.put("saldoUsado", usototalmes);
            fila.put("saldoSiguiente", saldoSiguiente);
            resumen.add(fila);
            saldo = saldoSiguiente;


            if (saldo <= 0) break;
            mes++;
            if (mes > 12) {
                mes = 1;
                anio++;
            }
        }

        return resumen;
    }

    @PostMapping("/GuardarDestinovale")
    public String guardarDestino(@ModelAttribute DESTINOVALE destinovale,
                                 @RequestParam int idvale,
                                 RedirectAttributes redirect,
                                 Model model) {
        VALECOMBUSTIBLE vale=servicioValecombustible.obtenerPorId(idvale);
        destinovale.setValeCombustible(vale);

        String error = validarDestinoVale(destinovale);
        if (error != null) {

            model.addAttribute("Error", error);
            model.addAttribute("abrirModalAgregar", true);

            List<DESTINOVALE> detalle = vale.getDestinos();
            List<Map<String, Object>> resumenMensual = calcularResumenPorValeHastaSaldoCero(vale);

            Map<Integer, Boolean> puedeEliminarMap = new HashMap<>();
            for (DESTINOVALE d : detalle) {
                boolean usadoEnKm = servicioDetallebkilometro.fueUsado(d.getIddestinovale());
                boolean usadoEnHoras = servicioDetallebhoras.fueUsado(d.getIddestinovale());
                puedeEliminarMap.put(d.getIddestinovale(), !(usadoEnKm || usadoEnHoras));
            }
            model.addAttribute("puedeEliminarMap", puedeEliminarMap);

            model.addAttribute("destinovale", destinovale); // ← Importante para mantener lo que ya se escribió
            model.addAttribute("destinos", servicioDestino.listardestinos());
            model.addAttribute("responsables", servicioResponsable.listarResponsables());
            model.addAttribute("resumenMensual", resumenMensual);
            model.addAttribute("vale", vale);
            model.addAttribute("detalleDestinos", detalle);
            return "Combustible/DetalleVale";
        }

        destinovale.setSaldorestante(destinovale.getCantidad());
        vale.setSaldorestante(vale.getSaldorestante()-destinovale.getCantidad());
        servicioValecombustible.AgregarValecombustible(vale);
        servicioDestinovale.agregarDestinovale(destinovale);
        return "redirect:/Combustible/Detalle/" + idvale;
    }


    private String validarDestinoVale(DESTINOVALE destinovale) {
        if (destinovale.getDestino() == null || destinovale.getDestino().getIddestino() == 0) {
            return "Debe seleccionar un destino válido.";
        }
        if (destinovale.getResponsable() == null || destinovale.getResponsable().getIdresponsable() == 0) {
            return "Debe seleccionar un responsable.";
        }
        if (destinovale.getCantidad() <= 0) {
            return "La cantidad debe ser mayor que 0.";
        }


        if (destinovale.getCantidad() > destinovale.getValeCombustible().getSaldorestante()) {
            return "La cantidad excede el saldo restante del vale.";
        }
        return null;
    }


    @GetMapping("/Desagsinardestino/{idvdestino}")
    public String desagsinar(@PathVariable int idvdestino, Model model) {
        DESTINOVALE dvale= servicioDestinovale.obtenerPorId(idvdestino);
        VALECOMBUSTIBLE vale=servicioValecombustible.obtenerPorId(dvale.getValeCombustible().getIdvcombustible());
        vale.setSaldorestante(vale.getSaldorestante()+dvale.getCantidad());
        servicioValecombustible.AgregarValecombustible(vale);
        servicioDestinovale.desagsinardestino(dvale);
        return "redirect:/Combustible/Detalle/" + vale.getIdvcombustible();
    }



    @GetMapping("/Destino/{id}")
    public String mostrarFormularioAsignar(@PathVariable int id, Model model) {
        DESTINOVALE destino = new DESTINOVALE();
        destino.setValeCombustible(servicioValecombustible.obtenerPorId(id));
        model.addAttribute("destino", destino);
        model.addAttribute("valeId", id);
        model.addAttribute("responsables", servicioResponsable.listarResponsables());
        model.addAttribute("abrirModalAsignar", true);
        return "Combustible/ListarValescombustible";
    }

}
