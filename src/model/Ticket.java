package model;

import java.util.UUID;

public class Ticket {
    private String id;
    private String tipo;
    private String descripcion;
    private boolean solucionado;
    private String tecnicoAsignado;

    public Ticket(String tipo, String descripcion) {
        this.id = UUID.randomUUID().toString();
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.solucionado = false;
        this.tecnicoAsignado = "";
    }

    // Getters y Setters
    public String getId() { return id; }
    public String getTipo() { return tipo; }
    public String getDescripcion() { return descripcion; }
    public boolean isSolucionado() { return solucionado; }
    public String getTecnicoAsignado() { return tecnicoAsignado; }

    public void setSolucionado(boolean solucionado) { this.solucionado = solucionado; }
    public void setTecnicoAsignado(String tecnico) { this.tecnicoAsignado = tecnico; }

    @Override
    public String toString() {
        return "Ticket{" +
                "id='" + id + '\'' +
                ", tipo='" + tipo + '\'' +
                ", descripcion='" + descripcion + '\'' +
                ", solucionado=" + solucionado +
                ", tecnico='" + tecnicoAsignado + '\'' +
                '}';
    }
}