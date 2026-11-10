 "use strict";

  const d = document, c = console;
  
  // Debemos cambiar localhost por la ip publica cuando lo subamos a la ec2.
  const LOCAL = "http://localhost:8080/people";
  const PROD  = "http://50.16.15.169:8089/people";

 const url = PROD;

  const $btn = d.getElementById("btn");
  const $ul = d.getElementById("ul");
  const $input1 = d.getElementById("input1");

  // Guarda el id que estamos editando. null = estamos creando.
  let editandoId = null;


  function cargarDatos() {

      fetch(url)
      .then(res => res.json())
          .then(data => {
              $ul.innerHTML = "";
              const dataClean = data.data;
              dataClean.forEach(dato => agregar(dato));
          })
          .catch(err => c.error(err));
  }

  // _______________________________________________________
  function agregar(dato) {
      const li = d.createElement("li");
      li.textContent = dato.nombre + " ";

      // Boton editar
      const btnEditar = d.createElement("button");
      btnEditar.textContent = "Editar";
      btnEditar.addEventListener("click", () => editar(dato));

      // Boton borrar
      const btnBorrar = d.createElement("button");
      btnBorrar.textContent = "Borrar";
      btnBorrar.addEventListener("click", () => borrar(dato.id));

      li.appendChild(btnEditar);
      li.appendChild(btnBorrar);
      $ul.appendChild(li);
  }

  // Pasar al modo edicion: rellena el input y recuerda el id
  function editar(dato) {
      $input1.value = dato.nombre;
      editandoId = dato.id;
      $btn.textContent = "Actualizar";
  }

  // _______________________________________________________
  // Borrar al dar click (DELETE)
  function borrar(id) {
      fetch(url + "/" + id, {
          method: "DELETE"
      })
      .then(res => res.json())
      .then(data => {
          c.log(data);
          cargarDatos();
      })
      .catch(err => c.error(err));
  }

  // _______________________________________________________
  // Crear o Actualizar al dar click (POST o PUT)
  $btn.addEventListener("click", (e)=> {
      e.preventDefault();

      if(!$input1.value.trim()) return;

      // Si hay un id en edicion -> PUT. Si no -> POST.
      const metodo = editandoId ? "PUT" : "POST";
      const endpoint = editandoId ? url + "/" + editandoId : url;

      fetch(endpoint, {
          method: metodo,
          headers: {
              "Content-type": "application/json"
          },
          body: JSON.stringify({nombre: $input1.value})
      })
      .then(res => res.json())
      .then(data => {
          c.log(data);
          cargarDatos();
          $input1.value = "";
          editandoId = null;          // salimos del modo edicion
          $btn.textContent = "Agregar";
      })
      .catch(err => c.error(err));
  });

  cargarDatos();