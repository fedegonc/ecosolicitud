/* SW mínimo: existe para que el navegador ofrezca "instalar la app".
   NO cachea nada: es una app server-rendered y cachear HTML mostraría
   datos viejos (solicitudes, bandeja). Todo request va directo a la red;
   el activate limpia caches por si algún SW anterior quedó registrado. */
self.addEventListener('install', function () {
  self.skipWaiting();
});

self.addEventListener('activate', function (event) {
  event.waitUntil(
    caches.keys()
      .then(function (names) { return Promise.all(names.map(function (n) { return caches.delete(n); })); })
      .then(function () { return self.clients.claim(); })
  );
});

self.addEventListener('fetch', function (event) {
  event.respondWith(
    fetch(event.request).catch(function () {
      return new Response('Sin conexión', { status: 503 });
    })
  );
});
