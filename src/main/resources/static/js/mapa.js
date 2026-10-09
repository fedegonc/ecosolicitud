/* Selector de ubicación con Leaflet: tocar el mapa o arrastrar el marcador
   escribe los inputs latitud/longitud del form; "Usar mi ubicación" solo
   propone un punto. Las vistas de solo lectura siguen con el iframe de OSM.
   Se carga desde el cuerpo (hx-boost no ejecuta scripts del head) y trae
   Leaflet bajo demanda desde las URLs versionadas que da el template. */
(function () {
    if (window.__mapaInit) {
        return;
    }
    window.__mapaInit = true;

    var CIUDADES = { RIVERA: [-30.9006, -55.5408], LIVRAMENTO: [-30.8894, -55.5318] };
    var FRONTERA = [-30.8950, -55.5363];
    var leaflet = null;

    function cargarLeaflet(el) {
        if (window.L) {
            return Promise.resolve();
        }
        if (!leaflet) {
            leaflet = new Promise(function (listo, falla) {
                var css = document.createElement('link');
                css.rel = 'stylesheet';
                css.href = el.dataset.leafletCss;
                document.head.appendChild(css);
                var js = document.createElement('script');
                js.src = el.dataset.leafletJs;
                js.onload = listo;
                js.onerror = falla;
                document.head.appendChild(js);
            });
        }
        return leaflet;
    }

    function error(form, visible) {
        var aviso = form.querySelector('[data-mapa-error]');
        if (aviso) {
            aviso.hidden = !visible;
        }
    }

    function iniciar(el) {
        if (el.dataset.mapaListo) {
            return;
        }
        el.dataset.mapaListo = '1';
        var form = el.closest('form');
        var lat = form.querySelector('[name="latitud"]');
        var lng = form.querySelector('[name="longitud"]');
        var quitar = form.querySelector('[data-mapa-quitar]');

        cargarLeaflet(el).then(function () {
            var mapa = L.map(el, { scrollWheelZoom: false });
            L.tileLayer('https://tile.openstreetmap.org/{z}/{x}/{y}.png', {
                maxZoom: 19,
                attribution: '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a>'
            }).addTo(mapa);
            var marcador = null;

            function escribir(p) {
                lat.value = p.lat.toFixed(6);
                lng.value = p.lng.toFixed(6);
                if (quitar) {
                    quitar.hidden = false;
                }
            }

            el.mapaPoner = function (p, centrar) {
                if (marcador) {
                    marcador.setLatLng(p);
                } else {
                    marcador = L.marker(p, { draggable: true, autoPan: true }).addTo(mapa);
                    marcador.on('dragend', function () { escribir(marcador.getLatLng()); });
                }
                escribir(p);
                if (centrar) {
                    mapa.setView(p, 17);
                }
            };
            el.mapaQuitar = function () {
                if (marcador) {
                    mapa.removeLayer(marcador);
                    marcador = null;
                }
                lat.value = '';
                lng.value = '';
                if (quitar) {
                    quitar.hidden = true;
                }
            };

            mapa.on('click', function (e) { el.mapaPoner(e.latlng, false); });
            if (lat.value && lng.value) {
                el.mapaPoner(L.latLng(Number(lat.value), Number(lng.value)), true);
            } else {
                var ciudad = form.querySelector('[name="ciudad"]');
                mapa.setView(CIUDADES[ciudad && ciudad.value] || FRONTERA, 14);
            }
            // el bloque puede estar oculto al cargar (cascada de nueva solicitud)
            new ResizeObserver(function () { mapa.invalidateSize(); }).observe(el);
        }).catch(function () {
            error(form, true);
        });
    }

    function escanear(raiz) {
        if (raiz.matches && raiz.matches('[data-mapa-editable]')) {
            iniciar(raiz);
        }
        if (raiz.querySelectorAll) {
            raiz.querySelectorAll('[data-mapa-editable]').forEach(iniciar);
        }
    }

    document.addEventListener('click', function (e) {
        var quitar = e.target.closest('[data-mapa-quitar]');
        var boton = e.target.closest('[data-mapa-geolocalizar]');
        if (!quitar && !boton) {
            return;
        }
        var form = (quitar || boton).closest('form');
        var el = form.querySelector('[data-mapa-editable]');
        if (quitar) {
            if (el && el.mapaQuitar) {
                el.mapaQuitar();
            }
            return;
        }
        if (!navigator.geolocation || !window.isSecureContext) {
            error(form, true);
            return;
        }
        boton.disabled = true;
        navigator.geolocation.getCurrentPosition(function (pos) {
            error(form, false);
            boton.disabled = false;
            if (el && el.mapaPoner) {
                el.mapaPoner(L.latLng(pos.coords.latitude, pos.coords.longitude), true);
            }
        }, function () {
            error(form, true);
            boton.disabled = false;
        }, { timeout: 10000 });
    });

    document.addEventListener('htmx:load', function (e) { escanear(e.detail.elt); });
    escanear(document);
})();
