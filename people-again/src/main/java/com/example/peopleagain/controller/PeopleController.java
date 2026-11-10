package com.example.peopleagain.controller;


import com.example.peopleagain.kernel.ApiResponse;
import com.example.peopleagain.model.People;
import com.example.peopleagain.service.PeopleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController                      // este controlador devuelve JSON
@RequestMapping("/people")
@CrossOrigin(origins = "*")
public class PeopleController {

    @Autowired
    private PeopleService peopleService;

    // GET http://localhost:8080/people
    @GetMapping
    public ResponseEntity<ApiResponse> listar() {
        ApiResponse r = peopleService.getAll();
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // GET http://localhost:8080/people/1
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse> obtener(@PathVariable Long id) {
        ApiResponse r = peopleService.getOne(id);
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // POST http://localhost:8080/people   body: { "nombre": "Erick" }
    @PostMapping
    public ResponseEntity<ApiResponse> crear(@RequestBody People people) {
        ApiResponse r = peopleService.crear(people);
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // PUT http://localhost:8080/people/1   body: { "nombre": "Nuevo" }
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse> actualizar(@PathVariable Long id, @RequestBody People people) {
        ApiResponse r = peopleService.actualizar(id, people);
        return ResponseEntity.status(r.getStatus()).body(r);
    }

    // DELETE http://localhost:8080/people/1
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse> eliminar(@PathVariable Long id) {
        ApiResponse r = peopleService.eliminar(id);
        return ResponseEntity.status(r.getStatus()).body(r);
    }
}