package com.example.peopleagain.model;

import jakarta.persistence.*;
import lombok.Data;

@Data                      // Lombok: crea getters, setters, toString, etc. solo
@Entity                    // Esto es una tabla de la BD
@Table(name = "people")    // La tabla se llamará "people"
public class People {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;   // una columna "nombre"
}
