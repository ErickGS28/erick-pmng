"use strict";

const d = document, c = console;

// Debemos cambiar localhost por la ip publica cuando lo subamos a la ec2.
  // Debemos cambiar localhost por la ip publica cuando lo subamos a la ec2.
  const LOCAL = "http://localhost:8080/people";
  const PROD  = "http://50.16.15.169:8089/people";

  const url = PROD;

const $btn = d.getElementById("btn");
const $ul = d.getElementById("ul");
const $input1 = d.getElementById("input1");


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
    li.textContent = dato.nombre;
    $ul.appendChild(li);
}

// Agregar al dar click
$btn.addEventListener("click", (e)=> {
    e.preventDefault();
    
    if(!$input1.value.trim()) return;

    fetch(url, {
        method: "POST",
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
    })
    .catch(err => c.error(err));
}); 

cargarDatos();