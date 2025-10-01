package com.example.demo.Entidad;


import jakarta.persistence.*;

@Entity
@Table(name = "DESTINOVALE")
public class DESTINOVALE {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int iddestinovale;

    @ManyToOne
    @JoinColumn(name = "idvale_combustible", foreignKey = @ForeignKey(name = "fk_destinovale_vale"))
    private VALECOMBUSTIBLE valeCombustible;


    private float cantidad;
    private float saldorestante;

    private String destino;

    @ManyToOne
    @JoinColumn(name="idresponsable", foreignKey = @ForeignKey(name = "fk_destinovale_responsable"))
    private RESPONSABLE responsable;

    public int getIddestinovale() {
        return iddestinovale;
    }

    public void setIddestinovale(int iddestinovale) {
        this.iddestinovale = iddestinovale;
    }

    public VALECOMBUSTIBLE getValeCombustible() {
        return valeCombustible;
    }

    public void setValeCombustible(VALECOMBUSTIBLE valeCombustible) {
        this.valeCombustible = valeCombustible;
    }

    public float getCantidad() {
        return cantidad;
    }

    public void setCantidad(float cantidad) {
        this.cantidad = cantidad;
    }

    public String getDestino() {
        return destino;
    }

    public void setDestino(String destino) {
        this.destino = destino;
    }

    public RESPONSABLE getResponsable() {
        return responsable;
    }

    public void setResponsable(RESPONSABLE responsable) {
        this.responsable = responsable;
    }

    public float getSaldorestante() {
        return saldorestante;
    }

    public void setSaldorestante(float saldorestante) {
        this.saldorestante = saldorestante;
    }
}
