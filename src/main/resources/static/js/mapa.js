/* Mapas sin librerías: el botón [data-mapa-geolocalizar] pide la ubicación del
   navegador, la vuelca en los inputs latitud/longitud del form y muestra el
   iframe OSM embebido (mismo URL que Ubicacion.mapaEmbebido() en el servidor).
   El listener vive en document: sobrevive a los swaps parciales de htmx. */
(function () {
    if (window.__mapaInit) {
        return;
    }
    window.__mapaInit = true;

    var MARGEN = 0.004;

    function urlMapa(lat, lng) {
        return 'https://www.openstreetmap.org/export/embed.html?bbox=' +
            (lng - MARGEN) + ',' + (lat - MARGEN) + ',' +
            (lng + MARGEN) + ',' + (lat + MARGEN) +
            '&layer=mapnik&marker=' + lat + ',' + lng;
    }

    document.addEventListener('click', function (e) {
        var boton = e.target.closest('[data-mapa-geolocalizar]');
        if (!boton || !navigator.geolocation) {
            return;
        }
        var form = boton.closest('form');
        var error = form.querySelector('[data-mapa-error]');
        var mapa = form.querySelector('[data-mapa-ubicacion]');
        boton.disabled = true;
        navigator.geolocation.getCurrentPosition(function (pos) {
            form.querySelector('[name="latitud"]').value = pos.coords.latitude;
            form.querySelector('[name="longitud"]').value = pos.coords.longitude;
            if (mapa) {
                mapa.src = urlMapa(pos.coords.latitude, pos.coords.longitude);
                mapa.hidden = false;
            }
            if (error) {
                error.hidden = true;
            }
            boton.disabled = false;
        }, function () {
            if (error) {
                error.hidden = false;
            }
            boton.disabled = false;
        }, { timeout: 10000 });
    });
})();
