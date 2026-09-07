package com.proyecto.netsit_demo.modelo;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "umbrales_trafico")
public class UmbralesTrafico {

    @Id
    private Long id;

    private double umbralAdvertencia;

    private double umbralCritico;

    public UmbralesTrafico() {
    }

    public UmbralesTrafico(Long id, double umbralAdvertencia, double umbralCritico) {
        this.id = id;
        this.umbralAdvertencia = umbralAdvertencia;
        this.umbralCritico = umbralCritico;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public double getUmbralAdvertencia() {
        return umbralAdvertencia;
    }

    public void setUmbralAdvertencia(double umbralAdvertencia) {
        this.umbralAdvertencia = umbralAdvertencia;
    }

    public double getUmbralCritico() {
        return umbralCritico;
    }

    public void setUmbralCritico(double umbralCritico) {
        this.umbralCritico = umbralCritico;
    }
}