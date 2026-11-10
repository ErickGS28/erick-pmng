package com.example.peopleagain.service;


import com.example.peopleagain.kernel.ApiResponse;
import com.example.peopleagain.model.People;
import com.example.peopleagain.model.PeopleRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PeopleService {

    @Autowired
    private PeopleRepository peopleRepository;


    public ApiResponse getAll() {
        List<People> people = peopleRepository.findAll();
        return new ApiResponse("Personas obtenidas", people, false, HttpStatus.OK);
    }


    public ApiResponse getOne(Long id) {
        Optional<People> p = peopleRepository.findById(id);
        if (p.isEmpty()) {
            return new ApiResponse("No existe esa persona", null, true, HttpStatus.NOT_FOUND);
        }
        return new ApiResponse("Persona encontrada", p.get(), false, HttpStatus.OK);
    }


    public ApiResponse crear(People people) {
        People saved = peopleRepository.save(people);
        return new ApiResponse("Persona creada", saved, false, HttpStatus.CREATED);
    }

    public ApiResponse actualizar(Long id, People datos) {
        Optional<People> existente = peopleRepository.findById(id);
        if (existente.isEmpty()) {
            return new ApiResponse("No existe esa persona", null, true, HttpStatus.NOT_FOUND);
        }
        People p = existente.get();
        p.setNombre(datos.getNombre());
        People actualizado = peopleRepository.save(p);
        return new ApiResponse("Persona actualizada", actualizado, false, HttpStatus.OK);
    }

    public ApiResponse eliminar(Long id) {
        if (!peopleRepository.existsById(id)) {
            return new ApiResponse("No existe esa persona", null, true, HttpStatus.NOT_FOUND);
        }
        peopleRepository.deleteById(id);
        return new ApiResponse("Persona eliminada", null, false, HttpStatus.OK);
    }
}