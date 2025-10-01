package com.example.demo.Controlador;

import com.example.demo.Entidad.BITACORA;
import com.example.demo.Entidad.DETALLEBHORAS;
import com.example.demo.Entidad.DETALLEBKILOMETRO;
import com.example.demo.Entidad.UNIDADES;
import com.example.demo.Repositorio.RepositorioUnidades;
import com.example.demo.Servicios.*;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.pdf.*;
import jakarta.servlet.http.HttpServletResponse;
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

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
    @Autowired
    private ServicioDetallebhoras servicioDetallebhoras;
    @Autowired
    private ServicioResponsable servicioResponsable;
    @Autowired
    private ServicioDetallebkilometro servicioDetallebkilometro;


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

        model.addAttribute("identificador", identificador);
        model.addAttribute("tipoUnidad", tipoUnidad);

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

            String regexSerie = "^[A-HJ-NPR-Z0-9-]{6,20}$";
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

    @GetMapping("/ReportetotalMensual")
    public void ReportetotalMensual(@RequestParam(required = false) Integer mes,
                                    @RequestParam(required = false) Integer anio,
                                    Model model,HttpServletResponse response) throws Exception {

        List<BITACORA> bitacorasdelmes= servicioBitacora.obtenerBitacorasConDetallePorMesYAniosp(mes, anio);

        if(bitacorasdelmes==null || bitacorasdelmes.isEmpty()){
            response.sendRedirect("/Unidades/ReporteMensual?mes=" + mes + "&anio=" + anio + "&page=0");
            return;
        }
        generarPDFUnificado(bitacorasdelmes,mes,response);
    }

    public void generarPDFUnificado(List<BITACORA> lista,int mes, HttpServletResponse response) throws Exception {
        response.setContentType("application/pdf");
        String nombrearchivo ="ReporteMensual"+mes+".pdf";
        response.setHeader("Content-Disposition", "attachment; filename=\""+nombrearchivo+"\"");

        Document document;
        BITACORA vbitacorai=lista.get(0);
        if(vbitacorai.getUnidad().getTipoUnidad().getMedicion().equals("Km")){
            document = new Document(PageSize.A4);
        }else{
            document = new Document(PageSize.A4.rotate());
        }
        PdfWriter writer = PdfWriter.getInstance(document, response.getOutputStream());
        document.open();

        for (int i = 0; i < lista.size(); i++){
            BITACORA b= lista.get(i);
            int anio = b.getAnio();
            YearMonth yearMonth = YearMonth.of(anio, mes);
            int diasDelMes = yearMonth.lengthOfMonth();

            Font fontTitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14);
            Font fontCabecera = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font fontFila = FontFactory.getFont(FontFactory.HELVETICA, 9);
            Font fontCabeceraPeque = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 8);
            Font fontFilaPeque = FontFactory.getFont(FontFactory.HELVETICA, 7);
            Font fontCabeceraGrande = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);

            if(b.getUnidad().getTipoUnidad().getMedicion().equalsIgnoreCase("Km")){
                int velocimetroinicial=0,velocimetrofinal=0;
                List<DETALLEBKILOMETRO> detalles = servicioDetallebkilometro.obtenerPorBitacora(b.getIdbitacora());
                Map<Integer, DETALLEBKILOMETRO> mapaDetalles = detalles.stream()
                        .collect(Collectors.toMap(DETALLEBKILOMETRO::getDia, d -> d));

                InputStream is = getClass().getResourceAsStream("/static/IMG/BitacoraKmcaratula.jpg");
                if (is != null) {
                    Image portada = Image.getInstance(is.readAllBytes());
                    portada.scaleToFit(PageSize.A4.getWidth(), PageSize.A4.getHeight());
                    portada.setAbsolutePosition(
                            (PageSize.A4.getWidth() - portada.getScaledWidth()) / 2,
                            (PageSize.A4.getHeight() - portada.getScaledHeight()) / 2
                    );
                    document.add(portada);
                    Font fontNegra = new Font(Font.HELVETICA, 20, Font.BOLD, java.awt.Color.BLACK);
                    ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_LEFT,
                            new Phrase(b.getUnidad().getTipoUnidad().getNombre(), fontNegra),
                            300, 270, 0);
                    ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_LEFT,
                            new Phrase(b.getUnidad().getIdentificador(), fontNegra),
                            300, 220, 0);
                    ColumnText.showTextAligned(writer.getDirectContent(),
                            Element.ALIGN_LEFT,
                            new Phrase(b.getUnidad().getNombre(), fontNegra),
                            300, 170, 0);
                    document.newPage(); // Sigue con el contenido en horizontal
                } else {
                    throw new FileNotFoundException("No se encontró la imagen de portada en /static/img/BitacoraHorascaratula.jpg");
                }

                double totalCombustible = 0;
                boolean vc = false;
                // ------------------ PÁGINA 1: RESUMEN DIARIO ------------------
                Paragraph titulo1 = new Paragraph("RESUMEN DIARIO", fontTitulo);
                titulo1.setAlignment(Element.ALIGN_CENTER);
                document.add(titulo1);


                String mestexto=obtenerNombreMes(mes).toUpperCase();
                Chunk meschunk=new Chunk(mestexto,fontCabecera);
                meschunk.setUnderline(1f,-2f);

                Chunk aniochunk= new Chunk(String.valueOf(anio),fontCabecera);
                aniochunk.setUnderline(1f,-2f);

                Phrase frase1= new Phrase();
                frase1.add(new Chunk("MES DE ",fontCabecera));
                frase1.add(meschunk);
                frase1.add(" ");
                frase1.add(aniochunk);

                Paragraph cabecera1 = new Paragraph(frase1);
                cabecera1.setAlignment(Element.ALIGN_RIGHT);
                cabecera1.setSpacingAfter(10f);
                document.add(cabecera1);

                PdfPTable tabla1 = new PdfPTable(12);
                tabla1.setWidthPercentage(100);
                tabla1.setWidths(new float[]{1f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f, 2f});

                String[] headers = {
                        "DÍA", "KMS INICIAL", "COMBUSTIBLE\nEN GLS.", "ACEITE DE\nMOTOR EN\nCUARTOS",
                        "ACEITE DE\nTRANSMISIÓN\nEN CUARTOS", "SERVICIO DE\nENGRASADO\nY LAVADO",
                        "SERVICIO DE\nMANTENIMIENTO", "FILTRO DE\nACEITE\nCAMBIO", "FILTRO\nPURIFICADOR\nCAMBIO",
                        "BATERÍA\nCAMBIO", "KMS FINAL", "VALE"
                };

                for (String h : headers) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fontCabecera));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    cell.setRotation(90);
                    cell.setFixedHeight(70f);
                    tabla1.addCell(cell);
                }

                // primera celda chiquita (debajo de "DIA")
                PdfPCell celdaDia = new PdfPCell(new Phrase(""));
                celdaDia.setFixedHeight(14f); // altura del espacio
                tabla1.addCell(celdaDia);

                // segunda celda que ocupa las 11 columnas restantes
                PdfPCell celdaResto = new PdfPCell(new Phrase(""));
                celdaResto.setColspan(11);
                celdaResto.setFixedHeight(14f);
                tabla1.addCell(celdaResto);

                for (int dia = 1; dia <= diasDelMes; dia++) {
                    DETALLEBKILOMETRO d = mapaDetalles.get(dia);
                    tabla1.addCell(new Phrase(String.valueOf(dia), fontFila));

                    if (d != null) {
                        if (!vc && d.getKminicial() != 0) {
                            velocimetroinicial = d.getKminicial();
                            vc = true;
                        }
                        if (d.getKmfinal() != 0) {
                            velocimetrofinal = d.getKmfinal();
                        }

                        tabla1.addCell(celdaDato(String.valueOf(d.getKminicial()), fontFila));
                        tabla1.addCell(celdaDato((d.getCombustiblegls() != 0) ? d.getCombustiblegls() + " gln" : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getAceitemotor() != 0) ? String.valueOf(d.getAceitemotor()) : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getAceitetransmision() != 0) ? String.valueOf(d.getAceitetransmision()) : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getServiciengrase() != null) ? d.getServiciengrase() : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getServiciomantenimiento() != null) ? d.getServiciomantenimiento() : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getFiltroaceitecambio() != null) ? d.getFiltroaceitecambio() : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getFiltropurificadorcambio() != null) ? d.getFiltropurificadorcambio() : " ", fontFila));
                        tabla1.addCell(celdaDato((d.getBateriacambio() != null) ? d.getBateriacambio() : " ", fontFila));
                        tabla1.addCell(celdaDato(String.valueOf(d.getKmfinal()), fontFila));
                        tabla1.addCell(celdaDato((d.getDestinovale()!=null) ? d.getDestinovale().getValeCombustible().getNvale()+"" : " ", fontFila));

                        if (d.getCombustiblegls() != 0)
                            totalCombustible += d.getCombustiblegls();
                    } else {
                        for (int j = 0; j < 11; j++) tabla1.addCell(new Phrase(" ", fontFila));
                    }
                }

                // Fila de totales
                PdfPCell totalCell = new PdfPCell(new Phrase("TOTALES", fontCabecera));
                totalCell.setColspan(2);
                totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla1.addCell(totalCell);
                tabla1.addCell(new Phrase(String.format("%.2f gln", totalCombustible), fontFila));
                for (int j = 0; j < 9; j++) tabla1.addCell(new Phrase(" ", fontFila));

                document.add(tabla1);

                //----Hoja Mantenimientos-----
                document.newPage();

                Paragraph tituloMantenimiento = new Paragraph("SERVICIOS DE MANTENIMIENTO REALIZADOS", fontCabecera);
                tituloMantenimiento.setAlignment(Element.ALIGN_CENTER);
                tituloMantenimiento.setSpacingAfter(8f);
                document.add(tituloMantenimiento);

                PdfPTable tabla2 = new PdfPTable(2);
                tabla2.setWidthPercentage(100);
                tabla2.setWidths(new float[]{1f, 7f});

                tabla2.addCell(new PdfPCell(new Phrase("DÍA", fontCabecera)));
                tabla2.addCell(new PdfPCell(new Phrase("SERVICIOS DE MANTENIMIENTO REALIZADOS", fontCabecera)));

                List<Integer> diasConMantenimiento = detalles.stream()
                        .filter(d -> d.getMantenimiendodescripcion() != null && !d.getMantenimiendodescripcion().isBlank())
                        .map(DETALLEBKILOMETRO::getDia)
                        .sorted()
                        .collect(Collectors.toList());

                for (Integer dia : diasConMantenimiento) {
                    DETALLEBKILOMETRO d = mapaDetalles.get(dia);
                    tabla2.addCell(new Phrase(String.valueOf(dia+"/"+d.getBitacora().getMes()), fontFila));
                    tabla2.addCell(new Phrase(d.getMantenimiendodescripcion(), fontFila));
                }

                for (int dia = 1; dia <= diasDelMes; dia++) {
                    if (!diasConMantenimiento.contains(dia)) {
                        tabla2.addCell(new Phrase(" ", fontFila));
                        tabla2.addCell(new Phrase(" ", fontFila));
                    }
                }

                document.add(tabla2);

                // --- Página 3: RESUMEN MENSUAL ---
                document.newPage();
                Paragraph titulo3 = new Paragraph("RESUMEN MENSUAL", fontTitulo);
                titulo3.setAlignment(Element.ALIGN_CENTER);
                document.add(titulo3);

                //Frase 2
                String iniciotext= "01 DE "+ obtenerNombreMes(mes).toUpperCase();
                Chunk iniciochunk = new Chunk(iniciotext, fontCabecera);
                iniciochunk.setUnderline(1f,-2f);

                String fintext= diasDelMes +" DE "+ obtenerNombreMes(mes).toUpperCase()+" "+anio;
                Chunk finchunk = new Chunk(fintext, fontCabecera);
                finchunk.setUnderline(1f,-2f);

                Phrase frase2= new Phrase();
                frase2.add(new Chunk("Periodo de ",fontCabecera));
                frase2.add(iniciochunk);
                frase2.add(new Chunk(" al ",fontCabecera));
                frase2.add(finchunk);

                //Frase 3
                String kminicio=String.valueOf(velocimetroinicial);
                Chunk kminiciochunk = new Chunk(kminicio, fontCabecera);
                kminiciochunk.setUnderline(1f,-2f);

                String kmfinal=String.valueOf(velocimetrofinal);
                Chunk kmfinalchunk = new Chunk(kmfinal, fontCabecera);
                kmfinalchunk.setUnderline(1f,-2f);

                Phrase frase3= new Phrase();
                frase3.add(new Chunk("Velocímetro Comienzo ",fontCabecera));
                frase3.add(kminiciochunk);
                frase3.add(new Chunk(" Final ",fontCabecera));
                frase3.add(kmfinalchunk);

                //Frase 4
                String unidadtext=b.getUnidad().getIdentificador();
                Chunk unidadchunk = new Chunk(unidadtext, fontCabecera);
                unidadchunk.setUnderline(1f,-2f);

                Phrase frase4= new Phrase();
                frase4.add(new Chunk("Agencia:    PNSDV     Placa: ",fontCabecera));
                frase4.add(unidadchunk);

                Paragraph cabecera3 = new Paragraph();
                cabecera3.add(frase2);
                cabecera3.add(Chunk.NEWLINE);
                cabecera3.add(frase3);
                cabecera3.add(Chunk.NEWLINE);
                cabecera3.add(frase4);
                cabecera3.setSpacingAfter(10f);
                document.add(cabecera3);

                PdfPTable tabla3 = new PdfPTable(3);
                tabla3.setWidthPercentage(100);
                tabla3.setWidths(new float[]{1f, 1.15f, 7f});
                tabla3.addCell(new PdfPCell(new Phrase("Día", fontCabecera)));
                tabla3.addCell(new PdfPCell(new Phrase("Kilómetros", fontCabecera)));
                tabla3.addCell(new PdfPCell(new Phrase("Detallar trabajos realizados", fontCabecera)));

                for (int dia = 1; dia <= diasDelMes; dia++) {
                    DETALLEBKILOMETRO d = mapaDetalles.get(dia);
                    tabla3.addCell(new Phrase(String.valueOf(dia), fontFila));
                    tabla3.addCell(new Phrase((d != null && d.getKmrecorridos() != 0) ? String.valueOf(d.getKmrecorridos()) : " ", fontFila));
                    tabla3.addCell(new Phrase((d != null && d.getTrabajosrealizados() != null) ? d.getTrabajosrealizados() : " ", fontFila));
                }
                document.add(tabla3);

                // --- Página 4: ANOTACIONES ---
                document.newPage();
                Paragraph titulo4 = new Paragraph("ANOTACIONES", fontTitulo);
                titulo4.setAlignment(Element.ALIGN_CENTER);
                titulo4.setSpacingAfter(8f);
                document.add(titulo4);

                PdfPTable tabla4 = new PdfPTable(2);
                tabla4.setWidthPercentage(100);
                tabla4.setWidths(new float[]{1f, 9f});
                tabla4.addCell(new PdfPCell(new Phrase("Día", fontCabecera)));
                tabla4.addCell(new PdfPCell(new Phrase("Anotaciones", fontCabecera)));

                List<Integer> diasConVale = detalles.stream()
                        .filter(d -> d.getDestinovale() != null) //
                        .map(DETALLEBKILOMETRO::getDia)
                        .sorted()
                        .collect(Collectors.toList());


                for (Integer dia : diasConVale) {
                    DETALLEBKILOMETRO d = mapaDetalles.get(dia);
                    tabla4.addCell(new Phrase(String.valueOf(dia+"/"+d.getBitacora().getMes()), fontFila));
                    tabla4.addCell(new Phrase(d.getAnotaciones(), fontFila));
                }

                for (int dia = 1; dia <= diasDelMes; dia++) {
                    if (!diasConVale.contains(dia)) {
                        tabla4.addCell(new Phrase(" ", fontFila));
                        tabla4.addCell(new Phrase(" ", fontFila));
                    }
                }
                document.add(tabla4);

            }else{

                List<DETALLEBHORAS> detalle = servicioDetallebhoras.obtenerPorBitacora(b.getIdbitacora());
                Map<Integer, DETALLEBHORAS> mapadetalles = detalle.stream().
                        collect(Collectors.toMap(DETALLEBHORAS::getDia,d->d));

                InputStream is = getClass().getResourceAsStream("/static/IMG/BitacoraHorascaratula.jpg");

                if(is != null) {
                    Image portada = Image.getInstance(is.readAllBytes());
                    portada.setRotationDegrees(-90f); // gira la imagen -90° (vertical visual)
                    portada.scaleToFit(PageSize.A4.getHeight(), PageSize.A4.getWidth());
                    portada.setAbsolutePosition(0, 0);
                    document.add(portada);

                    // ahora el canvas
                    PdfContentByte canvas = writer.getDirectContent();

                    // asegurar estado gráfico
                    canvas.saveState();

                    // fondo blanco
                    canvas.setColorFill(java.awt.Color.WHITE);
                    canvas.rectangle(480, 5, 120, 280); // posición de prueba más abajo
                    canvas.fill();
                    Font fontNegra = new Font(Font.HELVETICA, 16, Font.BOLD, java.awt.Color.BLACK);

                    // texto dentro del rectángulo
                    int margenX = 600;
                    int baseY = 285;

                    ColumnText.showTextAligned(canvas,
                            Element.ALIGN_LEFT,
                            new Phrase("Motor: "+b.getUnidad().getTipoUnidad().getNombre(), fontNegra),
                            margenX-20, baseY - 20, -90);

                    ColumnText.showTextAligned(canvas,
                            Element.ALIGN_LEFT,
                            new Phrase("Marca: "+b.getUnidad().getNombre(), fontNegra),
                            margenX-40, baseY - 20, -90);

                    ColumnText.showTextAligned(canvas,
                            Element.ALIGN_LEFT,
                            new Phrase("Serie: "+b.getUnidad().getIdentificador(), fontNegra),
                            margenX-60, baseY - 20, -90);

                    canvas.restoreState();
                    document.newPage();
                }
                else{
                    throw new FileNotFoundException("No se encontró la imagen de portada en /static/img/BitacoraHorascaratula.jpg");
                }

                Paragraph titulo = new Paragraph("PARTE DIARIO", fontTitulo);
                titulo.setAlignment(Element.ALIGN_CENTER);
                titulo.setSpacingBefore(5f);
                titulo.setSpacingAfter(2f);
                document.add(titulo);

                String mesTexto = obtenerNombreMes(mes).toUpperCase();
                Chunk mesChunk = new Chunk(mesTexto, fontCabeceraGrande);
                mesChunk.setUnderline(1f, -2f); // subrayado para mes

                Chunk anioChunk = new Chunk(String.valueOf(anio), fontCabeceraGrande);
                anioChunk.setUnderline(1f, -2f); // subrayado para año

                String unidadTexto = b.getUnidad().getIdentificador() + " - " + b.getUnidad().getNombre();
                Chunk unidadChunk = new Chunk(unidadTexto, fontCabeceraGrande);
                unidadChunk.setUnderline(1f, -2f); // subrayado para unidad

                Phrase frase = new Phrase();
                frase.add(new Chunk("Mes: ", fontCabeceraGrande));
                frase.add(mesChunk);
                frase.add(new Chunk("   Año: ", fontCabeceraGrande));
                frase.add(anioChunk);
                frase.add(new Chunk("   N°Serie/Unidad: ", fontCabeceraGrande));
                frase.add(unidadChunk);

                Paragraph cabecera = new Paragraph(frase);
                cabecera.setAlignment(Element.ALIGN_LEFT);
                cabecera.setSpacingAfter(8f);

                document.add(cabecera);
                //-----------------------------------------

                PdfPTable tabla = new PdfPTable(11);
                tabla.setWidthPercentage(100);
                tabla.setWidths(new float[]{0.85f, 1.5f, 1.5f, 2.3f, 1.5f, 2.7f, 9f, 9f, 3f, 1.5f, 4f});

                // Fila 1
                tabla.addCell(new PdfPCell(new Phrase("Día", fontCabeceraPeque)){{setRowspan(2); setHorizontalAlignment(Element.ALIGN_CENTER);}});
                tabla.addCell(new PdfPCell(new Phrase("Horas de Operación", fontCabeceraPeque)){{setColspan(3); setHorizontalAlignment(Element.ALIGN_CENTER);}});
                tabla.addCell(new PdfPCell(new Phrase("Consumo Gln", fontCabeceraPeque)){{setColspan(2); setHorizontalAlignment(Element.ALIGN_CENTER);}});
                tabla.addCell(new PdfPCell(new Phrase("Itinerario", fontCabeceraPeque)){{setColspan(2); setHorizontalAlignment(Element.ALIGN_CENTER);}});
                tabla.addCell(new PdfPCell(new Phrase("Operador Responsable", fontCabeceraPeque)){{setRowspan(2); setHorizontalAlignment(Element.ALIGN_CENTER);}});
                tabla.addCell(new PdfPCell(new Phrase("N° Vale", fontCabeceraPeque)){{setRowspan(2); setHorizontalAlignment(Element.ALIGN_CENTER);}});
                tabla.addCell(new PdfPCell(new Phrase("Servicio de Mantenimiento", fontCabeceraPeque)){{setRowspan(2); setHorizontalAlignment(Element.ALIGN_CENTER);}});

                // Fila 2
                String[] subHeaders = {"Hora\nInicio", "Hora\nFin", "Horas\nOperación", "Aceite", "Combustible", "Salida - Destino, Uso", "Justificación"};
                for (String h : subHeaders) {
                    PdfPCell cell = new PdfPCell(new Phrase(h, fontCabeceraPeque));
                    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
                    cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                    tabla.addCell(cell);
                }

                float totalHoras = 0f, totalCombustible = 0f;

                for (int dia = 1; dia <= diasDelMes; dia++) {
                    DETALLEBHORAS d = mapadetalles.get(dia);
                    tabla.addCell(celdaDato(String.valueOf(dia), fontFilaPeque));
                    if (d != null) {

                        tabla.addCell(celdaDato(String.valueOf(d.getHinicio()), fontFilaPeque));
                        tabla.addCell(celdaDato(String.valueOf(d.getHfinal()), fontFilaPeque));

                        tabla.addCell(celdaDato(convertirhoradecimalastring(d.getHoperacion()), fontFilaPeque));
                        tabla.addCell(celdaDato(d.getAceite(), fontFilaPeque));
                        if(d.getCombustible()==0){tabla.addCell(celdaDato( " ", fontFilaPeque));}
                        else{String comb= d.getCombustible()+" gln" ; tabla.addCell(celdaDato(comb, fontFilaPeque));}
                        tabla.addCell(celdaDato(d.getDestino(), fontFilaPeque));
                        tabla.addCell(celdaDato(d.getJustificacion(), fontFilaPeque));
                        tabla.addCell(celdaDato(servicioResponsable.buscarResponsable(d.getResponsable().getIdresponsable()).getNombre(), fontFilaPeque));

                        long nvale = 0;
                        if (d.getDestinovale() != null && d.getDestinovale().getValeCombustible() != null) {
                            nvale = d.getDestinovale().getValeCombustible().getNvale();
                        }
                        if(nvale==0){tabla.addCell(new Phrase(" ", fontFilaPeque));}
                        else{tabla.addCell(celdaDato(String.valueOf(nvale), fontFilaPeque));}

                        tabla.addCell(celdaDato(d.getReporte(), fontFilaPeque));

                        totalHoras += d.getHoperacion();
                        totalCombustible += d.getCombustible();
                    } else {
                        for (int j = 0; j < 10; j++) tabla.addCell(new Phrase(" ", fontFilaPeque));
                    }
                }

                // === Fila de Totales ===
                PdfPCell totalCell = new PdfPCell(new Phrase("TOTAL", fontCabeceraPeque));
                totalCell.setColspan(3); // Día + H. Inicio + H. Fin
                totalCell.setHorizontalAlignment(Element.ALIGN_CENTER);
                tabla.addCell(totalCell);

                // Celda aceite (vacía o con total si deseas)
                tabla.addCell(celdaDato(convertirhoradecimalastring(totalHoras), fontCabeceraPeque));

                tabla.addCell(celdaDato(" ", fontCabeceraPeque));

                // Celda con total de combustible
                tabla.addCell(celdaDato(String.format("%.2f gln", totalCombustible), fontCabeceraPeque));

                // Celdas vacías para las demás columnas (Destino, Justificación, Operador, Vale, Mantenimiento)
                for (int j = 0; j < 5; j++) {
                    tabla.addCell(celdaDato(" ", fontCabeceraPeque));
                }


                document.add(tabla);
            }

            //Verificacion de orientacion de pagina de la siguiente bitgacora

            if( i < lista.size()-1) {
                BITACORA bs = lista.get(i+1);
                if (bs.getUnidad().getTipoUnidad().getMedicion().equals("Km")) {
                    document.setPageSize(PageSize.A4);
                    document.newPage();
                }
                else{
                    document.setPageSize(PageSize.A4.rotate());
                    document.newPage();
                }
            }


        }
        document.close();
    }

    private PdfPCell celdaDato(String texto, Font font) {
        PdfPCell c = new PdfPCell(new Phrase(texto != null ? texto : " ", font));
        c.setHorizontalAlignment(Element.ALIGN_CENTER);
        c.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return c;
    }

    private String obtenerNombreMes(int mes) {
        switch (mes) {
            case 1: return "Enero";
            case 2: return "Febrero";
            case 3: return "Marzo";
            case 4: return "Abril";
            case 5: return "Mayo";
            case 6: return "Junio";
            case 7: return "Julio";
            case 8: return "Agosto";
            case 9: return "Septiembre";
            case 10: return "Octubre";
            case 11: return "Noviembre";
            case 12: return "Diciembre";
            default: return "";
        }
    }

    public String convertirhoradecimalastring(float horas){
        int h = (int) Math.floor(horas);
        int m= (int) Math.round((horas-h)*60);
        return h+"h "+m+"m";
    }
















}
